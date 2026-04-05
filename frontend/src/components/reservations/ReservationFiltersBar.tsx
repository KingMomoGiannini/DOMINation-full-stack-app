import type { ReservationSortMode, ReservationStatusFilter, ReservationTimeFilter } from '../../utils/reservationUi';

export interface BranchOption {
  id: number;
  name: string;
}

interface ReservationFiltersBarProps {
  branchOptions: BranchOption[];
  branchId: number | 'ALL';
  status: ReservationStatusFilter;
  time: ReservationTimeFilter;
  sort: ReservationSortMode;
  onBranchId: (v: number | 'ALL') => void;
  onStatus: (v: ReservationStatusFilter) => void;
  onTime: (v: ReservationTimeFilter) => void;
  onSort: (v: ReservationSortMode) => void;
  showBranchFilter: boolean;
  idPrefix: string;
}

export function ReservationFiltersBar({
  branchOptions,
  branchId,
  status,
  time,
  sort,
  onBranchId,
  onStatus,
  onTime,
  onSort,
  showBranchFilter,
  idPrefix,
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
        <label htmlFor={`${idPrefix}-status`}>Estado</label>
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
        <label htmlFor={`${idPrefix}-time`}>Momento</label>
        <select
          id={`${idPrefix}-time`}
          value={time}
          onChange={(e) => onTime(e.target.value as ReservationTimeFilter)}
        >
          <option value="ALL">Todas</option>
          <option value="UPCOMING">Próximas / vigentes</option>
          <option value="PAST">Pasadas</option>
        </select>
      </div>
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
    </div>
  );
}
