import apiClient, { type ApiResponse } from './client';

export interface TagItem {
  id: string;
  name: string;
  color: string | null;
}

export async function listTags(): Promise<TagItem[]> {
  const res = await apiClient.get<ApiResponse<{ items: TagItem[] }>>('/tags');
  return res.data.data.items;
}

export async function createTag(name: string, color?: string): Promise<TagItem> {
  const res = await apiClient.post<ApiResponse<TagItem>>('/tags', { name, color });
  return res.data.data;
}

export async function deleteTag(id: string): Promise<void> {
  await apiClient.delete(`/tags/${id}`);
}
