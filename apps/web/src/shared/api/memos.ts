import apiClient, { type ApiResponse } from './client';

export interface MemoItem {
  id: string;
  title: string;
  content: string | null;
  categoryId: string | null;
  status: 'active' | 'archived' | 'deleted' | 'completed';
  isPinned: boolean;
  tagIds: string[];
  remindAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MemoListData {
  items: MemoItem[];
  nextCursor: string | null;
  hasMore: boolean;
}

export async function listMemos(params: {
  status?: string;
  categoryId?: string;
  tagId?: string;
  cursor?: string;
  remindBefore?: string;
  limit?: number;
}): Promise<MemoListData> {
  const res = await apiClient.get<ApiResponse<MemoListData>>('/memos', { params });
  return res.data.data;
}

export async function getMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.get<ApiResponse<MemoItem>>(`/memos/${id}`);
  return res.data.data;
}

export async function createMemo(data: {
  title: string;
  content?: string;
  categoryId?: string;
  tagIds?: string[];
  isPinned?: boolean;
  remindAt?: string;
}): Promise<MemoItem> {
  const res = await apiClient.post<ApiResponse<MemoItem>>('/memos', data);
  return res.data.data;
}

export async function updateMemo(id: string, data: {
  title?: string;
  content?: string;
  categoryId?: string;
  tagIds?: string[];
  isPinned?: boolean;
}): Promise<MemoItem> {
  const res = await apiClient.patch<ApiResponse<MemoItem>>(`/memos/${id}`, data);
  return res.data.data;
}

export async function deleteMemo(id: string): Promise<void> {
  await apiClient.delete(`/memos/${id}`);
}

export async function pinMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.post<ApiResponse<MemoItem>>(`/memos/${id}/pin`);
  return res.data.data;
}

export async function unpinMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.delete<ApiResponse<MemoItem>>(`/memos/${id}/pin`);
  return res.data.data;
}

export async function archiveMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.post<ApiResponse<MemoItem>>(`/memos/${id}/archive`);
  return res.data.data;
}

export async function unarchiveMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.delete<ApiResponse<MemoItem>>(`/memos/${id}/archive`);
  return res.data.data;
}

export async function completeMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.post<ApiResponse<MemoItem>>(`/memos/${id}/complete`);
  return res.data.data;
}

export async function uncompleteMemo(id: string): Promise<MemoItem> {
  const res = await apiClient.delete<ApiResponse<MemoItem>>(`/memos/${id}/complete`);
  return res.data.data;
}

export async function getReminderCount(): Promise<number> {
  const res = await apiClient.get<ApiResponse<{ count: number }>>('/memos/reminder-count');
  return res.data.data.count;
}
