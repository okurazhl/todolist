import apiClient, { type ApiResponse } from './client';

export interface AsrTask {
  taskId: string;
  status: 'pending' | 'processing' | 'completed' | 'failed';
  fileName: string;
  fileSize: number;
  memoId: string | null;
  transcribedText: string | null;
  durationSeconds: number | null;
  errorMessage: string | null;
  createdAt: string;
  completedAt: string | null;
}

export async function uploadAudio(file: File, memoId?: string): Promise<AsrTask> {
  const form = new FormData();
  form.append('file', file);
  if (memoId) form.append('memo_id', memoId);

  const res = await apiClient.post<ApiResponse<AsrTask>>('/asr/tasks', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data.data;
}

export async function getTask(taskId: string): Promise<AsrTask> {
  const res = await apiClient.get<ApiResponse<AsrTask>>(`/asr/tasks/${taskId}`);
  return res.data.data;
}

export async function listTasks(): Promise<AsrTask[]> {
  const res = await apiClient.get<ApiResponse<{ items: AsrTask[] }>>('/asr/tasks');
  return res.data.data.items;
}
