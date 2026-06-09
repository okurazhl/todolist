import { useEffect, useState, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createMemo, updateMemo, getMemo } from '../../shared/api/memos';
import { listTags, type TagItem } from '../../shared/api/tags';
import { listCategories, type CategoryItem } from '../../shared/api/categories';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';
import { uploadAudio, getTask } from '../../shared/api/asr';
import { refineContent } from '../../shared/api/ai';

export function MemoEditorPage() {
  const { id } = useParams<{ id: string }>();
  const isEdit = !!id;
  const navigate = useNavigate();

  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [tags, setTags] = useState<TagItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [remindAt, setRemindAt] = useState('');

  // 录音
  const [recording, setRecording] = useState(false);
  const [transcribing, setTranscribing] = useState(false);
  const [refining, setRefining] = useState(false);
  const mediaRecorder = useRef<MediaRecorder | null>(null);
  const chunks = useRef<Blob[]>([]);

  useEffect(() => {
    Promise.all([listTags(), listCategories()]).then(([t, c]) => {
      setTags(t); setCategories(c);
    }).catch(() => {});
  }, []);

  useEffect(() => {
    if (isEdit && id) {
      setLoading(true);
      getMemo(id).then((memo) => {
        setTitle(memo.title);
        setContent(memo.content || '');
        setCategoryId(memo.categoryId || '');
        setSelectedTags(memo.tagIds || []);
        setRemindAt(memo.remindAt ? memo.remindAt.slice(0, 16) : '');
      }).catch(() => setError('加载失败')).finally(() => setLoading(false));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const data = { title, content: content || undefined, categoryId: categoryId || undefined, tagIds: selectedTags,
        remindAt: remindAt ? remindAt + ':00+08:00' : undefined };
      if (isEdit && id) { await updateMemo(id, data); }
      else { await createMemo(data); }
      navigate('/');
    } catch { setError('保存失败'); } finally { setSaving(false); }
  };

  const toggleTag = (tagId: string) => {
    setSelectedTags((prev) => prev.includes(tagId) ? prev.filter((t) => t !== tagId) : [...prev, tagId]);
  };

  // ===== 录音 → ASR → AI 提炼 =====
  const startRecording = useCallback(async () => {
    setError('');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : 'audio/webm';
      const recorder = new MediaRecorder(stream, { mimeType: mime });
      mediaRecorder.current = recorder;
      chunks.current = [];
      recorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.current.push(e.data); };
      recorder.onstop = () => stream.getTracks().forEach((t) => t.stop());
      recorder.start(1000);
      setRecording(true);
    } catch { setError('无法访问麦克风'); }
  }, []);

  const stopRecording = useCallback(async () => {
    if (!mediaRecorder.current) return;
    mediaRecorder.current.stop();
    setRecording(false);
    setTranscribing(true);

    await new Promise((r) => setTimeout(r, 300));

    if (chunks.current.length === 0) {
      setTranscribing(false);
      setError('未捕获到音频数据，请重试');
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
        // 自动 AI 提炼
        const refined = await refineContent(task.transcribedText);
        setContent((prev) => prev + (prev ? '\n' : '') + refined);
      } else {
        setError('转写失败: ' + (task.errorMessage || '未知错误'));
      }
    } catch { setError('录音处理失败'); }
    finally { setTranscribing(false); }
  }, []);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="memo-editor-page">
      <h2>{isEdit ? '编辑备忘录' : '新建备忘录'}</h2>
      <form onSubmit={handleSubmit}>
        <label>
          标题
          <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={256} />
        </label>
        <label>
          正文
          <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={8} maxLength={50000} />
          <div className="editor-toolbar">
            {recording ? (
              <button type="button" className="btn-record active" onClick={stopRecording}>
                ⏹ 停止录音
              </button>
            ) : (
              <button type="button" className="btn-record" onClick={startRecording} disabled={transcribing}>
                🎙️ 语音输入
              </button>
            )}
            {transcribing && <span className="transcribing-hint">转写中...</span>}

            <button type="button" className="btn-ai" disabled={refining || !content.trim()}
              onClick={async () => {
                setRefining(true); setError('');
                try { const refined = await refineContent(content); setContent(refined); }
                catch { setError('AI 提炼失败'); } finally { setRefining(false); }
              }}>
              {refining ? '🤖 提炼中...' : '🤖 AI 提炼'}
            </button>
          </div>
        </label>
        <label>
          分类
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">无分类</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label>
          提醒时间
          <input type="datetime-local" value={remindAt}
            onChange={(e) => setRemindAt(e.target.value)} />
        </label>
        <div className="tag-selector">
          <span>标签：</span>
          {tags.map((t) => (
            <label key={t.id} className="tag-checkbox">
              <input type="checkbox" checked={selectedTags.includes(t.id)} onChange={() => toggleTag(t.id)} />
              {t.name}
            </label>
          ))}
        </div>
        {error && <p className="form-error">{error}</p>}
        <button type="submit" disabled={saving || recording}>{saving ? '保存中...' : '保存'}</button>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>取消</button>
      </form>
    </div>
  );
}
