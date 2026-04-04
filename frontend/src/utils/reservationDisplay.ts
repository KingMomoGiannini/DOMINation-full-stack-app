/**
 * Presentación de reservas para la UI (sin datos inventados; solo formateo).
 */

export interface ReservationScheduleParts {
  /** Título principal: día y fecha legibles */
  headline: string;
  /** Franja horaria del mismo día (se asume misma zona que el backend) */
  timeRange: string;
  /** Texto auxiliar si inicio y fin cruzan medianoche */
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
        timeRange: `${startAt} → ${endAt}`,
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
    const timeRange = `${timeFmt.format(start)} – ${timeFmt.format(end)}`;

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
      timeRange: `${startAt} → ${endAt}`,
      crossDayNote: null,
    };
  }
}

export interface ReservationStatusMeta {
  label: string;
  hint: string;
  modifier: 'pending' | 'confirmed' | 'cancelled' | 'unknown';
}

export function getReservationStatusMeta(status: string): ReservationStatusMeta {
  switch (status) {
    case 'PENDING':
      return {
        label: 'Pendiente',
        hint: 'La reserva está registrada; el estado puede actualizarse según las reglas del negocio.',
        modifier: 'pending',
      };
    case 'CONFIRMED':
      return {
        label: 'Confirmada',
        hint: 'Reserva activa para la franja indicada.',
        modifier: 'confirmed',
      };
    case 'CANCELLED':
      return {
        label: 'Cancelada',
        hint: 'Esta reserva ya no aplica para la franja.',
        modifier: 'cancelled',
      };
    default:
      return {
        label: status,
        hint: 'Estado reportado por el servidor.',
        modifier: 'unknown',
      };
  }
}
