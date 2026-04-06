import type { Branch, Item } from '../types/catalog';
import type { PageResponse } from '../types/page';
import { http } from './http';

export interface CreateBranchRequest {
  name: string;
  address: string;
}

export interface CreateRoomRequest {
  name: string;
  hourlyPrice: number;
}

export interface ProviderRequestResponse {
  id?: number;
  userId?: number;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt?: string;
  message?: string;
}

export const getMyBranches = async (): Promise<Branch[]> => {
  const { data } = await http.get<Branch[]>('/api/catalog/provider/branches');
  return data;
};

export const createBranch = async (request: CreateBranchRequest): Promise<Branch> => {
  const { data } = await http.post<Branch>('/api/catalog/provider/branches', request);
  return data;
};

export const updateBranch = async (id: number, request: CreateBranchRequest): Promise<Branch> => {
  const { data } = await http.put<Branch>(`/api/catalog/provider/branches/${id}`, request);
  return data;
};

export const deleteBranch = async (id: number): Promise<void> => {
  await http.delete(`/api/catalog/provider/branches/${id}`);
};

export const createRoom = async (branchId: number, request: CreateRoomRequest): Promise<Item> => {
  const { data } = await http.post<Item>(`/api/catalog/provider/branches/${branchId}/rooms`, request);
  return data;
};

export const setBranchActive = async (id: number, active: boolean): Promise<Branch> => {
  const { data } = await http.patch<Branch>(`/api/catalog/provider/branches/${id}/active`, { active });
  return data;
};

export const createProviderRequest = async (): Promise<ProviderRequestResponse> => {
  const { data } = await http.post<ProviderRequestResponse>('/auth/provider-requests');
  return data;
};

export const getMyProviderRequest = async (): Promise<ProviderRequestResponse | null> => {
  const { data } = await http.get<ProviderRequestResponse | { message?: string }>(
    '/auth/provider-requests/me'
  );
  if (data && typeof data === 'object' && 'message' in data && data.message === 'No tienes solicitudes') {
    return null;
  }
  return data as ProviderRequestResponse;
};

export type AdminProviderRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AdminProviderRequestsQuery {
  page?: number;
  size?: number;
  status?: AdminProviderRequestStatus | 'ALL';
  userId?: number;
  sort?: string;
}

export interface AdminProviderRequestSummary {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
}

export const getAdminProviderRequestSummary = async (): Promise<AdminProviderRequestSummary> => {
  const { data } = await http.get<AdminProviderRequestSummary>('/admin/provider-requests/summary');
  return data;
};

export const getAdminProviderRequestsPage = async (
  q: AdminProviderRequestsQuery
): Promise<PageResponse<ProviderRequestResponse>> => {
  const params = new URLSearchParams();
  params.set('page', String(q.page ?? 0));
  params.set('size', String(q.size ?? 20));
  params.set('sort', q.sort ?? 'createdAt,desc');
  if (q.userId != null && !Number.isNaN(q.userId)) {
    params.set('userId', String(q.userId));
  }
  if (q.status != null && q.status !== 'ALL') {
    params.set('status', q.status);
  }
  const { data } = await http.get<PageResponse<ProviderRequestResponse>>(
    `/admin/provider-requests?${params.toString()}`
  );
  return data;
};

export const approveProviderRequest = async (id: number): Promise<void> => {
  await http.post(`/admin/provider-requests/${id}/approve`);
};

export const rejectProviderRequest = async (id: number): Promise<void> => {
  await http.post(`/admin/provider-requests/${id}/reject`);
};
