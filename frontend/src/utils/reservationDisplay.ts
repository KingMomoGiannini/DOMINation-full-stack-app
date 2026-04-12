/**
 * Presentación de reservas para la UI (sin datos inventados; solo formateo).
 */

export interface ReservationScheduleParts {
  headline: string;
  timeRange: string;
  crossDayNote: string | null;
}

const capitalizeEs = (s: string) => s.charAt(0).toLocaleUpperCase('es-AR') + s.slice(1);

export function formatReservationSchedule(startAt: string, endAt: string): ReservationScheduleParts {
  try {
    const start = new Date(startAt);
    const end = new Date(endAt);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return {
        headline: 'Fecha no disponible',
        timeRange: `${startAt} -> ${endAt}`,
        crossDayNote: null,
      };
    }

    const dayFmt = new Intl.DateTimeFormat('es-AR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
    const timeFmt = new Intl.DateTimeFormat('es-AR', {
      hour: '2-digit',
      minute: '2-digit',
    });

    const headline = capitalizeEs(dayFmt.format(start));
    const timeRange = `${timeFmt.format(start)} - ${timeFmt.format(end)}`;

    const sameCalendarDay =
      start.getFullYear() === end.getFullYear() &&
      start.getMonth() === end.getMonth() &&
      start.getDate() === end.getDate();

    const crossDayNote = sameCalendarDay
      ? null
      : `La reserva termina el ${capitalizeEs(dayFmt.format(end))} a las ${timeFmt.format(end)}.`;

    return { headline, timeRange, crossDayNote };
  } catch {
    return {
      headline: 'Fecha no disponible',
      timeRange: `${startAt} -> ${endAt}`,
      crossDayNote: null,
    };
  }
}

export interface ReservationStatusMeta {
  label: string;
  hint: string;
  modifier:
    | 'upcoming'
    | 'in-progress'
    | 'completed'
    | 'cancelled'
    | 'pending'
    | 'confirmed'
    | 'unknown';
}

export function getReservationOperationalMeta(status: string): ReservationStatusMeta {
  switch (status) {
    case 'UPCOMING':
      return {
        label: 'Próxima',
        hint: 'La franja todavía no empezó.',
        modifier: 'upcoming',
      };
    case 'IN_PROGRESS':
      return {
        label: 'En curso',
        hint: 'La franja está corriendo ahora.',
        modifier: 'in-progress',
      };
    case 'COMPLETED':
      return {
        label: 'Finalizada',
        hint: 'La franja ya terminó.',
        modifier: 'completed',
      };
    case 'CANCELLED':
      return {
        label: 'Cancelada',
        hint: 'La reserva ya no aplica para esta franja.',
        modifier: 'cancelled',
      };
    default:
      return {
        label: status,
        hint: 'Estado operativo reportado por el servidor.',
        modifier: 'unknown',
      };
  }
}

export function getReservationRecordMeta(status: string): ReservationStatusMeta {
  switch (status) {
    case 'PENDING':
      return {
        label: 'Registro pendiente',
        hint: 'Persistido como pendiente; útil para compatibilidad y futuros flujos explícitos de confirmación.',
        modifier: 'pending',
      };
    case 'CONFIRMED':
      return {
        label: 'Registro confirmado',
        hint: 'Persistido como reserva activa.',
        modifier: 'confirmed',
      };
    case 'CANCELLED':
      return {
        label: 'Registro cancelado',
        hint: 'Persistido como cancelado.',
        modifier: 'cancelled',
      };
    default:
      return {
        label: `Registro ${status}`,
        hint: 'Estado persistido reportado por el servidor.',
        modifier: 'unknown',
      };
  }
}
