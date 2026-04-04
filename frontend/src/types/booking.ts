export interface ReservationLine {
  id: number;
  itemId: number;
  quantity: number;
  price: number;
}

export interface Reservation {
  id: number;
  customerId: string;
  branchId: number;
  providerId?: number;
  startAt: string;
  endAt: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
  lines: ReservationLine[];
}

export interface CreateReservationRequest {
  branchId: number;
  startAt: string;
  endAt: string;
  lines: { itemId: number; quantity: number }[];
}

export interface AvailabilityConflict {
  itemId?: number;
  reason?: string;
  detail?: string;
  requestedQty?: number;
  availableQty?: number;
  reservedQty?: number;
}

export interface AvailabilityResponse {
  available: boolean;
  conflicts: AvailabilityConflict[];
}
