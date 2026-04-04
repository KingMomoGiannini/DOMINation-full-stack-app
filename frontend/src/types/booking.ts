export interface ReservationLine {
  id: number;
  itemId: number;
  /** Nombre legible del ítem cuando el backend lo envía (Sprint 8). */
  itemName?: string | null;
  quantity: number;
  price: number;
}

export interface Reservation {
  id: number;
  customerId: string;
  /** Username para contexto del prestador; no expone email (Sprint 8). */
  customerUsername?: string | null;
  branchId: number;
  /** Nombre de sucursal cuando el backend lo persiste o enriquece (Sprint 8). */
  branchName?: string | null;
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
