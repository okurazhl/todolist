import apiClient, { type ApiResponse } from './client';

export interface CategoryItem {
  id: string;
  name: string;
  color: string | null;
  sortOrder: number;
}

export async function listCategories(): Promise<CategoryItem[]> {
  const res = await apiClient.get<ApiResponse<{ items: CategoryItem[] }>>('/categories');
  return res.data.data.items;
}

export async function createCategory(name: string, color?: string): Promise<CategoryItem> {
  const res = await apiClient.post<ApiResponse<CategoryItem>>('/categories', { name, color, sortOrder: 0 });
  return res.data.data;
}

export async function deleteCategory(id: string): Promise<void> {
  await apiClient.delete(`/categories/${id}`);
}
