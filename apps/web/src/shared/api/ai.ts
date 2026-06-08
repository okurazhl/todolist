import apiClient, { type ApiResponse } from './client';

export async function refineContent(content: string): Promise<string> {
  const res = await apiClient.post<ApiResponse<{ refined: string }>>('/ai/refine', { content });
  return res.data.data.refined;
}
