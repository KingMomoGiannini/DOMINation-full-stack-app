import type {
  ReservationAttendanceFilter,
  ReservationSortMode,
  ReservationStatusFilter,
  ReservationTimeFilter,
} from '../../utils/reservationUi';

export interface BranchOption {
  id: number;
  name: string;
}

interface ReservationFiltersBarProps {
  branchOptions: BranchOption[];
  branchId: number | 'ALL';
  status: ReservationStatusFilter;
  time: ReservationTimeFilter;
  attendance?: ReservationAttendanceFilter;
  sort: ReservationSortMode;
  onBranchId: (v: number | 'ALL') => void;
  onStatus: (v: ReservationStatusFilter) => void;
  onTime: (v: ReservationTimeFilter) => void;
  onAttendance?: (v: ReservationAttendanceFilter) => void;
  onSort: (v: ReservationSortMode) => void;
  showBranchFilter: boolean;
  idPrefix: string;
  showWindowFilter?: boolean;
  windowFrom?: string;
  windowTo?: string;
  onWindowFrom?: (v: string) => void;
  onWindowTo?: (v: string) => void;
}

export function ReservationFiltersBar({
  branchOptions,
  branchId,
  status,
  time,
  attendance = 'ALL',
  sort,
  onBranchId,
  onStatus,
  onTime,
  onAttendance,
  onSort,
  showBranchFilter,
  idPrefix,
  showWindowFilter = false,
  windowFrom = '',
  windowTo = '',
  onWindowFrom,
  onWindowTo,
}: ReservationFiltersBarProps) {
  return (
    <div className="reservation-filters" role="search" aria-label="Filtros de reservas">
      {showBranchFilter && (
        <div className="reservation-filters__field">
          <label htmlFor={`${idPrefix}-branch`}>Sucursal</label>
          <select
            id={`${idPrefix}-branch`}
            value={branchId === 'ALL' ? 'ALL' : String(branchId)}
            onChange={(e) => {
              const v = e.target.value;
              onBranchId(v === 'ALL' ? 'ALL' : Number(v));
            }}
          >
            <option value="ALL">Todas</option>
            {branchOptions.map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="reservation-filters__field">
        <label htmlFor={`${idPrefix}-status`}>Registro</label>
        <select
          id={`${idPrefix}-status`}
          value={status}
          onChange={(e) => onStatus(e.target.value as ReservationStatusFilter)}
        >
          <option value="ALL">Todos</option>
          <option value="PENDING">Pendiente</option>
          <option value="CONFIRMED">Confirmada</option>
          <option value="CANCELLED">Cancelada</option>
        </select>
      </div>
      <div className="reservation-filters__field">
        <label htmlFor={`${idPrefix}-time`}>Estado operativo</label>
        <select
          id={`${idPrefix}-time`}
          value={time}
          onChange={(e) => onTime(e.target.value as ReservationTimeFilter)}
        >
          <option value="ALL">Todas</option>
          <option value="UPCOMING">Próximas</option>
          <option value="IN_PROGRESS">En curso</option>
          <option value="COMPLETED">Finalizadas</option>
        </select>
      </div>
      {onAttendance ? (
        <div className="reservation-filters__field">
          <label htmlFor={`${idPrefix}-attendance`}>Operacion</label>
          <select
            id={`${idPrefix}-attendance`}
            value={attendance}
            onChange={(e) => onAttendance(e.target.value as ReservationAttendanceFilter)}
          >
            <option value="ALL">Todas</option>
            <option value="NOT_RECORDED">Sin registrar</option>
            <option value="CHECKED_IN">Con check-in</option>
            <option value="NO_SHOW">No-show</option>
            <option value="NOT_APPLICABLE">No aplica</option>
          </select>
        </div>
      ) : null}
      <div className="reservation-filters__field">
        <label htmlFor={`${idPrefix}-sort`}>Orden</label>
        <select
          id={`${idPrefix}-sort`}
          value={sort}
          onChange={(e) => onSort(e.target.value as ReservationSortMode)}
        >
          <option value="START_DESC">Fecha: más reciente primero</option>
          <option value="START_ASC">Fecha: más lejana primero</option>
        </select>
      </div>
      {showWindowFilter && onWindowFrom && onWindowTo ? (
        <>
          <div className="reservation-filters__field">
            <label htmlFor={`${idPrefix}-win-from`}>Ventana desde</label>
            <input
              id={`${idPrefix}-win-from`}
              type="datetime-local"
              className="admin-pr-input"
              value={windowFrom}
              onChange={(e) => onWindowFrom(e.target.value)}
            />
          </div>
          <div className="reservation-filters__field">
            <label htmlFor={`${idPrefix}-win-to`}>Ventana hasta</label>
            <input
              id={`${idPrefix}-win-to`}
              type="datetime-local"
              className="admin-pr-input"
              value={windowTo}
              onChange={(e) => onWindowTo(e.target.value)}
            />
          </div>
        </>
      ) : null}
    </div>
  );
}
