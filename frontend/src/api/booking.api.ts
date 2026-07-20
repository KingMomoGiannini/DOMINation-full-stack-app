import type {
  AvailabilityResponse,
  CreateReservationRequest,
  Reservation,
  ReservationAuditEvent,
} from '../types/booking';
import type { PageResponse } from '../types/page';
import { http } from './http';

export type {
  AvailabilityResponse,
  CreateReservationRequest,
  Reservation,
  ReservationAuditEvent,
  ReservationAuditEventType,
  ReservationLine,
  AvailabilityConflict,
} from '../types/booking';

export const getMyReservations = async (): Promise<Reservation[]> => {
  const { data } = await http.get<Reservation[]>('/api/booking/my/reservations');
  return data;
};

export const checkAvailability = async (
  request: CreateReservationRequest
): Promise<AvailabilityResponse> => {
  const { data } = await http.post<AvailabilityResponse>('/api/booking/availability', request);
  return data;
};

export const createReservation = async (request: CreateReservationRequest): Promise<Reservation> => {
  const { data } = await http.post<Reservation>('/api/booking/reservations', request);
  return data;
};

export const cancelReservation = async (id: number): Promise<Reservation> => {
  const { data } = await http.post<Reservation>(`/api/booking/reservations/${id}/cancel`);
  return data;
};

export const getReservationAudit = async (
  reservationId: number
): Promise<ReservationAuditEvent[]> => {
  const { data } = await http.get<ReservationAuditEvent[]>(
    `/api/booking/reservations/${reservationId}/audit`
  );
  return data;
};

export interface ProviderReservationsQuery {
  page?: number;
  size?: number;
  branchId?: number;
  status?: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  attendance?: 'ALL' | 'NOT_RECORDED' | 'CHECKED_IN' | 'NO_SHOW' | 'NOT_APPLICABLE';
  time?: 'ALL' | 'UPCOMING' | 'IN_PROGRESS' | 'COMPLETED';
  from?: string;
  to?: string;
  sort?: string;
}

export interface ProviderReservationMetrics {
  total: number;
  cancelled: number;
  upcoming: number;
  inProgress: number;
  completed: number;
  checkedIn: number;
  noShow: number;
}

export const getProviderReservationsPage = async (
  q: ProviderReservationsQuery
): Promise<PageResponse<Reservation>> => {
  const params = new URLSearchParams();
  params.set('page', String(q.page ?? 0));
  params.set('size', String(q.size ?? 15));
  params.set('time', q.time ?? 'ALL');
  params.set('attendance', q.attendance ?? 'ALL');
  params.set('sort', q.sort ?? 'startAt,desc');
  if (q.branchId != null) params.set('branchId', String(q.branchId));
  if (q.status != null) params.set('status', q.status);
  if (q.from) params.set('from', q.from);
  if (q.to) params.set('to', q.to);
  const { data } = await http.get<PageResponse<Reservation>>(
    `/api/booking/provider/reservations?${params.toString()}`
  );
  return data;
};

export const getProviderReservationMetrics = async (): Promise<ProviderReservationMetrics> => {
  const { data } = await http.get<ProviderReservationMetrics>('/api/booking/provider/reservations/metrics');
  return data;
};

export const providerCheckInReservation = async (id: number): Promise<Reservation> => {
  const { data } = await http.post<Reservation>(`/api/booking/provider/reservations/${id}/check-in`);
  return data;
};

export const providerMarkNoShowReservation = async (id: number): Promise<Reservation> => {
  const { data } = await http.post<Reservation>(`/api/booking/provider/reservations/${id}/no-show`);
  return data;
};
