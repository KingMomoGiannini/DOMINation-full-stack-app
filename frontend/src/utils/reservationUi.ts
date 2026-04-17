import type { Reservation } from '../types/booking';

export type ReservationStatusFilter = 'ALL' | 'PENDING' | 'CONFIRMED' | 'CANCELLED';
export type ReservationTimeFilter = 'ALL' | 'UPCOMING' | 'IN_PROGRESS' | 'COMPLETED';
export type ReservationAttendanceFilter = 'ALL' | 'NOT_RECORDED' | 'CHECKED_IN' | 'NO_SHOW' | 'NOT_APPLICABLE';
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
    attendance?: ReservationAttendanceFilter;
    nowMs?: number;
  }
): boolean {
  if (opts.branchId !== 'ALL' && r.branchId !== opts.branchId) return false;
  if (opts.status !== 'ALL' && r.status !== opts.status) return false;
  if (opts.time !== 'ALL' && getOperationalStatus(r, opts.nowMs) !== opts.time) return false;
  if (opts.attendance && opts.attendance !== 'ALL' && r.attendanceStatus !== opts.attendance) return false;
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

  const tomorrow = new Date(dayStart);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowEnd = new Date(tomorrow);
  tomorrowEnd.setDate(tomorrowEnd.getDate() + 1);

  if (start >= tomorrow.getTime() && start < tomorrowEnd.getTime()) return 'Mañana';

  const diffDays = Math.ceil((start - dayStart.getTime()) / (86400 * 1000));
  if (diffDays === 2) return 'Pasado mañana';
  if (diffDays > 2 && diffDays <= 14) return `En ${diffDays} dias`;
  return 'Proxima';
}

export function isReservationLiveNow(r: Reservation, nowMs?: number): boolean {
  return getOperationalStatus(r, nowMs) === 'IN_PROGRESS';
}

export function getCancellationMessage(r: Reservation): string {
  if (r.cancellable) {
    return 'Disponible hasta antes del inicio de la franja.';
  }
  if (r.cancellationBlockReason === 'ALREADY_CANCELLED') {
    return 'No hace falta cancelarla: la reserva ya figura como cancelada.';
  }
  if (r.cancellationBlockReason === 'ALREADY_STARTED') {
    return 'No puede cancelarse porque la franja ya comenzo o ya paso.';
  }
  return 'La accion queda sujeta a la politica vigente informada por el backend.';
}

export function getAttendanceMessage(r: Reservation): string {
  if (r.attendanceStatus === 'CHECKED_IN') {
    return r.checkedInAt
      ? `Check-in registrado el ${formatCompactDateTime(r.checkedInAt)}.`
      : 'Check-in registrado por el prestador.';
  }
  if (r.attendanceStatus === 'NO_SHOW') {
    return r.noShowMarkedAt
      ? `No-show marcado el ${formatCompactDateTime(r.noShowMarkedAt)}.`
      : 'El prestador marcó la reserva como no-show.';
  }
  if (r.attendanceStatus === 'NOT_APPLICABLE') {
    return 'Sin control de asistencia porque la reserva fue cancelada.';
  }
  return 'Todavía no hay un hecho operativo de asistencia persistido.';
}

export function getProviderCheckInMessage(r: Reservation): string {
  if (r.providerCheckInAllowed) {
    return 'Disponible mientras la franja esté en curso.';
  }
  switch (r.providerCheckInBlockReason) {
    case 'CANCELLED':
      return 'Bloqueado porque la reserva fue cancelada.';
    case 'ALREADY_CHECKED_IN':
      return 'Ya se registró la llegada del cliente.';
    case 'ALREADY_MARKED_NO_SHOW':
      return 'Bloqueado porque la reserva ya fue marcada como no-show.';
    case 'BEFORE_START':
      return 'Se habilita cuando comienza la franja.';
    case 'AFTER_END':
      return 'La franja ya terminó.';
    default:
      return 'La disponibilidad depende de la política vigente del backend.';
  }
}

export function getProviderNoShowMessage(r: Reservation): string {
  if (r.providerMarkNoShowAllowed) {
    return 'Disponible una vez terminada la franja y si no hubo check-in.';
  }
  switch (r.providerMarkNoShowBlockReason) {
    case 'CANCELLED':
      return 'Bloqueado porque la reserva fue cancelada.';
    case 'ALREADY_CHECKED_IN':
      return 'No corresponde: ya hay check-in registrado.';
    case 'ALREADY_MARKED_NO_SHOW':
      return 'Ya fue marcada como no-show.';
    case 'BEFORE_END':
      return 'Se habilita cuando la franja termina.';
    default:
      return 'La disponibilidad depende de la política vigente del backend.';
  }
}

function formatCompactDateTime(value: string): string {
  try {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat('es-AR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(date);
  } catch {
    return value;
  }
}
