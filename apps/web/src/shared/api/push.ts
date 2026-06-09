import apiClient, { type ApiResponse } from './client';

export interface PushMessage {
  id: string;
  userId: string;
  memoId: string | null;
  type: string;
  title: string;
  body: string | null;
  read: boolean;
  createdAt: string;
}

export interface PushListData {
  items: PushMessage[];
  unreadCount: number;
}

export async function listPushMessages(): Promise<PushListData> {
  const res = await apiClient.get<ApiResponse<PushListData>>('/push/messages');
  return res.data.data;
}

export async function getUnreadPushCount(): Promise<number> {
  const res = await apiClient.get<ApiResponse<{ count: number }>>('/push/unread-count');
  return res.data.data.count;
}

export async function markPushRead(id: string): Promise<void> {
  await apiClient.post(`/push/messages/${id}/read`);
}

export async function markAllPushRead(): Promise<void> {
  await apiClient.post('/push/messages/read-all');
}
