import { useEffect, useState } from 'react';
import { listCategories, createCategory, deleteCategory, type CategoryItem } from '../../shared/api/categories';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';

export function CategoryManagePage() {
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [name, setName] = useState('');

  const load = () => {
    listCategories().then(setCategories).finally(() => setLoading(false));
  };
  useEffect(load, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    const newCat = await createCategory(name.trim());
    setCategories((prev) => [...prev, newCat]);
    setName('');
  };

  const handleDelete = async (id: string) => {
    await deleteCategory(id);
    setCategories((prev) => prev.filter((c) => c.id !== id));
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="manage-page">
      <h2>分类管理</h2>
      <form onSubmit={handleCreate} className="inline-form">
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="新分类名" maxLength={64} />
        <button type="submit">添加</button>
      </form>
      <ul className="item-list">
        {categories.map((c) => (
          <li key={c.id}>{c.name} <button className="btn-small-danger" onClick={() => handleDelete(c.id)}>删除</button></li>
        ))}
      </ul>
    </div>
  );
}
