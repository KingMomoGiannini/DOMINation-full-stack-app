import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMyBranches, getProviderReservationsPage } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveReservationBranchDisplay } from '../../../utils/branchLookup';
import { formatReservationSchedule, getReservationStatusMeta } from '../../../utils/reservationDisplay';
import {
  getReservationTemporalHint,
  isReservationLiveNow,
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

function toIsoParam(localDatetime: string): string | undefined {
  const t = localDatetime.trim();
  if (!t) return undefined;
  return t.length === 16 ? `${t}:00` : t;
}

export function ProviderReservationsPage() {
  const [branchFilter, setBranchFilter] = useState<number | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<ReservationStatusFilter>('ALL');
  const [timeFilter, setTimeFilter] = useState<ReservationTimeFilter>('ALL');
  const [sortMode, setSortMode] = useState<ReservationSortMode>('START_DESC');
  const [windowFrom, setWindowFrom] = useState('');
  const [windowTo, setWindowTo] = useState('');
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(15);

  const listParams = useMemo(
    () => ({
      page,
      size: pageSize,
      branchId: branchFilter === 'ALL' ? undefined : branchFilter,
      status: statusFilter === 'ALL' ? undefined : statusFilter,
      time: timeFilter,
      from: toIsoParam(windowFrom),
      to: toIsoParam(windowTo),
      sort: sortMode === 'START_ASC' ? 'startAt,asc' : 'startAt,desc',
    }),
    [page, pageSize, branchFilter, statusFilter, timeFilter, windowFrom, windowTo, sortMode]
  );

  useEffect(() => {
    setPage(0);
  }, [branchFilter, statusFilter, timeFilter, sortMode, windowFrom, windowTo, pageSize]);

  const listQuery = useQuery({
    queryKey: ['providerReservations', listParams],
    queryFn: () => getProviderReservationsPage(listParams),
    placeholderData: (p) => p,
  });

  const branchesQuery = useQuery({
    queryKey: ['providerBranches'],
    queryFn: getMyBranches,
  });

  const rows: Reservation[] = listQuery.data?.content ?? [];
  const pg = listQuery.data;

  const needsBranchCatalog = useMemo(() => {
    return rows.some((r) => !String(r.branchName ?? '').trim());
  }, [rows]);

  const branchMap = useMemo(() => branchesToMap(branchesQuery.data ?? []), [branchesQuery.data]);

  const branchOptions = useMemo(
    () => (branchesQuery.data ?? []).map((b) => ({ id: b.id, name: b.name })),
    [branchesQuery.data]
  );

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

  const listInitialLoading = listQuery.isPending && !listQuery.data;
  const hasFilters =
    branchFilter !== 'ALL' ||
    statusFilter !== 'ALL' ||
    timeFilter !== 'ALL' ||
    windowFrom.trim() !== '' ||
    windowTo.trim() !== '';

  return (
    <div className="main-content">
      <PageHeader
        title="Reservas en"
        highlight="tus sucursales"
        subtitle="Filtros y paginación en servidor. Ventana opcional: reservas que solapan con el rango indicado."
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

      {listInitialLoading ? (
        <Spinner label="Cargando reservas…" />
      ) : (pg?.totalElements ?? 0) === 0 ? (
        <EmptyState
          title="No hay reservas que coincidan"
          description={
            hasFilters
              ? 'Probá ampliar filtros, ventana temporal o volvé a «Todas».'
              : 'Cuando un cliente reserve una franja en una sucursal tuya, el registro aparecerá acá.'
          }
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
            showWindowFilter
            windowFrom={windowFrom}
            windowTo={windowTo}
            onWindowFrom={setWindowFrom}
            onWindowTo={setWindowTo}
          />
          <div className="reservation-filters" style={{ marginTop: '-0.5rem', marginBottom: '1rem' }}>
            <div className="reservation-filters__field">
              <label htmlFor="prov-page-size">Por página</label>
              <select
                id="prov-page-size"
                value={String(pageSize)}
                onChange={(e) => setPageSize(Number(e.target.value))}
                className="admin-pr-input"
                style={{ minWidth: '100px' }}
              >
                <option value="10">10</option>
                <option value="15">15</option>
                <option value="25">25</option>
                <option value="50">50</option>
              </select>
            </div>
          </div>
          <p className="reservation-filters__meta">
            Página <strong>{(pg?.number ?? 0) + 1}</strong> de <strong>{Math.max(pg?.totalPages ?? 1, 1)}</strong> ·{' '}
            <strong>{pg?.numberOfElements ?? rows.length}</strong> en esta página ·{' '}
            <strong>{pg?.totalElements ?? 0}</strong> totales con filtros actuales
            {listQuery.isFetching && !listInitialLoading ? ' · Actualizando…' : null}.
          </p>

          <nav className="pagination-bar" aria-label="Páginas de reservas">
            <button
              type="button"
              className="btn btn-secondary"
              disabled={pg?.first !== false}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Anterior
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={pg?.last !== false}
              onClick={() => setPage((p) => p + 1)}
            >
              Siguiente
            </button>
          </nav>

          {rows.length === 0 ? (
            <EmptyState
              title="Página vacía"
              description="No hay filas en esta página. Probá otra página o tamaño."
            >
              <button type="button" className="btn btn-primary" onClick={() => setPage(0)}>
                Primera página
              </button>
            </EmptyState>
          ) : (
            <div className="reservation-list">
              {rows.map((r) => {
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
