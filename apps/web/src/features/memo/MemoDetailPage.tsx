import { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getMemo, deleteMemo, pinMemo, unpinMemo, archiveMemo, type MemoItem } from '../../shared/api/memos';
import { LoadingSpinner } from '../../shared/components/LoadingSpinner';
import { ErrorMessage } from '../../shared/components/ErrorMessage';

export function MemoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [memo, setMemo] = useState<MemoItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = () => {
    if (!id) return;
    setLoading(true);
    getMemo(id).then(setMemo).catch(() => setError('加载失败')).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, [id]);

  const handlePin = async () => {
    if (!memo) return;
    const updated = memo.isPinned ? await unpinMemo(memo.id) : await pinMemo(memo.id);
    setMemo(updated);
  };

  const handleArchive = async () => {
    if (!memo) return;
    await archiveMemo(memo.id);
    navigate('/');
  };

  const handleDelete = async () => {
    if (!memo || !confirm('确定删除？')) return;
    await deleteMemo(memo.id);
    navigate('/');
  };

  if (loading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={error} onRetry={load} />;
  if (!memo) return <ErrorMessage message="备忘录不存在" />;

  return (
    <div className="memo-detail-page">
      <div className="memo-detail-header">
        <h2>{memo.title} {memo.isPinned && '📌'}</h2>
        <span className={`status-badge status-${memo.status}`}>{memo.status}</span>
      </div>
      <div className="memo-detail-meta">
        <span>创建：{new Date(memo.createdAt).toLocaleString()}</span>
        <span>更新：{new Date(memo.updatedAt).toLocaleString()}</span>
      </div>
      <div className="memo-detail-content">{memo.content || '(无内容)'}</div>
      <div className="memo-detail-actions">
        <button onClick={handlePin}>{memo.isPinned ? '取消置顶' : '置顶'}</button>
        <button onClick={handleArchive}>归档</button>
        <Link to={`/memo/${memo.id}/edit`}>编辑</Link>
        <button className="btn-danger" onClick={handleDelete}>删除</button>
      </div>
      <Link to="/" className="btn-back">← 返回列表</Link>
    </div>
  );
}
