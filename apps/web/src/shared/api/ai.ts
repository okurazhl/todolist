import apiClient, { type ApiResponse } from './client';

export async function refineContent(content: string): Promise<string> {
  const res = await apiClient.post<ApiResponse<{ refined: string }>>('/ai/refine', { content });
  return res.data.data.refined;
}

export interface ParsedNLResult {
  title: string;
  event: string;
  datetime: string;
  isPast: boolean;
  suggestedTime: string | null;
  suggestedLabel: string | null;
}

export async function parseNaturalLanguage(content: string): Promise<ParsedNLResult> {
  const res = await apiClient.post<ApiResponse<ParsedNLResult>>('/ai/parse-nl', { content });
  return res.data.data;
}
