import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getReservationAudit } from '../../api';
import type {
  ReservationAuditEvent,
  ReservationAuditEventType,
} from '../../types/booking';
import { getApiErrorMessage } from '../../utils/apiError';

const eventLabels: Record<ReservationAuditEventType, string> = {
  CREATED: 'Reserva creada',
  CANCELLED_BY_CUSTOMER: 'Cancelada por cliente',
  CHECKED_IN: 'Check-in registrado',
  MARKED_NO_SHOW: 'No-show registrado',
};

const reasonLabels: Record<string, string> = {
  RESERVATION_CREATED: 'Creación de la reserva',
  CUSTOMER_REQUEST: 'Solicitud del cliente',
  PROVIDER_CONFIRMED_ATTENDANCE: 'Asistencia confirmada por el provider',
  CUSTOMER_DID_NOT_ATTEND: 'El cliente no se presentó',
};

const actorRoleLabels: Record<string, string> = {
  ROLE_USER: 'Cliente',
  ROLE_PROVIDER: 'Provider',
  ROLE_ADMIN: 'Administrador',
};

function formatEventDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'Fecha no disponible';
  }

  return new Intl.DateTimeFormat('es-AR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}

function formatReason(reason: string): string {
  return reasonLabels[reason] ?? reason;
}

function renderEvent(event: ReservationAuditEvent) {
  const actorRole = actorRoleLabels[event.actorRole] ?? event.actorRole;

  return (
    <li key={event.id} className="reservation-audit__event">
      <span className="reservation-audit__marker" aria-hidden="true" />
      <div className="reservation-audit__event-body">
        <div className="reservation-audit__event-heading">
          <strong>{eventLabels[event.eventType] ?? event.eventType}</strong>
          <time dateTime={event.createdAt}>{formatEventDate(event.createdAt)}</time>
        </div>
        <p className="reservation-audit__actor">
          Actor: {actorRole} · ID {event.actorUserId}
        </p>
        {event.reason?.trim() ? (
          <p className="reservation-audit__detail">
            <span>Motivo</span>
            {formatReason(event.reason.trim())}
          </p>
        ) : null}
        {event.comment?.trim() ? (
          <p className="reservation-audit__detail">
            <span>Comentario</span>
            {event.comment.trim()}
          </p>
        ) : null}
      </div>
    </li>
  );
}

export function ReservationAuditTimeline({ reservationId }: { reservationId: number }) {
  const [isOpen, setIsOpen] = useState(false);
  const panelId = `reservation-audit-${reservationId}`;
  const auditQuery = useQuery({
    queryKey: ['reservationAudit', reservationId],
    queryFn: () => getReservationAudit(reservationId),
    enabled: isOpen,
  });

  return (
    <section className={`reservation-audit${isOpen ? ' reservation-audit--open' : ''}`}>
      <button
        type="button"
        className="reservation-audit__toggle"
        aria-expanded={isOpen}
        aria-controls={panelId}
        onClick={() => setIsOpen((current) => !current)}
      >
        <span>
          <strong>Historial operativo</strong>
          <small>Acciones registradas sobre esta reserva</small>
        </span>
        <span className="reservation-audit__toggle-label">
          {isOpen ? 'Ocultar historial' : 'Ver historial'}
        </span>
      </button>

      {isOpen ? (
        <div id={panelId} className="reservation-audit__panel">
          {auditQuery.isPending ? (
            <p className="reservation-audit__state" role="status" aria-live="polite">
              Cargando historial…
            </p>
          ) : null}

          {auditQuery.isError ? (
            <div className="reservation-audit__error" role="alert">
              <p>
                {getApiErrorMessage(
                  auditQuery.error,
                  'No pudimos cargar el historial de esta reserva.'
                )}
              </p>
              <button
                type="button"
                className="reservation-audit__retry"
                onClick={() => auditQuery.refetch()}
              >
                Reintentar
              </button>
            </div>
          ) : null}

          {auditQuery.isSuccess && auditQuery.data.length === 0 ? (
            <p className="reservation-audit__state">No hay eventos registrados.</p>
          ) : null}

          {auditQuery.isSuccess && auditQuery.data.length > 0 ? (
            <ol className="reservation-audit__list">{auditQuery.data.map(renderEvent)}</ol>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
