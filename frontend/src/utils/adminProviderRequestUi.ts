import type { ProviderRequestResponse } from '../api';

export type AdminRequestStatusTab = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';
export type AdminRequestSortMode = 'CREATED_DESC' | 'CREATED_ASC';

export function countProviderRequestsByStatus(rows: ProviderRequestResponse[]): {
  total: number;
  pending: number;
  approved: number;
  rejected: number;
} {
  let pending = 0;
  let approved = 0;
  let rejected = 0;
  for (const r of rows) {
    const s = r.status;
    if (s === 'PENDING') pending += 1;
    else if (s === 'APPROVED') approved += 1;
    else if (s === 'REJECTED') rejected += 1;
  }
  return { total: rows.length, pending, approved, rejected };
}

export function filterProviderRequestsByTab(
  rows: ProviderRequestResponse[],
  tab: AdminRequestStatusTab
): ProviderRequestResponse[] {
  if (tab === 'ALL') return rows;
  return rows.filter((r) => r.status === tab);
}

/** Coincidencia por subcadena en el id de usuario (solo dígitos en la búsqueda; vacío = sin filtrar por esto). */
export function filterProviderRequestsByUserQuery(
  rows: ProviderRequestResponse[],
  rawQuery: string
): ProviderRequestResponse[] {
  const q = rawQuery.trim();
  if (!q) return rows;
  if (!/^\d+$/.test(q)) return rows.filter(() => false);
  return rows.filter((r) => r.userId != null && String(r.userId).includes(q));
}

export function sortProviderRequests(
  rows: ProviderRequestResponse[],
  mode: AdminRequestSortMode
): ProviderRequestResponse[] {
  const copy = [...rows];
  copy.sort((a, b) => {
    const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
    const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
    const na = Number.isNaN(ta) ? 0 : ta;
    const nb = Number.isNaN(tb) ? 0 : tb;
    return mode === 'CREATED_ASC' ? na - nb : nb - na;
  });
  return copy;
}
