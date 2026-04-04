import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { cancelReservation, getMyReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { PageHeader } from '../../../components/ui/PageHeader';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { getApiErrorMessage } from '../../../utils/apiError';

function formatRange(startAt: string, endAt: string): string {
  try {
    const s = new Date(startAt);
    const e = new Date(endAt);
    const df = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });
    return `${df.format(s)} → ${df.format(e)}`;
  } catch {
    return `${startAt} → ${endAt}`;
  }
}

export function MyReservationsPage() {
  const queryClient = useQueryClient();
  const [confirmId, setConfirmId] = useState<number | null>(null);

  const listQuery = useQuery({
    queryKey: ['myReservations'],
    queryFn: getMyReservations,
  });

  const cancelMutation = useMutation({
    mutationFn: cancelReservation,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myReservations'] });
      setConfirmId(null);
    },
  });

  const reservations = listQuery.data ?? [];
  const errorMsg =
    listQuery.error &&
    getApiErrorMessage(listQuery.error, 'No pudimos cargar tus reservas.');
  const cancelError =
    cancelMutation.error &&
    getApiErrorMessage(cancelMutation.error, 'No pudimos cancelar la reserva.');

  const canCancel = (r: Reservation) => r.status !== 'CANCELLED';

  return (
    <div className="main-content">
      <PageHeader title="Mis" highlight="reservas" subtitle="Gestioná las reservas asociadas a tu cuenta." />

      {errorMsg && <div className="alert alert-error">⚠️ {errorMsg}</div>}
      {cancelError && <div className="alert alert-error">⚠️ {cancelError}</div>}

      {listQuery.isPending ? (
        <Spinner label="Cargando tus reservas…" />
      ) : reservations.length === 0 ? (
        <EmptyState
          title="Todavía no tenés reservas"
          description="Cuando hagas una reserva desde «Nueva reserva», la verás listada acá."
        />
      ) : (
        <div className="cards-grid" style={{ gridTemplateColumns: '1fr' }}>
          {reservations.map((r) => (
            <div key={r.id} className="card" style={{ textAlign: 'left' }}>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  gap: '1rem',
                  flexWrap: 'wrap',
                }}
              >
                <div>
                  <h3>Reserva #{r.id}</h3>
                  <p style={{ marginTop: '0.5rem' }}>{formatRange(r.startAt, r.endAt)}</p>
                  <p>
                    Sucursal: <strong>{r.branchId}</strong>
                  </p>
                  <p>
                    Estado: <span className="badge badge-secondary">{r.status}</span>
                  </p>
                </div>
                {canCancel(r) && (
                  <div>
                    <button
                      type="button"
                      className="btn btn-logout"
                      disabled={cancelMutation.isPending}
                      onClick={() => setConfirmId(r.id)}
                    >
                      Cancelar
                    </button>
                  </div>
                )}
              </div>
              {r.lines?.length > 0 && (
                <ul style={{ marginTop: '1rem', paddingLeft: '1.25rem', lineHeight: 1.6 }}>
                  {r.lines.map((line) => (
                    <li key={line.id}>
                      Ítem #{line.itemId} — cant. {line.quantity} — $
                      {line.price?.toLocaleString('es-AR')}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
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
