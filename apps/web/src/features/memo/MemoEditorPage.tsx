import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createMemo, updateMemo, getMemo } from '../../shared/api/memos';
import { listTags, type TagItem } from '../../shared/api/tags';
import { listCategories, type CategoryItem } from '../../shared/api/categories';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';

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
      }).catch(() => setError('加载失败')).finally(() => setLoading(false));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const data = { title, content: content || undefined, categoryId: categoryId || undefined, tagIds: selectedTags };
      if (isEdit && id) {
        await updateMemo(id, data);
      } else {
        await createMemo(data);
      }
      navigate('/');
    } catch {
      setError('保存失败');
    } finally {
      setSaving(false);
    }
  };

  const toggleTag = (tagId: string) => {
    setSelectedTags((prev) =>
      prev.includes(tagId) ? prev.filter((t) => t !== tagId) : [...prev, tagId]
    );
  };

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
        </label>
        <label>
          分类
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">无分类</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
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
        <button type="submit" disabled={saving}>{saving ? '保存中...' : '保存'}</button>
        <button type="button" className="btn-secondary" onClick={() => navigate(-1)}>取消</button>
      </form>
    </div>
  );
}
