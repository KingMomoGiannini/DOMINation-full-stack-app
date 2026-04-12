import type { Reservation } from '../types/booking';

export type ReservationStatusFilter = 'ALL' | 'PENDING' | 'CONFIRMED' | 'CANCELLED';
export type ReservationTimeFilter = 'ALL' | 'UPCOMING' | 'IN_PROGRESS' | 'COMPLETED';
export type ReservationSortMode = 'START_DESC' | 'START_ASC';

function deriveOperationalStatusFallback(r: Reservation, nowMs: number): Reservation['operationalStatus'] {
  if (r.status === 'CANCELLED') return 'CANCELLED';
  const start = new Date(r.startAt).getTime();
  const end = new Date(r.endAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) return 'UPCOMING';
  if (nowMs < start) return 'UPCOMING';
  if (nowMs < end) return 'IN_PROGRESS';
  return 'COMPLETED';
}

export function getOperationalStatus(r: Reservation, nowMs?: number): Reservation['operationalStatus'] {
  if (r.operationalStatus) return r.operationalStatus;
  return deriveOperationalStatusFallback(r, nowMs ?? Date.now());
}

export function reservationMatchesFilters(
  r: Reservation,
  opts: {
    branchId: number | 'ALL';
    status: ReservationStatusFilter;
    time: ReservationTimeFilter;
    nowMs?: number;
  }
): boolean {
  if (opts.branchId !== 'ALL' && r.branchId !== opts.branchId) return false;
  if (opts.status !== 'ALL' && r.status !== opts.status) return false;
  if (opts.time !== 'ALL' && getOperationalStatus(r, opts.nowMs) !== opts.time) return false;
  return true;
}

export function sortReservations(list: Reservation[], mode: ReservationSortMode): Reservation[] {
  const copy = [...list];
  copy.sort((a, b) => {
    const ta = new Date(a.startAt).getTime();
    const tb = new Date(b.startAt).getTime();
    return mode === 'START_ASC' ? ta - tb : tb - ta;
  });
  return copy;
}

export function getReservationTemporalHint(r: Reservation, nowMs?: number): string {
  const operationalStatus = getOperationalStatus(r, nowMs);
  if (operationalStatus === 'CANCELLED') {
    return 'Reserva cancelada';
  }

  const now = nowMs ?? Date.now();
  const start = new Date(r.startAt).getTime();
  const end = new Date(r.endAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) return '';

  if (operationalStatus === 'IN_PROGRESS') return 'En curso ahora';
  if (operationalStatus === 'COMPLETED') return 'Franja finalizada';

  const dayStart = new Date(now);
  dayStart.setHours(0, 0, 0, 0);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);

  if (start >= dayStart.getTime() && start < dayEnd.getTime()) return 'Hoy';

  const tom = new Date(dayStart);
  tom.setDate(tom.getDate() + 1);
  const tomEnd = new Date(tom);
  tomEnd.setDate(tomEnd.getDate() + 1);
  if (start >= tom.getTime() && start < tomEnd.getTime()) return 'Mañana';

  const diffDays = Math.ceil((start - dayStart.getTime()) / (86400 * 1000));
  if (diffDays === 2) return 'Pasado mañana';
  if (diffDays > 2 && diffDays <= 14) return `En ${diffDays} días`;
  return 'Próxima';
}

export function isReservationLiveNow(r: Reservation, nowMs?: number): boolean {
  return getOperationalStatus(r, nowMs) === 'IN_PROGRESS';
}

export function getCancellationMessage(r: Reservation): string {
  if (r.cancellable) {
    return 'Se puede cancelar antes del inicio de la franja.';
  }
  if (r.cancellationBlockReason === 'ALREADY_CANCELLED') {
    return 'Ya fue cancelada.';
  }
  if (r.cancellationBlockReason === 'ALREADY_STARTED') {
    return 'Solo puede cancelarse antes del inicio.';
  }
  return 'La cancelación depende de la regla vigente del backend.';
}
