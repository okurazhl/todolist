import axios from 'axios';

/**
 * 统一 API 响应格式
 */
export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

/**
 * 分页响应格式
 */
export interface PaginatedResponse<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}

/**
 * API Client 实例。
 * 统一处理 baseURL、token 注入、traceId 传递、错误转换。
 */
const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：注入 traceId
apiClient.interceptors.request.use((config) => {
  const traceId = generateTraceId();
  config.headers.set('X-Trace-Id', traceId);
  return config;
});

// 响应拦截器：统一错误处理
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const body = error.response.data as ApiResponse | undefined;
      console.error(`[API Error] ${body?.code ?? 'UNKNOWN'}: ${body?.message ?? error.message}`);
    } else if (error.request) {
      console.error('[API Error] 网络不可达，请检查后端服务是否启动');
    }
    return Promise.reject(error);
  },
);

function generateTraceId(): string {
  // 生成 UUID v7 风格 traceId
  return crypto.randomUUID();
}

export default apiClient;
