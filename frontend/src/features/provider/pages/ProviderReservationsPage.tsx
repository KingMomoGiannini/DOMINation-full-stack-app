import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMyBranches, getProviderReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveReservationBranchDisplay } from '../../../utils/branchLookup';
import { formatReservationSchedule, getReservationStatusMeta } from '../../../utils/reservationDisplay';
import {
  getReservationTemporalHint,
  isReservationLiveNow,
  reservationMatchesFilters,
  sortReservations,
  type ReservationSortMode,
  type ReservationStatusFilter,
  type ReservationTimeFilter,
} from '../../../utils/reservationUi';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { ReservationStatusBadge } from '../../../components/reservations/ReservationStatusBadge';
import { ReservationScheduleBlock } from '../../../components/reservations/ReservationScheduleBlock';
import { ReservationLineItems } from '../../../components/reservations/ReservationLineItems';
import { ReservationFiltersBar } from '../../../components/reservations/ReservationFiltersBar';
import { ProviderAreaNav } from '../../../components/provider/ProviderAreaNav';

export function ProviderReservationsPage() {
  const listQuery = useQuery({
    queryKey: ['providerReservations'],
    queryFn: getProviderReservations,
  });

  const [branchFilter, setBranchFilter] = useState<number | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<ReservationStatusFilter>('ALL');
  const [timeFilter, setTimeFilter] = useState<ReservationTimeFilter>('ALL');
  const [sortMode, setSortMode] = useState<ReservationSortMode>('START_DESC');

  const needsBranchCatalog = useMemo(() => {
    const list = listQuery.data ?? [];
    return list.some((r) => !String(r.branchName ?? '').trim());
  }, [listQuery.data]);

  const branchesQuery = useQuery({
    queryKey: ['providerBranches'],
    queryFn: getMyBranches,
    enabled: listQuery.isSuccess && (listQuery.data?.length ?? 0) > 0,
  });

  const branchMap = useMemo(() => branchesToMap(branchesQuery.data ?? []), [branchesQuery.data]);

  const branchOptions = useMemo(
    () => (branchesQuery.data ?? []).map((b) => ({ id: b.id, name: b.name })),
    [branchesQuery.data]
  );

  const rows: Reservation[] = listQuery.data ?? [];

  const filteredSorted = useMemo(() => {
    const filtered = rows.filter((r) =>
      reservationMatchesFilters(r, {
        branchId: branchFilter,
        status: statusFilter,
        time: timeFilter,
      })
    );
    return sortReservations(filtered, sortMode);
  }, [rows, branchFilter, statusFilter, timeFilter, sortMode]);

  const renderBranch = (r: Reservation) => {
    const catalogReady = !needsBranchCatalog || branchesQuery.isFetched;
    const d = resolveReservationBranchDisplay(
      r.branchId,
      r.branchName,
      branchMap,
      catalogReady,
      'Cargando tus sucursales para mostrar el nombre…'
    );
    if (d.resolved) return d;
    if (catalogReady && needsBranchCatalog) {
      return {
        primary: d.primary,
        secondary: [
          d.secondary,
          'Si la sucursal ya no está en tu panel, puede ser datos históricos: el contrato solo garantiza branchId.',
        ]
          .filter(Boolean)
          .join(' '),
        resolved: false,
      };
    }
    return d;
  };

  return (
    <div className="main-content">
      <PageHeader
        title="Reservas en"
        highlight="tus sucursales"
        subtitle="Filtrá por sucursal, estado o momento. La cancelación la hace el cliente desde su cuenta."
      />

      <ProviderAreaNav />

      {listQuery.isError && (
        <QueryErrorPanel
          error={listQuery.error}
          fallback="No pudimos cargar las reservas de tus sucursales."
          title="Error al cargar reservas"
          onRetry={() => listQuery.refetch()}
        />
      )}

      {needsBranchCatalog && branchesQuery.isError && (
        <QueryErrorPanel
          error={branchesQuery.error}
          fallback="No pudimos cargar tus sucursales para mostrar nombres."
          title="Catálogo de sucursales (prestador)"
          onRetry={() => branchesQuery.refetch()}
        />
      )}

      {listQuery.isPending ? (
        <Spinner label="Cargando reservas…" />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No hay reservas en tus sucursales"
          description="Cuando un cliente reserve una franja en una sucursal tuya, el registro aparecerá acá."
        />
      ) : (
        <>
          <ReservationFiltersBar
            idPrefix="prov"
            showBranchFilter={branchOptions.length > 0}
            branchOptions={branchOptions}
            branchId={branchFilter}
            status={statusFilter}
            time={timeFilter}
            sort={sortMode}
            onBranchId={setBranchFilter}
            onStatus={setStatusFilter}
            onTime={setTimeFilter}
            onSort={setSortMode}
          />
          <p className="reservation-filters__meta">
            Mostrando <strong>{filteredSorted.length}</strong> de {rows.length} reservas
            {branchFilter !== 'ALL' || statusFilter !== 'ALL' || timeFilter !== 'ALL'
              ? ' (filtros activos)'
              : null}
            .
          </p>

          {filteredSorted.length === 0 ? (
            <EmptyState
              title="Nada coincide con los filtros"
              description="Probá ampliar sucursal, estado o momento, o restablecé «Todos» en cada lista."
            />
          ) : (
            <div className="reservation-list">
              {filteredSorted.map((r) => {
                const schedule = formatReservationSchedule(r.startAt, r.endAt);
                const statusMeta = getReservationStatusMeta(r.status);
                const branch = renderBranch(r);
                const temporal = getReservationTemporalHint(r.startAt, r.endAt);
                const live = isReservationLiveNow(r);
                return (
                  <article
                    key={r.id}
                    className={`reservation-card${live ? ' reservation-card--live' : ''}`}
                  >
                    <div className="reservation-card__top">
                      <div className="reservation-card__main-col">
                        <ReservationStatusBadge meta={statusMeta} />
                        <ReservationScheduleBlock schedule={schedule} />
                        {temporal ? (
                          <span className="reservation-card__temporal">{temporal}</span>
                        ) : null}
                        <div className="reservation-card__branch">
                          <strong>{branch.primary}</strong>
                          {branch.secondary ? <small>{branch.secondary}</small> : null}
                        </div>
                        <p className="reservation-card__ref">Referencia #{r.id}</p>
                        <div className="provider-customer-ref">
                          {r.customerUsername?.trim() ? (
                            <>
                              Cliente: <strong>@{r.customerUsername.trim()}</strong>
                              <br />
                              <span style={{ fontSize: '0.8rem' }}>
                                ID: <code style={{ color: '#fff' }}>{r.customerId}</code>
                              </span>
                            </>
                          ) : (
                            <>
                              Cliente ID: <code style={{ color: '#fff' }}>{r.customerId}</code>
                              <br />
                              <span style={{ fontSize: '0.8rem' }}>
                                Sin username en la respuesta (dato antiguo o fallo de enriquecimiento).
                              </span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>
                    <ReservationLineItems lines={r.lines ?? []} />
                  </article>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
