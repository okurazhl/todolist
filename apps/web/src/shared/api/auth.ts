import apiClient, { type ApiResponse } from './client';

interface AuthData {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

interface RegisterData {
  userId: string;
  username: string;
}

export async function login(username: string, password: string): Promise<AuthData> {
  const res = await apiClient.post<ApiResponse<AuthData>>('/auth/login', { username, password });
  return res.data.data;
}

export async function register(username: string, password: string, email?: string): Promise<RegisterData> {
  const res = await apiClient.post<ApiResponse<RegisterData>>('/auth/register', { username, password, email });
  return res.data.data;
}
