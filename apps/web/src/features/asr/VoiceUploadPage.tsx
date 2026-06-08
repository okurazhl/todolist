import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { uploadAudio, getTask, type AsrTask } from '../../shared/api/asr';
import { createMemo } from '../../shared/api/memos';

export function VoiceUploadPage() {
  const [task, setTask] = useState<AsrTask | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);
  const navigate = useNavigate();

  const handleFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setError('');
    setUploading(true);
    setTask(null);

    try {
      // 1. 上传 + 创建任务
      const created = await uploadAudio(file);
      setTask(created);

      // 2. 轮询状态
      let current = created;
      while (current.status === 'pending' || current.status === 'processing') {
        await new Promise((r) => setTimeout(r, 2000));
        current = await getTask(current.taskId);
        setTask({ ...current });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const createFromAudio = async () => {
    if (!task?.transcribedText) return;
    try {
      const memo = await createMemo({
        title: `语音备忘 ${new Date().toLocaleString('zh-CN')}`,
        content: task.transcribedText,
      });
      navigate(`/memo/${memo.id}`);
    } catch {
      setError('创建备忘录失败');
    }
  };

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
        <div className="voice-upload-area">
          <p>选择音频文件上传，自动转写为文字</p>
          <input
            ref={fileRef}
            type="file"
            accept="audio/*"
            onChange={handleFile}
            disabled={uploading}
          />
          {uploading && <p className="voice-status">⏳ 上传中...</p>}
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
                  重新上传
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
