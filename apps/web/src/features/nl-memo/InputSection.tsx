import { useRef, useCallback, useState } from 'react';
import { uploadAudio, getTask } from '../../shared/api/asr';

interface Props {
  value: string;
  onChange: (v: string) => void;
  onConvert: () => void;
  converting: boolean;
}

export function InputSection({ value, onChange, onConvert, converting }: Props) {
  const [recording, setRecording] = useState(false);
  const [transcribing, setTranscribing] = useState(false);
  const [voiceError, setVoiceError] = useState('');
  const mediaRecorder = useRef<MediaRecorder | null>(null);
  const chunks = useRef<Blob[]>([]);

  const startRecording = useCallback(async () => {
    setVoiceError('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
        ? 'audio/webm;codecs=opus'
        : 'audio/webm';
      const recorder = new MediaRecorder(stream, { mimeType: mime });
      mediaRecorder.current = recorder;
      chunks.current = [];
      recorder.ondataavailable = (e) => {
        if (e.data.size > 0) chunks.current.push(e.data);
      };
      recorder.onstop = () => stream.getTracks().forEach((t) => t.stop());
      recorder.start(1000);
      setRecording(true);
    } catch {
      setVoiceError('无法访问麦克风，请检查浏览器权限设置');
    }
  }, []);

  const stopRecording = useCallback(async () => {
    if (!mediaRecorder.current) return;
    mediaRecorder.current.stop();
    setRecording(false);
    setTranscribing(true);
    setVoiceError('');

    await new Promise((r) => setTimeout(r, 300));

    if (chunks.current.length === 0) {
      setTranscribing(false);
      setVoiceError('未捕获到音频数据，请重试');
      return;
    }

    const blob = new Blob(chunks.current, { type: 'audio/webm' });
    const file = new File([blob], `recording-${Date.now()}.webm`, { type: 'audio/webm' });

    try {
      let task = await uploadAudio(file);
      while (task.status === 'pending' || task.status === 'processing') {
        await new Promise((r) => setTimeout(r, 2000));
        task = await getTask(task.taskId);
      }
      if (task.status === 'completed' && task.transcribedText) {
        onChange(value ? value + '\n' + task.transcribedText : task.transcribedText);
      } else if (task.status === 'failed') {
        setVoiceError('语音转写失败: ' + (task.errorMessage || '未知错误'));
      }
    } catch {
      setVoiceError('语音上传失败，请检查网络后重试');
    } finally {
      setTranscribing(false);
    }
  }, [value, onChange]);

  return (
    <div className="nl-card">
      <div className="nl-card-title">
        <span className="step-num">1.</span> 输入你的提醒内容
      </div>

      <div className="nl-textarea-wrap">
        <textarea
          className="nl-textarea"
          placeholder="例如：今天晚上五点提醒我去吃饭"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          maxLength={200}
          rows={3}
        />
        <div className="nl-char-count">{value.length}/200</div>
      </div>

      <div className="nl-input-actions">
        {/* Mic button */}
        {recording ? (
          <button className="nl-btn-mic recording" onClick={stopRecording} title="停止录音">
            ⏹
          </button>
        ) : (
          <button
            className="nl-btn-mic"
            onClick={startRecording}
            disabled={transcribing}
            title="语音输入"
          >
            🎙️
          </button>
        )}
        {transcribing && <span className="transcribing-hint">转写中...</span>}
        {voiceError && <span className="form-error">{voiceError}</span>}

        <button className="nl-btn-clear" onClick={() => onChange('')}>清空</button>

        <button
          className="nl-btn-convert"
          onClick={onConvert}
          disabled={converting || !value.trim()}
        >
          {converting ? '⏳ 解析中...' : '转换为备忘录'} ✨
        </button>
      </div>
    </div>
  );
}
