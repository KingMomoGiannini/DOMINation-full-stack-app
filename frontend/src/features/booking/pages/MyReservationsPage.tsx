import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cancelReservation, getBranches, getMyReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveReservationBranchDisplay } from '../../../utils/branchLookup';
import {
  formatReservationSchedule,
  getReservationOperationalMeta,
  getReservationRecordMeta,
} from '../../../utils/reservationDisplay';
import {
  getCancellationMessage,
  getReservationTemporalHint,
  isReservationLiveNow,
  reservationMatchesFilters,
  sortReservations,
  type ReservationSortMode,
  type ReservationStatusFilter,
  type ReservationTimeFilter,
} from '../../../utils/reservationUi';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { PageHeader } from '../../../components/ui/PageHeader';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { ReservationStatusBadge } from '../../../components/reservations/ReservationStatusBadge';
import { ReservationScheduleBlock } from '../../../components/reservations/ReservationScheduleBlock';
import { ReservationLineItems } from '../../../components/reservations/ReservationLineItems';
import { ReservationFiltersBar } from '../../../components/reservations/ReservationFiltersBar';
import { getApiErrorMessage } from '../../../utils/apiError';

export function MyReservationsPage() {
  const queryClient = useQueryClient();
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [cancelSuccess, setCancelSuccess] = useState(false);
  const [branchFilter, setBranchFilter] = useState<number | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<ReservationStatusFilter>('ALL');
  const [timeFilter, setTimeFilter] = useState<ReservationTimeFilter>('ALL');
  const [sortMode, setSortMode] = useState<ReservationSortMode>('START_DESC');

  const listQuery = useQuery({
    queryKey: ['myReservations'],
    queryFn: getMyReservations,
  });

  const needsBranchCatalog = useMemo(() => {
    const list = listQuery.data ?? [];
    return list.some((r) => !String(r.branchName ?? '').trim());
  }, [listQuery.data]);

  const branchesQuery = useQuery({
    queryKey: ['branches'],
    queryFn: getBranches,
    enabled: listQuery.isSuccess && (listQuery.data?.length ?? 0) > 0,
  });

  const branchMap = useMemo(() => branchesToMap(branchesQuery.data ?? []), [branchesQuery.data]);

  const branchOptions = useMemo(
    () => (branchesQuery.data ?? []).map((b) => ({ id: b.id, name: b.name })),
    [branchesQuery.data]
  );

  const cancelMutation = useMutation({
    mutationFn: cancelReservation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myReservations'] });
      setConfirmId(null);
      setCancelSuccess(true);
    },
  });

  const reservations = listQuery.data ?? [];

  const filteredSorted = useMemo(() => {
    const filtered = reservations.filter((r) =>
      reservationMatchesFilters(r, {
        branchId: branchFilter,
        status: statusFilter,
        time: timeFilter,
      })
    );
    return sortReservations(filtered, sortMode);
  }, [reservations, branchFilter, statusFilter, timeFilter, sortMode]);

  const cancelError =
    cancelMutation.error &&
    getApiErrorMessage(cancelMutation.error, 'No pudimos cancelar la reserva.');

  const renderBranch = (r: Reservation) =>
    resolveReservationBranchDisplay(
      r.branchId,
      r.branchName,
      branchMap,
      !needsBranchCatalog || branchesQuery.isFetched,
      'Sincronizando nombre con el catálogo público…'
    );

  const confirmReservation = confirmId != null ? reservations.find((r) => r.id === confirmId) : null;

  return (
    <div className="main-content">
      <PageHeader
        title="Mis"
        highlight="reservas"
        subtitle="Ciclo operativo claro: próximas, en curso, finalizadas o canceladas. Fechas en tu zona horaria local."
      />

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginBottom: '1rem' }}>
        <Link to="/reservations/new" className="btn btn-success">
          Nueva reserva
        </Link>
      </div>

      {cancelSuccess && (
        <div className="alert alert-success alert--stack" role="status">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', width: '100%' }}>
            <strong>Reserva cancelada</strong>
            <span>El estado quedó actualizado en el servidor. Podés reservar otra franja cuando quieras.</span>
            <button
              type="button"
              className="btn btn-secondary"
              style={{ alignSelf: 'flex-start', marginTop: '0.25rem' }}
              onClick={() => setCancelSuccess(false)}
            >
              Entendido
            </button>
          </div>
        </div>
      )}

      {listQuery.isError && (
        <QueryErrorPanel
          error={listQuery.error}
          fallback="No pudimos cargar tus reservas."
          title="Error al cargar reservas"
          onRetry={() => listQuery.refetch()}
        />
      )}

      {needsBranchCatalog && branchesQuery.isError && (
        <QueryErrorPanel
          error={branchesQuery.error}
          fallback="No pudimos cargar el catálogo de sucursales."
          title="No pudimos enriquecer nombres de sucursal"
          onRetry={() => branchesQuery.refetch()}
        />
      )}

      {cancelError && <div className="alert alert-error">⚠️ {cancelError}</div>}

      {listQuery.isPending ? (
        <Spinner label="Cargando tus reservas…" />
      ) : reservations.length === 0 ? (
        <EmptyState
          title="Todavía no tenés reservas"
          description="Cuando confirmes una franja desde «Nueva reserva», aparecerá acá con fecha clara y estado."
        >
          <Link to="/reservations/new" className="btn btn-success">
            Ir a nueva reserva
          </Link>
        </EmptyState>
      ) : (
        <>
          <ReservationFiltersBar
            idPrefix="my"
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
            Mostrando <strong>{filteredSorted.length}</strong> de {reservations.length} reservas.
          </p>

          {filteredSorted.length === 0 ? (
            <EmptyState
              title="Nada coincide con los filtros"
              description="Ajustá sucursal, estado o momento, o elegí «Todos» en cada lista."
            />
          ) : (
            <div className="reservation-list">
              {filteredSorted.map((r) => {
                const schedule = formatReservationSchedule(r.startAt, r.endAt);
                const operationalMeta = getReservationOperationalMeta(r.operationalStatus);
                const recordMeta = getReservationRecordMeta(r.status);
                const branch = renderBranch(r);
                const temporal = getReservationTemporalHint(r);
                const live = isReservationLiveNow(r);
                return (
                  <article
                    key={r.id}
                      className={`reservation-card${live ? ' reservation-card--live' : ''}`}
                  >
                    <div className="reservation-card__top">
                      <div className="reservation-card__main-col">
                        <div className="reservation-card__badges">
                          <ReservationStatusBadge meta={operationalMeta} />
                          <ReservationStatusBadge meta={recordMeta} />
                        </div>
                        <ReservationScheduleBlock schedule={schedule} />
                        {temporal ? (
                          <span className="reservation-card__temporal">{temporal}</span>
                        ) : null}
                        <div className="reservation-card__branch">
                          <strong>{branch.primary}</strong>
                          {branch.secondary ? <small>{branch.secondary}</small> : null}
                        </div>
                        <p className="reservation-card__ref">Referencia #{r.id}</p>
                        <p className="reservation-card__policy">{getCancellationMessage(r)}</p>
                      </div>
                      <div
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: 'flex-end',
                          gap: '0.5rem',
                        }}
                      >
                        {r.cancellable && (
                          <button
                            type="button"
                            className="btn btn-logout"
                            disabled={cancelMutation.isPending}
                            onClick={() => setConfirmId(r.id)}
                          >
                            Cancelar reserva
                          </button>
                        )}
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

      <ConfirmDialog
        open={confirmId !== null}
        title="¿Cancelar esta reserva?"
        description={
          confirmReservation
            ? `Se marcará como cancelada la reserva #${confirmReservation.id} (${formatReservationSchedule(confirmReservation.startAt, confirmReservation.endAt).headline}). Esta acción solo está disponible antes del inicio.`
            : 'Esta acción marca la reserva como cancelada.'
        }
        confirmLabel={cancelMutation.isPending ? 'Cancelando…' : 'Sí, cancelar'}
        cancelLabel="Volver"
        danger
        loading={cancelMutation.isPending}
        onCancel={() => setConfirmId(null)}
        onConfirm={() => {
          if (confirmId != null) cancelMutation.mutate(confirmId);
        }}
      />
    </div>
  );
}
