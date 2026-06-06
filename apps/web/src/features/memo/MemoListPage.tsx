import { useEffect, useState, useCallback } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { listMemos, deleteMemo, pinMemo, unpinMemo, archiveMemo, type MemoItem } from '../../shared/api/memos';
import { listCategories, type CategoryItem } from '../../shared/api/categories';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';
import { ErrorMessage } from '../../shared/components/ErrorMessage';

export function MemoListPage() {
  const [memos, setMemos] = useState<MemoItem[]>([]);
  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchParams, setSearchParams] = useSearchParams();

  const status = searchParams.get('status') || 'active';
  const categoryId = searchParams.get('categoryId') || undefined;

  const load = useCallback(async (cursor?: string) => {
    setLoading(true);
    setError('');
    try {
      const data = await listMemos({ status, categoryId, tagId: undefined, cursor, limit: 20 });
      if (cursor) {
        setMemos((prev) => [...prev, ...data.items]);
      } else {
        setMemos(data.items);
      }
    } catch {
      setError('加载失败');
    } finally {
      setLoading(false);
    }
  }, [status, categoryId]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    listCategories().then(setCategories).catch(() => {});
  }, []);

  const handleDelete = async (id: string) => {
    if (!confirm('确定删除？')) return;
    await deleteMemo(id);
    setMemos((prev) => prev.filter((m) => m.id !== id));
  };

  const handlePin = async (id: string, pinned: boolean) => {
    const updated = pinned ? await unpinMemo(id) : await pinMemo(id);
    setMemos((prev) => prev.map((m) => (m.id === id ? updated : m)));
  };

  const handleArchive = async (id: string) => {
    await archiveMemo(id);
    setMemos((prev) => prev.filter((m) => m.id !== id));
  };

  return (
    <div className="memo-list-page">
      <div className="memo-list-header">
        <h2>我的备忘录</h2>
        <Link to="/memo/new" className="btn-primary">新建</Link>
      </div>

      <div className="memo-filters">
        <select value={status} onChange={(e) => setSearchParams({ status: e.target.value })}>
          <option value="active">活跃</option>
          <option value="archived">已归档</option>
        </select>
        <select value={categoryId || ''} onChange={(e) => {
          const params: Record<string, string> = { status };
          if (e.target.value) params.categoryId = e.target.value;
          setSearchParams(params);
        }}>
          <option value="">全部分类</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>

      {error && <ErrorMessage message={error} onRetry={() => load()} />}

      <div className="memo-list">
        {memos.map((memo) => (
          <div key={memo.id} className={`memo-card ${memo.isPinned ? 'pinned' : ''}`}>
            <div className="memo-card-header">
              <Link to={`/memo/${memo.id}`} className="memo-title">{memo.title}</Link>
              {memo.isPinned && <span className="pin-badge">📌</span>}
            </div>
            <p className="memo-preview">{memo.content?.substring(0, 100) || '(无内容)'}</p>
            <div className="memo-card-actions">
              <button onClick={() => handlePin(memo.id, memo.isPinned)}>
                {memo.isPinned ? '取消置顶' : '置顶'}
              </button>
              <button onClick={() => handleArchive(memo.id)}>归档</button>
              <Link to={`/memo/${memo.id}/edit`}>编辑</Link>
              <button className="btn-danger" onClick={() => handleDelete(memo.id)}>删除</button>
            </div>
          </div>
        ))}
      </div>

      {loading && <LoadingSpinner />}
    </div>
  );
}
