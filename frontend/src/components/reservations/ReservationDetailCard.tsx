import type { Reservation } from '../../types/booking';
import {
  formatReservationCreatedAt,
  getReservationAttendanceMeta,
  formatReservationSchedule,
  getReservationOperationalMeta,
  getReservationRecordMeta,
  getReservationSubjectSummary,
} from '../../utils/reservationDisplay';
import {
  getAttendanceMessage,
  getCancellationMessage,
  getProviderCheckInMessage,
  getProviderNoShowMessage,
  getReservationTemporalHint,
  isReservationLiveNow,
} from '../../utils/reservationUi';
import { ReservationLineItems } from './ReservationLineItems';
import { ReservationScheduleBlock } from './ReservationScheduleBlock';
import { ReservationStatusBadge } from './ReservationStatusBadge';

interface ReservationDetailCardProps {
  reservation: Reservation;
  audience: 'user' | 'provider';
  branch: {
    primary: string;
    secondary?: string | null;
  };
  cancelAction?: {
    onRequestCancel: (reservationId: number) => void;
    loading: boolean;
  };
  providerActions?: {
    onRequestCheckIn: (reservationId: number) => void;
    onRequestNoShow: (reservationId: number) => void;
    loading: boolean;
  };
}

function getEyebrow(reservation: Reservation, audience: 'user' | 'provider') {
  const prefix = audience === 'provider' ? 'Agenda operativa' : 'Tu reserva';

  switch (reservation.operationalStatus) {
    case 'UPCOMING':
      return `${prefix} proxima`;
    case 'IN_PROGRESS':
      return `${prefix} en curso`;
    case 'COMPLETED':
      return `${prefix} finalizada`;
    case 'CANCELLED':
      return `${prefix} cancelada`;
    default:
      return prefix;
  }
}

function renderCustomerSummary(reservation: Reservation) {
  if (reservation.customerUsername?.trim()) {
    return {
      title: `@${reservation.customerUsername.trim()}`,
      detail: `ID interno ${reservation.customerId}`,
    };
  }

  return {
    title: reservation.customerId,
    detail: 'Sin username disponible en esta respuesta.',
  };
}

export function ReservationDetailCard({
  reservation,
  audience,
  branch,
  cancelAction,
  providerActions,
}: ReservationDetailCardProps) {
  const schedule = formatReservationSchedule(reservation.startAt, reservation.endAt);
  const operationalMeta = getReservationOperationalMeta(reservation.operationalStatus);
  const recordMeta = getReservationRecordMeta(reservation.status);
  const attendanceMeta = getReservationAttendanceMeta(reservation.attendanceStatus);
  const subject = getReservationSubjectSummary(reservation.lines ?? []);
  const temporal = getReservationTemporalHint(reservation);
  const live = isReservationLiveNow(reservation);
  const customer = renderCustomerSummary(reservation);

  return (
    <article className={`reservation-card${live ? ' reservation-card--live' : ''}`}>
      <div className="reservation-card__hero">
        <div className="reservation-card__main-col">
          <div className="reservation-card__badges">
            <ReservationStatusBadge meta={operationalMeta} />
            <ReservationStatusBadge meta={recordMeta} />
            <ReservationStatusBadge meta={attendanceMeta} />
          </div>
          <p className="reservation-card__eyebrow">{getEyebrow(reservation, audience)}</p>
          <h3 className="reservation-card__title">{subject.title}</h3>
          <p className="reservation-card__summary">{subject.subtitle}</p>
          <ReservationScheduleBlock schedule={schedule} />
          {temporal ? <span className="reservation-card__temporal">{temporal}</span> : null}
        </div>

        {cancelAction ? (
          <div className="reservation-card__actions">
            {reservation.cancellable ? (
              <button
                type="button"
                className="btn btn-logout"
                disabled={cancelAction.loading}
                onClick={() => cancelAction.onRequestCancel(reservation.id)}
              >
                Cancelar reserva
              </button>
            ) : (
              <div className="reservation-card__action-note">
                <span className="reservation-card__action-label">Cancelacion</span>
                <strong>No disponible</strong>
                <small>{getCancellationMessage(reservation)}</small>
              </div>
            )}
          </div>
        ) : null}

        {providerActions ? (
          <div className="reservation-card__actions">
            <button
              type="button"
              className="btn btn-success"
              disabled={!reservation.providerCheckInAllowed || providerActions.loading}
              onClick={() => providerActions.onRequestCheckIn(reservation.id)}
            >
              Registrar check-in
            </button>
            <small className="reservation-card__action-note">{getProviderCheckInMessage(reservation)}</small>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={!reservation.providerMarkNoShowAllowed || providerActions.loading}
              onClick={() => providerActions.onRequestNoShow(reservation.id)}
            >
              Marcar no-show
            </button>
            <small className="reservation-card__action-note">{getProviderNoShowMessage(reservation)}</small>
          </div>
        ) : null}
      </div>

      <div className="reservation-card__facts">
        <div className="reservation-detail-item">
          <span className="reservation-detail-item__label">Sucursal</span>
          <strong>{branch.primary}</strong>
          {branch.secondary ? <small>{branch.secondary}</small> : null}
        </div>

        {audience === 'provider' ? (
          <div className="reservation-detail-item">
            <span className="reservation-detail-item__label">Cliente</span>
            <strong>{customer.title}</strong>
            <small>{customer.detail}</small>
          </div>
        ) : (
          <div className="reservation-detail-item">
            <span className="reservation-detail-item__label">Estado operativo</span>
            <strong>{operationalMeta.label}</strong>
            <small>{operationalMeta.hint}</small>
          </div>
        )}

        <div
          className={`reservation-detail-item${reservation.cancellable ? ' reservation-detail-item--positive' : ''}`}
        >
          <span className="reservation-detail-item__label">Cancelacion</span>
          <strong>{reservation.cancellable ? 'Disponible' : 'No disponible'}</strong>
          <small>{getCancellationMessage(reservation)}</small>
        </div>

        <div className="reservation-detail-item">
          <span className="reservation-detail-item__label">Operacion real</span>
          <strong>{attendanceMeta.label}</strong>
          <small>{getAttendanceMessage(reservation)}</small>
        </div>
      </div>

      <ReservationLineItems lines={reservation.lines ?? []} />

      <div className="reservation-card__secondary">
        <span>Creada el {formatReservationCreatedAt(reservation.createdAt)}</span>
        <span>Ref. interna #{reservation.id}</span>
      </div>
    </article>
  );
}
