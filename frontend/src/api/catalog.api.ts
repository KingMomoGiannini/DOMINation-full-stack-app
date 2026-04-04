import type { Branch, Item } from '../types/catalog';
import { http } from './http';

export type { Branch, Item } from '../types/catalog';

export const getBranches = async (): Promise<Branch[]> => {
  const { data } = await http.get<Branch[]>('/api/catalog/branches');
  return data;
};

export const getItems = async (branchId?: number, type?: string): Promise<Item[]> => {
  const params = new URLSearchParams();
  if (branchId != null) params.append('branchId', branchId.toString());
  if (type) params.append('type', type);
  const qs = params.toString();
  const { data } = await http.get<Item[]>(`/api/catalog/items${qs ? `?${qs}` : ''}`);
  return data;
};
