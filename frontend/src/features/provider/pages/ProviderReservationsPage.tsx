import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getMyBranches, getProviderReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { branchesToMap, resolveReservationBranchDisplay } from '../../../utils/branchLookup';
import { formatReservationSchedule, getReservationStatusMeta } from '../../../utils/reservationDisplay';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { ReservationStatusBadge } from '../../../components/reservations/ReservationStatusBadge';
import { ReservationScheduleBlock } from '../../../components/reservations/ReservationScheduleBlock';
import { ReservationLineItems } from '../../../components/reservations/ReservationLineItems';

export function ProviderReservationsPage() {
  const listQuery = useQuery({
    queryKey: ['providerReservations'],
    queryFn: getProviderReservations,
  });

  const needsBranchCatalog = useMemo(() => {
    const list = listQuery.data ?? [];
    return list.some((r) => !String(r.branchName ?? '').trim());
  }, [listQuery.data]);

  const branchesQuery = useQuery({
    queryKey: ['providerBranches'],
    queryFn: getMyBranches,
    enabled: (listQuery.data?.length ?? 0) > 0 && needsBranchCatalog,
  });

  const branchMap = useMemo(() => branchesToMap(branchesQuery.data ?? []), [branchesQuery.data]);

  const rows: Reservation[] = listQuery.data ?? [];
  const sorted = useMemo(() => {
    const copy = [...rows];
    copy.sort((a, b) => new Date(b.startAt).getTime() - new Date(a.startAt).getTime());
    return copy;
  }, [rows]);

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
        subtitle="Vista de solo lectura para el prestador. La cancelación la realiza el cliente desde su cuenta."
      />

      <p style={{ marginBottom: '1rem' }}>
        <Link to="/provider">← Volver al panel de prestador</Link>
      </p>

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
      ) : sorted.length === 0 ? (
        <EmptyState
          title="No hay reservas en tus sucursales"
          description="Cuando un cliente reserve una franja en una sucursal tuya, el registro aparecerá acá ordenado por fecha."
        />
      ) : (
        <div className="reservation-list">
          {sorted.map((r) => {
            const schedule = formatReservationSchedule(r.startAt, r.endAt);
            const statusMeta = getReservationStatusMeta(r.status);
            const branch = renderBranch(r);
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
                    <p className="provider-customer-ref">
                      {r.customerUsername?.trim() ? (
                        <>
                          Cliente: <strong>@{r.customerUsername.trim()}</strong>
                          <br />
                          <span style={{ fontSize: '0.8rem' }}>
                            ID de cuenta: <code style={{ color: '#fff' }}>{r.customerId}</code>
                          </span>
                        </>
                      ) : (
                        <>
                          Cliente (ID de cuenta): <code style={{ color: '#fff' }}>{r.customerId}</code>
                          <br />
                          <span style={{ fontSize: '0.8rem' }}>
                            Sin nombre de usuario en la respuesta (reserva antigua o no se pudo enriquecer).
                          </span>
                        </>
                      )}
                    </p>
                  </div>
                  <ReservationStatusBadge meta={statusMeta} />
                </div>
                <ReservationLineItems lines={r.lines ?? []} />
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
