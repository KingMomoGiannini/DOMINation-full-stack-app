import type { ReservationScheduleParts } from '../../utils/reservationDisplay';

interface ReservationScheduleBlockProps {
  schedule: ReservationScheduleParts;
}

export function ReservationScheduleBlock({ schedule }: ReservationScheduleBlockProps) {
  return (
    <div className="reservation-schedule-block">
      <p className="reservation-schedule-block__day">{schedule.headline}</p>
      <p className="reservation-schedule-block__time">{schedule.timeRange}</p>
      {schedule.crossDayNote ? (
        <p className="reservation-schedule-block__note">{schedule.crossDayNote}</p>
      ) : null}
    </div>
  );
}
