import axios from 'axios';

export interface ApiResponse<T = unknown> {
  code: string;
  message: string;
  data: T;
  traceId: string;
}

const TOKEN_KEY = 'access_token';
const REFRESH_KEY = 'refresh_token';

const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

// ===== 请求拦截器：注入 Token 和 TraceId =====
apiClient.interceptors.request.use((config) => {
  config.headers.set('X-Trace-Id', crypto.randomUUID());
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

// ===== 响应拦截器：401 自动刷新 =====
let isRefreshing = false;
let refreshQueue: Array<{ resolve: (token: string) => void; reject: (err: unknown) => void }> = [];

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = localStorage.getItem(REFRESH_KEY);
      if (!refreshToken) {
        clearAuth();
        window.location.href = '/login';
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          refreshQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.set('Authorization', `Bearer ${token}`);
          return apiClient(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const res = await axios.post('/api/v1/auth/refresh', { refreshToken });
        const { accessToken, refreshToken: newRefresh } = res.data.data;
        localStorage.setItem(TOKEN_KEY, accessToken);
        localStorage.setItem(REFRESH_KEY, newRefresh);

        refreshQueue.forEach((q) => q.resolve(accessToken));
        refreshQueue = [];

        originalRequest.headers.set('Authorization', `Bearer ${accessToken}`);
        return apiClient(originalRequest);
      } catch (refreshError) {
        clearAuth();
        refreshQueue.forEach((q) => q.reject(refreshError));
        refreshQueue = [];
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }
    return Promise.reject(error);
  },
);

export function saveAuth(accessToken: string, refreshToken: string) {
  localStorage.setItem(TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_KEY, refreshToken);
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export default apiClient;
