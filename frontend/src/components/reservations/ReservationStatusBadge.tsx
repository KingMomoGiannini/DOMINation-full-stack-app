import type { ReservationStatusMeta } from '../../utils/reservationDisplay';

interface ReservationStatusBadgeProps {
  meta: ReservationStatusMeta;
}

export function ReservationStatusBadge({ meta }: ReservationStatusBadgeProps) {
  return (
    <span className={`reservation-status-badge reservation-status-badge--${meta.modifier}`} title={meta.hint}>
      {meta.label}
    </span>
  );
}
