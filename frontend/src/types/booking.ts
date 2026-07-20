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
  operationalStatus: 'UPCOMING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  attendanceStatus: 'NOT_RECORDED' | 'CHECKED_IN' | 'NO_SHOW' | 'NOT_APPLICABLE';
  cancellable: boolean;
  cancellationBlockReason?: 'ALREADY_CANCELLED' | 'ALREADY_STARTED' | null;
  checkedInAt?: string | null;
  noShowMarkedAt?: string | null;
  providerCheckInAllowed: boolean;
  providerCheckInBlockReason?:
    | 'CANCELLED'
    | 'ALREADY_CHECKED_IN'
    | 'ALREADY_MARKED_NO_SHOW'
    | 'BEFORE_START'
    | 'AFTER_END'
    | 'BEFORE_END'
    | null;
  providerMarkNoShowAllowed: boolean;
  providerMarkNoShowBlockReason?:
    | 'CANCELLED'
    | 'ALREADY_CHECKED_IN'
    | 'ALREADY_MARKED_NO_SHOW'
    | 'BEFORE_START'
    | 'AFTER_END'
    | 'BEFORE_END'
    | null;
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

export type ReservationAuditEventType =
  | 'CREATED'
  | 'CANCELLED_BY_CUSTOMER'
  | 'CHECKED_IN'
  | 'MARKED_NO_SHOW';

export interface ReservationAuditEvent {
  id: number;
  reservationId: number;
  actorUserId: string;
  actorRole: string;
  eventType: ReservationAuditEventType;
  reason?: string | null;
  comment?: string | null;
  createdAt: string;
}
