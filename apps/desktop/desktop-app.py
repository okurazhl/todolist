"""
智能备忘录桌面端 — 无边框原生窗口
"""
import webview
import sys

URL = "http://localhost:5173"

class API:
    def __init__(self):
        self._window = None
    def set_window(self, w): self._window = w
    def minimize(self): self._window and self._window.minimize()
    def close(self): self._window and self._window.destroy()
    def toggle_maximize(self):
        if self._window:
            try: self._window.restore()
            except: self._window.maximize()

# 页面加载完成后注入标题栏
TITLEBAR_JS = """
(function() {
  if (document.querySelector('.desktop-titlebar')) return;
  var bar = document.createElement('div');
  bar.className = 'desktop-titlebar';
  bar.innerHTML =
    '<span class="title">智能备忘录</span>' +
    '<div class="ctrls">' +
    '<button class="ctrl" onclick="window.pywebview.api.minimize()">━</button>' +
    '<button class="ctrl" onclick="window.pywebview.api.toggle_maximize()">☐</button>' +
    '<button class="ctrl close" onclick="window.pywebview.api.close()">✕</button>' +
    '</div>';
  document.body.prepend(bar);
})();
"""

if __name__ == "__main__":
    try:
        import urllib.request
        urllib.request.urlopen(URL, timeout=3)
    except Exception:
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
    # pywebview 不支持 events.loaded 注入 JS，改用 webview.start 后
    webview.start(gui="edgechromium", debug=False)
