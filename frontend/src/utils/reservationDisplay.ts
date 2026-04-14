import type { ReservationLine } from '../types/booking';

/**
 * Presentation helpers for reservation cards.
 * They only format data already present in the contract.
 */

export interface ReservationScheduleParts {
  headline: string;
  timeRange: string;
  durationLabel: string | null;
  crossDayNote: string | null;
}

const capitalizeEs = (value: string) => value.charAt(0).toLocaleUpperCase('es-AR') + value.slice(1);

export function formatReservationSchedule(startAt: string, endAt: string): ReservationScheduleParts {
  try {
    const start = new Date(startAt);
    const end = new Date(endAt);

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      return {
        headline: 'Fecha no disponible',
        timeRange: `${startAt} -> ${endAt}`,
        durationLabel: null,
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
    const durationLabel = formatDurationLabel(start, end);

    const sameCalendarDay =
      start.getFullYear() === end.getFullYear() &&
      start.getMonth() === end.getMonth() &&
      start.getDate() === end.getDate();

    const crossDayNote = sameCalendarDay
      ? null
      : `La reserva termina el ${capitalizeEs(dayFmt.format(end))} a las ${timeFmt.format(end)}.`;

    return { headline, timeRange, durationLabel, crossDayNote };
  } catch {
    return {
      headline: 'Fecha no disponible',
      timeRange: `${startAt} -> ${endAt}`,
      durationLabel: null,
      crossDayNote: null,
    };
  }
}

function formatDurationLabel(start: Date, end: Date): string | null {
  const minutes = Math.max(0, Math.round((end.getTime() - start.getTime()) / 60000));
  if (minutes <= 0) return null;

  const hours = Math.floor(minutes / 60);
  const remainingMinutes = minutes % 60;

  if (hours > 0 && remainingMinutes > 0) {
    return `${hours} h ${remainingMinutes} min`;
  }
  if (hours > 0) {
    return `${hours} h`;
  }
  return `${remainingMinutes} min`;
}

export interface ReservationSubjectSummary {
  title: string;
  subtitle: string;
}

export function getReservationSubjectSummary(lines: ReservationLine[]): ReservationSubjectSummary {
  const namedItems = Array.from(
    new Set(
      (lines ?? [])
        .map((line) => line.itemName?.trim())
        .filter((value): value is string => Boolean(value))
    )
  );

  if (namedItems.length === 0) {
    const count = lines?.length ?? 0;
    return {
      title: count > 0 ? `Reserva con ${count} item${count === 1 ? '' : 's'}` : 'Reserva registrada',
      subtitle:
        count > 0
          ? 'El detalle existe, pero no llego enriquecido con nombres legibles.'
          : 'Todavia no hay items visibles asociados a esta reserva.',
    };
  }

  if (namedItems.length === 1) {
    return {
      title: namedItems[0],
      subtitle: 'Reserva enfocada en un unico item o servicio.',
    };
  }

  return {
    title: namedItems[0],
    subtitle:
      namedItems.length === 2
        ? `Incluye tambien ${namedItems[1]}.`
        : `Incluye tambien ${namedItems[1]} y ${namedItems.length - 2} item${namedItems.length - 2 === 1 ? '' : 's'} mas.`,
  };
}

export function formatReservationCreatedAt(createdAt: string): string {
  try {
    const created = new Date(createdAt);
    if (Number.isNaN(created.getTime())) return 'Creada recientemente';

    return new Intl.DateTimeFormat('es-AR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(created);
  } catch {
    return 'Creada recientemente';
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
        label: 'Proxima',
        hint: 'La franja todavia no empezo.',
        modifier: 'upcoming',
      };
    case 'IN_PROGRESS':
      return {
        label: 'En curso',
        hint: 'La franja esta corriendo ahora.',
        modifier: 'in-progress',
      };
    case 'COMPLETED':
      return {
        label: 'Finalizada',
        hint: 'La franja ya termino.',
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
        hint: 'Persistido como pendiente; reservado para flujos futuros de confirmacion explicita.',
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
