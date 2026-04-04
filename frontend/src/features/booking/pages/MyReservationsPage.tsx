import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { cancelReservation, getBranches, getMyReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveBranchDisplay } from '../../../utils/branchLookup';
import { formatReservationSchedule, getReservationStatusMeta } from '../../../utils/reservationDisplay';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { PageHeader } from '../../../components/ui/PageHeader';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { ReservationStatusBadge } from '../../../components/reservations/ReservationStatusBadge';
import { ReservationScheduleBlock } from '../../../components/reservations/ReservationScheduleBlock';
import { ReservationLineItems } from '../../../components/reservations/ReservationLineItems';
import { getApiErrorMessage } from '../../../utils/apiError';

export function MyReservationsPage() {
  const queryClient = useQueryClient();
  const [confirmId, setConfirmId] = useState<number | null>(null);
  const [cancelSuccess, setCancelSuccess] = useState(false);

  const listQuery = useQuery({
    queryKey: ['myReservations'],
    queryFn: getMyReservations,
  });

  const branchesQuery = useQuery({
    queryKey: ['branches'],
    queryFn: getBranches,
  });

  const branchMap = useMemo(() => branchesToMap(branchesQuery.data ?? []), [branchesQuery.data]);

  const cancelMutation = useMutation({
    mutationFn: cancelReservation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myReservations'] });
      setConfirmId(null);
      setCancelSuccess(true);
    },
  });

  const reservations = listQuery.data ?? [];
  const sorted = useMemo(() => {
    const copy = [...reservations];
    copy.sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());
    return copy;
  }, [reservations]);

  const cancelError =
    cancelMutation.error &&
    getApiErrorMessage(cancelMutation.error, 'No pudimos cancelar la reserva.');

  const canCancel = (r: Reservation) => r.status !== 'CANCELLED';

  const renderBranch = (branchId: number) => {
    if (!branchesQuery.isFetched) {
      return {
        primary: 'Sucursal',
        secondary: 'Sincronizando nombre con el catálogo público…',
        resolved: false,
      };
    }
    return resolveBranchDisplay(branchMap, branchId);
  };

  return (
    <div className="main-content">
      <PageHeader
        title="Mis"
        highlight="reservas"
        subtitle="Franjas confirmadas y pendientes. Las fechas se muestran en tu zona horaria local."
      />

      {cancelSuccess && (
        <div className="alert alert-success alert--stack" role="status">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem', width: '100%' }}>
            <strong>Reserva cancelada</strong>
            <span>El estado se actualizó. Podés crear una nueva desde «Nueva reserva».</span>
            <button type="button" className="btn btn-secondary" style={{ alignSelf: 'flex-start', marginTop: '0.25rem' }} onClick={() => setCancelSuccess(false)}>
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

      {branchesQuery.isError && (
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
      ) : sorted.length === 0 ? (
        <EmptyState
          title="Todavía no tenés reservas"
          description="Cuando confirmes una franja desde «Nueva reserva», aparecerá acá con fecha clara y estado."
        >
          <Link to="/reservations/new" className="btn btn-success">
            Ir a nueva reserva
          </Link>
        </EmptyState>
      ) : (
        <div className="reservation-list">
          {sorted.map((r) => {
            const schedule = formatReservationSchedule(r.startAt, r.endAt);
            const statusMeta = getReservationStatusMeta(r.status);
            const branch = renderBranch(r.branchId);
            return (
              <article key={r.id} className="reservation-card">
                <div className="reservation-card__top">
                  <div>
                    <ReservationScheduleBlock schedule={schedule} />
                    <div className="reservation-card__branch">
                      <strong>{branch.primary}</strong>
                      {branch.secondary ? <small>{branch.secondary}</small> : null}
                    </div>
                    <p className="reservation-card__ref">Referencia interna · #{r.id}</p>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '0.5rem' }}>
                    <ReservationStatusBadge meta={statusMeta} />
                    {canCancel(r) && (
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

      <ConfirmDialog
        open={confirmId !== null}
        title="¿Cancelar esta reserva?"
        description="Esta acción marca la reserva como cancelada. Podés crear una nueva reserva después si lo necesitás."
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
