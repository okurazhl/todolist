import { useEffect, useState } from 'react';
import apiClient, { type ApiResponse } from '../../shared/api/client';

interface HealthData {
  service: string;
  status: string;
  timestamp: string;
}

type HealthStatus = 'loading' | 'up' | 'down';

export function HealthPage() {
  const [status, setStatus] = useState<HealthStatus>('loading');
  const [detail, setDetail] = useState<ApiResponse<HealthData> | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient
      .get<ApiResponse<HealthData>>('/health')
      .then((res) => {
        setDetail(res.data);
        setStatus(res.data.data.status === 'UP' ? 'up' : 'down');
      })
      .catch((err) => {
        setError(err.message);
        setStatus('down');
      });
  }, []);

  return (
    <div className="health-page">
      <h2>系统健康检查</h2>

      <div className={`health-status ${status}`}>
        {status === 'loading' && '⏳ 检查中...'}
        {status === 'up' && '✅ 系统运行正常'}
        {status === 'down' && '❌ 系统异常'}
      </div>

      {error && (
        <div className="health-detail">
          <p>错误信息：{error}</p>
        </div>
      )}

      {detail && (
        <div className="health-detail">
          <h3>详情</h3>
          <pre>{JSON.stringify(detail, null, 2)}</pre>
        </div>
      )}
    </div>
  );
}
