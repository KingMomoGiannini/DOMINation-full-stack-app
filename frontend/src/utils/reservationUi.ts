import type { Reservation } from '../types/booking';

export type ReservationStatusFilter = 'ALL' | 'PENDING' | 'CONFIRMED' | 'CANCELLED';
export type ReservationTimeFilter = 'ALL' | 'UPCOMING' | 'PAST';
export type ReservationSortMode = 'START_DESC' | 'START_ASC';

export function reservationMatchesFilters(
  r: Reservation,
  opts: {
    branchId: number | 'ALL';
    status: ReservationStatusFilter;
    time: ReservationTimeFilter;
    nowMs?: number;
  }
): boolean {
  const now = opts.nowMs ?? Date.now();
  if (opts.branchId !== 'ALL' && r.branchId !== opts.branchId) return false;
  if (opts.status !== 'ALL' && r.status !== opts.status) return false;
  if (opts.time !== 'ALL') {
    const end = new Date(r.endAt).getTime();
    if (opts.time === 'UPCOMING') {
      if (r.status === 'CANCELLED') return false;
      return end >= now;
    }
    if (opts.time === 'PAST') {
      return end < now;
    }
  }
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

/** Pista operativa para escanear rápido (solo fechas del cliente). */
export function getReservationTemporalHint(startAt: string, endAt: string, nowMs?: number): string {
  const now = nowMs ?? Date.now();
  const start = new Date(startAt).getTime();
  const end = new Date(endAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) return '';

  if (now >= start && now <= end) return 'En curso ahora';

  const dayStart = new Date(now);
  dayStart.setHours(0, 0, 0, 0);
  const dayEnd = new Date(dayStart);
  dayEnd.setDate(dayEnd.getDate() + 1);

  if (end < now) return 'Franja pasada';

  if (start >= dayStart.getTime() && start < dayEnd.getTime()) return 'Hoy';

  const tom = new Date(dayStart);
  tom.setDate(tom.getDate() + 1);
  const tomEnd = new Date(tom);
  tomEnd.setDate(tomEnd.getDate() + 1);
  if (start >= tom.getTime() && start < tomEnd.getTime()) return 'Mañana';

  const diffDays = Math.ceil((start - dayStart.getTime()) / (86400 * 1000));
  if (diffDays === 2) return 'Pasado mañana';
  if (diffDays > 2 && diffDays <= 14) return `En ${diffDays} días`;
  if (diffDays > 14) return 'Próxima';
  return 'Próxima';
}

export function isReservationLiveNow(r: Reservation, nowMs?: number): boolean {
  if (r.status === 'CANCELLED') return false;
  const now = nowMs ?? Date.now();
  const start = new Date(r.startAt).getTime();
  const end = new Date(r.endAt).getTime();
  if (Number.isNaN(start) || Number.isNaN(end)) return false;
  return now >= start && now <= end;
}
