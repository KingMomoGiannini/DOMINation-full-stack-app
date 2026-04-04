import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  approveProviderRequest,
  getAdminProviderRequests,
  rejectProviderRequest,
  type AdminProviderRequestStatus,
  type ProviderRequestResponse,
} from '../../../api';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { getApiErrorMessage } from '../../../utils/apiError';

type FilterTab = 'PENDING' | 'APPROVED' | 'REJECTED' | 'ALL';

export function AdminProviderRequestsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<FilterTab>('PENDING');
  const [confirmApproveId, setConfirmApproveId] = useState<number | null>(null);
  const [confirmRejectId, setConfirmRejectId] = useState<number | null>(null);

  const apiStatus: AdminProviderRequestStatus | undefined =
    filter === 'ALL' ? undefined : (filter as AdminProviderRequestStatus);

  const listQuery = useQuery({
    queryKey: ['adminProviderRequests', apiStatus ?? 'ALL'],
    queryFn: () => getAdminProviderRequests(apiStatus),
  });

  const approveMut = useMutation({
    mutationFn: approveProviderRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequests'] });
      setConfirmApproveId(null);
    },
  });

  const rejectMut = useMutation({
    mutationFn: rejectProviderRequest,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequests'] });
      setConfirmRejectId(null);
    },
  });

  const rows: ProviderRequestResponse[] = listQuery.data ?? [];

  const pageError = useMemo(() => {
    const err = listQuery.error || approveMut.error || rejectMut.error;
    return err ? getApiErrorMessage(err, 'No pudimos completar la operación.') : null;
  }, [listQuery.error, approveMut.error, rejectMut.error]);

  const statusLabel = (s?: string) => {
    if (s === 'PENDING') return 'Pendiente';
    if (s === 'APPROVED') return 'Aprobada';
    if (s === 'REJECTED') return 'Rechazada';
    return s ?? '—';
  };

  return (
    <div className="main-content">
      <PageHeader
        title="Solicitudes de"
        highlight="prestador"
        subtitle="Aprobá o rechazá pedidos de usuarios para obtener rol PROVIDER."
      />

      {pageError && <div className="alert alert-error">⚠️ {pageError}</div>}

      <div className="filters-container" style={{ marginBottom: '1.5rem' }}>
        {(['PENDING', 'APPROVED', 'REJECTED', 'ALL'] as const).map((key) => (
          <button
            key={key}
            type="button"
            className={`filter-btn ${filter === key ? 'active' : ''}`}
            onClick={() => setFilter(key)}
          >
            {key === 'ALL'
              ? 'Todas'
              : key === 'PENDING'
                ? 'Pendientes'
                : key === 'APPROVED'
                  ? 'Aprobadas'
                  : 'Rechazadas'}
          </button>
        ))}
      </div>

      {listQuery.isPending ? (
        <Spinner label="Cargando solicitudes…" />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No hay solicitudes"
          description="No hay registros para el filtro elegido."
        />
      ) : (
        <div className="cards-grid">
          {rows.map((req) => (
            <div key={req.id} className="card" style={{ textAlign: 'left' }}>
              <h3>Solicitud #{req.id}</h3>
              <p>
                <strong>Usuario ID:</strong> {req.userId ?? '—'}
              </p>
              <p>
                <strong>Alta:</strong>{' '}
                {req.createdAt
                  ? new Date(req.createdAt).toLocaleString('es-AR', {
                      dateStyle: 'medium',
                      timeStyle: 'short',
                    })
                  : '—'}
              </p>
              <p style={{ marginTop: '0.75rem' }}>
                <span className="badge badge-secondary">{statusLabel(req.status)}</span>
              </p>
              {req.status === 'PENDING' && (
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.25rem', flexWrap: 'wrap' }}>
                  <button type="button" className="btn btn-success" onClick={() => setConfirmApproveId(req.id!)}>
                    Aprobar
                  </button>
                  <button type="button" className="btn btn-secondary" onClick={() => setConfirmRejectId(req.id!)}>
                    Rechazar
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={confirmApproveId != null}
        title="¿Aprobar solicitud?"
        description="El usuario recibirá el rol de prestador. Deberá volver a iniciar sesión para refrescar el token."
        confirmLabel="Aprobar"
        loading={approveMut.isPending}
        onCancel={() => setConfirmApproveId(null)}
        onConfirm={() => {
          if (confirmApproveId != null) approveMut.mutate(confirmApproveId);
        }}
      />

      <ConfirmDialog
        open={confirmRejectId != null}
        title="¿Rechazar solicitud?"
        description="El pedido quedará marcado como rechazado. Podés comunicar el motivo por fuera de la app."
        confirmLabel="Rechazar"
        danger
        loading={rejectMut.isPending}
        onCancel={() => setConfirmRejectId(null)}
        onConfirm={() => {
          if (confirmRejectId != null) rejectMut.mutate(confirmRejectId);
        }}
      />
    </div>
  );
}
