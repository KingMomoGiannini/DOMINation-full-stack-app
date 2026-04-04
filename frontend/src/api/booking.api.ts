import type {
  AvailabilityResponse,
  CreateReservationRequest,
  Reservation,
} from '../types/booking';
import { http } from './http';

export type {
  AvailabilityResponse,
  CreateReservationRequest,
  Reservation,
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

export const getProviderReservations = async (): Promise<Reservation[]> => {
  const { data } = await http.get<Reservation[]>('/api/booking/provider/reservations');
  return data;
};
