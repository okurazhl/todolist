import { useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadAudio, getTask, type AsrTask } from '../../shared/api/asr';
import { createMemo } from '../../shared/api/memos';

export function VoiceUploadPage() {
  const [recording, setRecording] = useState(false);
  const [task, setTask] = useState<AsrTask | null>(null);
  const [error, setError] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const mediaRecorder = useRef<MediaRecorder | null>(null);
  const chunks = useRef<Blob[]>([]);
  const timerRef = useRef<number>(0);
  const navigate = useNavigate();

  // ===== 开始录音 =====
  const startRecording = useCallback(async () => {
    setError('');
    setTask(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream, {
        mimeType: MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
          ? 'audio/webm;codecs=opus'
          : 'audio/webm',
      });
      mediaRecorder.current = recorder;
      chunks.current = [];

      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.current.push(e.data);
      };

      recorder.onstop = () => {
        stream.getTracks().forEach((t) => t.stop());
      };

      recorder.start(1000); // 每秒收集一次数据
      setRecording(true);
      setElapsed(0);

      timerRef.current = window.setInterval(() => {
        setElapsed((prev) => prev + 1);
      }, 1000);
    } catch {
      setError('无法访问麦克风，请检查浏览器权限');
    }
  }, []);

  // ===== 停止录音并转写 =====
  const stopRecording = useCallback(async () => {
    if (!mediaRecorder.current) return;

    mediaRecorder.current.stop();
    clearInterval(timerRef.current);
    setRecording(false);

    // 等待 onstop 完成
    await new Promise((r) => setTimeout(r, 200));

    const blob = new Blob(chunks.current, { type: 'audio/webm' });
    const file = new File([blob], `recording-${Date.now()}.webm`, { type: 'audio/webm' });

    try {
      const created = await uploadAudio(file);
      setTask(created);

      let current = created;
      while (current.status === 'pending' || current.status === 'processing') {
        await new Promise((r) => setTimeout(r, 2000));
        current = await getTask(current.taskId);
        setTask({ ...current });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '转写失败');
    }
  }, []);

  // ===== 创建备忘录 =====
  const createFromAudio = async () => {
    if (!task?.transcribedText) return;
    try {
      const memo = await createMemo({
        title: `语音备忘 ${new Date().toLocaleString('zh-CN')}`,
        content: task.transcribedText,
      });
      navigate(`/memo/${memo.id}`);
    } catch {
      setError('创建失败');
    }
  };

  // ===== 格式化时间 =====
  const fmt = (s: number) => `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const statusLabel = (s: string) => {
    switch (s) {
      case 'pending': return '⏳ 排队中';
      case 'processing': return '🔄 转写中...';
      case 'completed': return '✅ 完成';
      case 'failed': return '❌ 失败';
      default: return s;
    }
  };

  return (
    <div className="voice-page">
      <h2>语音录入</h2>

      {!task && (
        <div className="voice-record-area">
          {recording ? (
            <div className="voice-recording">
              <div className="voice-mic-icon">🎙️</div>
              <div className="voice-timer">{fmt(elapsed)}</div>
              <p className="voice-hint">正在录音...</p>
              <button className="btn-danger" onClick={stopRecording}>
                ⏹ 停止录音
              </button>
            </div>
          ) : (
            <div className="voice-idle">
              <p>点击下方按钮开始录音，说完后停止即可自动转写</p>
              <button className="btn-primary voice-start-btn" onClick={startRecording}>
                🎙️ 开始录音
              </button>
              <p className="voice-sub-hint">需要授权麦克风权限</p>
            </div>
          )}
        </div>
      )}

      {task && (
        <div className="voice-task-card">
          <div className={`voice-status voice-${task.status}`}>
            {statusLabel(task.status)}
          </div>

          <div className="voice-meta">
            <span>文件：{task.fileName}</span>
            <span>大小：{(task.fileSize / 1024).toFixed(1)} KB</span>
          </div>

          {task.status === 'processing' && (
            <p className="voice-progress">正在识别语音内容，请稍候...</p>
          )}

          {task.status === 'completed' && task.transcribedText && (
            <div className="voice-result">
              <h3>转写结果</h3>
              <pre>{task.transcribedText}</pre>
              <div className="voice-actions">
                <button className="btn-primary" onClick={createFromAudio}>
                  创建备忘录
                </button>
                <button className="btn-secondary" onClick={() => setTask(null)}>
                  重新录音
                </button>
              </div>
            </div>
          )}

          {task.status === 'failed' && (
            <div className="voice-error">
              <p>转写失败：{task.errorMessage || '未知错误'}</p>
              <button onClick={() => setTask(null)}>重试</button>
            </div>
          )}
        </div>
      )}

      {error && <div className="form-error">{error}</div>}
    </div>
  );
}
