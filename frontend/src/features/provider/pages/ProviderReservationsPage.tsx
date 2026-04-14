import { useEffect, useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getMyBranches, getProviderReservationsPage } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveReservationBranchDisplay } from '../../../utils/branchLookup';
import {
  type ReservationSortMode,
  type ReservationStatusFilter,
  type ReservationTimeFilter,
} from '../../../utils/reservationUi';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { ReservationFiltersBar } from '../../../components/reservations/ReservationFiltersBar';
import { ReservationDetailCard } from '../../../components/reservations/ReservationDetailCard';
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

  const needsBranchCatalog = useMemo(() => rows.some((r) => !String(r.branchName ?? '').trim()), [rows]);

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
      'Cargando tus sucursales para mostrar el nombre...'
    );
    if (d.resolved) return d;
    if (catalogReady && needsBranchCatalog) {
      return {
        primary: d.primary,
        secondary: [
          d.secondary,
          'Si la sucursal ya no está en tu panel, puede tratarse de datos históricos: el contrato solo garantiza branchId.',
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
        subtitle="Paginación y filtros server-side con semántica operativa clara: próximas, en curso, finalizadas y canceladas."
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
        <Spinner label="Cargando reservas..." />
      ) : (pg?.totalElements ?? 0) === 0 ? (
        <EmptyState
          title="No hay reservas que coincidan"
          description={
            hasFilters
              ? 'Probá ampliar filtros, cambiar el estado operativo o volver a "Todas".'
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
            {listQuery.isFetching && !listInitialLoading ? ' · Actualizando...' : null}.
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
              description="No hay filas en esta página. Probá otra página o un tamaño distinto."
            >
              <button type="button" className="btn btn-primary" onClick={() => setPage(0)}>
                Primera página
              </button>
            </EmptyState>
          ) : (
            <div className="reservation-list">
              {rows.map((r) => {
                const branch = renderBranch(r);
                return (
                  <ReservationDetailCard
                    key={r.id}
                    reservation={r}
                    audience="provider"
                    branch={branch}
                  />
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
