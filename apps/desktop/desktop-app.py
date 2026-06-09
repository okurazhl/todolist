"""
智能备忘录桌面端 — 无边框原生窗口 + 语音录入
"""
import webview
import sys
import os
import json
import tempfile
import threading
import urllib.request

URL = "http://localhost:5173"

class API:
    def __init__(self):
        self._window = None
        self._recording = False

    def set_window(self, w): self._window = w
    def minimize(self): self._window and self._window.minimize()
    def close(self): self._window and self._window.destroy()
    def toggle_maximize(self):
        if self._window:
            try: self._window.restore()
            except: self._window.maximize()

    def voice_record(self, token=""):
        """Python 端录音 → ASR 转写 → 通过回调返回文本。绕过 WebView2 麦克风限制。
        录音和上传在后台线程执行，不阻塞 UI；
        结果通过 window.__voiceResultCallback 回调推送到前端。"""
        if self._recording:
            return ""
        if not token:
            return "[请先登录网页版]"
        self._recording = True

        def _record_thread():
            try:
                import sounddevice as sd
                import wave

                rate = 16000
                duration = 5
                recording = sd.rec(int(duration * rate), samplerate=rate,
                                   channels=1, dtype='int16')
                sd.wait()

                # 使用 mkstemp + 立即关闭 fd，避免 Windows 上
                # NamedTemporaryFile 句柄未关闭导致 wave.open 报
                # "另一个程序正在使用此文件" (进程被占用)
                fd, tmppath = tempfile.mkstemp(suffix=".wav")
                os.close(fd)

                wf = wave.open(tmppath, 'wb')
                wf.setnchannels(1)
                wf.setsampwidth(2)
                wf.setframerate(rate)
                wf.writeframes(recording.tobytes())
                wf.close()

                text = self._upload_asr(tmppath, token)
                try:
                    os.unlink(tmppath)
                except OSError:
                    pass

                _push_result(text, None)
            except Exception as e:
                _push_result(None, str(e))
            finally:
                self._recording = False

        def _push_result(text, error):
            """通过 evaluate_js 将录音/转写结果推回前端。"""
            if not self._window:
                return
            try:
                payload = json.dumps({"text": text, "error": error}, ensure_ascii=False)
                self._window.evaluate_js(
                    f"window.__voiceResultCallback && window.__voiceResultCallback({payload})"
                )
            except Exception:
                pass  # 窗口可能已关闭

        threading.Thread(target=_record_thread, daemon=True).start()
        # 立即返回空字符串，不阻塞 webview 主线程
        # 实际结果通过 __voiceResultCallback 回调传递
        return ""

    def _upload_asr(self, filepath, token):
        """上传音频到 ASR 服务并轮询结果"""
        fname = os.path.basename(filepath)
        with open(filepath, "rb") as f:
            audio = f.read()

        boundary = "----DesktopBoundary"
        body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; "
                f"filename=\"{fname}\"\r\nContent-Type: audio/wav\r\n\r\n").encode()
        body += audio
        body += f"\r\n--{boundary}--\r\n".encode()

        req = urllib.request.Request(
            "http://localhost:8080/api/v1/asr/tasks",
            data=body,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": f"multipart/form-data; boundary={boundary}",
            },
            method="POST",
        )
        resp = json.loads(urllib.request.urlopen(req, timeout=30).read())
        task_id = resp["data"]["taskId"]

        import time
        for _ in range(15):
            time.sleep(2)
            req2 = urllib.request.Request(
                f"http://localhost:8080/api/v1/asr/tasks/{task_id}",
                headers={"Authorization": f"Bearer {token}"},
            )
            r2 = json.loads(urllib.request.urlopen(req2).read())
            if r2["data"]["status"] == "completed":
                return r2["data"].get("transcribedText", "")
            if r2["data"]["status"] == "failed":
                return "[转写失败]"
        return "[转写超时]"


if __name__ == "__main__":
    # 防止重复启动（文件锁）
    import tempfile
    lockfile = os.path.join(tempfile.gettempdir(), "smartmemo_desktop.lock")
    try:
        lock_fd = os.open(lockfile, os.O_CREAT | os.O_EXCL | os.O_RDWR)
        os.close(lock_fd)
    except FileExistsError:
        print("桌面端已在运行中，按 Alt+Tab 切换窗口")
        sys.exit(0)
    # 注册退出时清理锁文件
    import atexit
    atexit.register(lambda: os.remove(lockfile) if os.path.exists(lockfile) else None)

    try:
        urllib.request.urlopen(URL, timeout=3)
    except Exception:
        os.remove(lockfile)
        print(f"无法连接到 {URL}，请先启动 npm run dev")
        sys.exit(1)

    api = API()

    window = webview.create_window(
        title="智能备忘录",
        url=URL,
        width=1100, height=750,
        min_size=(900, 600),
        frameless=True,
        easy_drag=True,
        background_color="#182334",
        js_api=api,
    )

    api.set_window(window)
    webview.start(gui="edgechromium", debug=False)
