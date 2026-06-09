import { useEffect, useState } from 'react';
import { listTags, createTag, deleteTag, type TagItem } from '../../shared/api/tags';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';

export function TagManagePage() {
  const [tags, setTags] = useState<TagItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');

  const load = () => {
    listTags().then(setTags).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    const newTag = await createTag(name.trim());
    setTags((prev) => [...prev, newTag]);
    setName('');
  };

  const handleDelete = async (id: string) => {
    await deleteTag(id);
    setTags((prev) => prev.filter((t) => t.id !== id));
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="manage-page">
      <h2>标签管理</h2>
      <form onSubmit={handleCreate} className="inline-form">
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="新标签名" maxLength={32} />
        <button type="submit">添加</button>
      </form>
      <ul className="item-list">
        {tags.map((t) => (
          <li key={t.id}>{t.name} <button className="btn-small-danger" onClick={() => handleDelete(t.id)}>删除</button></li>
        ))}
      </ul>
    </div>
  );
}
