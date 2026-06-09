import apiClient, { type ApiResponse } from './client';
import type { MemoItem } from './memos';

export interface PullResponse {
  items: MemoItem[];
  cursor: string;
  hasMore: boolean;
}

export interface ConflictRecord {
  memoId: string;
  serverVersion: MemoItem;
  clientVersion: MemoItem;
}

export interface PushResult {
  accepted: MemoItem[];
  conflicts: ConflictRecord[];
}

export async function pullChanges(cursor?: string): Promise<PullResponse> {
  const params = cursor ? { cursor } : {};
  const res = await apiClient.get<ApiResponse<PullResponse>>('/sync/pull', { params });
  return res.data.data;
}

export async function pushChanges(changes: MemoItem[], baseCursor: string): Promise<PushResult> {
  const res = await apiClient.post<ApiResponse<PushResult>>('/sync/push', {
    changes,
    baseCursor,
  });
  return res.data.data;
}
