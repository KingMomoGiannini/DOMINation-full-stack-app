import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { createProviderRequest, getMyProviderRequest } from '../../../api';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { getApiErrorMessage } from '../../../utils/apiError';

function statusLabel(status?: string): string {
  switch (status) {
    case 'PENDING':
      return 'Pendiente';
    case 'APPROVED':
      return 'Aprobada';
    case 'REJECTED':
      return 'Rechazada';
    default:
      return status ?? '—';
  }
}

export function ProviderRequestPage() {
  const queryClient = useQueryClient();

  const requestQuery = useQuery({
    queryKey: ['providerRequest', 'me'],
    queryFn: getMyProviderRequest,
  });

  const createMutation = useMutation({
    mutationFn: () => createProviderRequest(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['providerRequest', 'me'] });
    },
  });

  const request = requestQuery.data;
  const loadError =
    requestQuery.error &&
    getApiErrorMessage(requestQuery.error, 'No pudimos cargar el estado de tu solicitud.');
  const actionError =
    createMutation.error &&
    getApiErrorMessage(createMutation.error, 'No pudimos enviar la solicitud.');

  if (requestQuery.isPending) {
    return (
      <div className="main-content">
        <Spinner label="Cargando…" />
      </div>
    );
  }

  return (
    <div className="main-content">
      <PageHeader
        title="Solicitud de"
        highlight="prestador"
        subtitle="Pedí permisos para cargar sucursales y salas. Un administrador revisará tu pedido."
      />

      {loadError && <div className="alert alert-error">⚠️ {loadError}</div>}
      {actionError && <div className="alert alert-error">⚠️ {actionError}</div>}

      {request ? (
        <div className="card" style={{ maxWidth: '640px', margin: '0 auto', textAlign: 'left' }}>
          <h3 style={{ marginBottom: '0.75rem' }}>Estado de tu solicitud</h3>
          <p>
            <span className="badge badge-secondary">{statusLabel(request.status)}</span>
          </p>
          {request.createdAt && (
            <p style={{ marginTop: '0.75rem', color: 'var(--gray-light)' }}>
              Alta:{' '}
              {new Date(request.createdAt).toLocaleString('es-AR', {
                dateStyle: 'medium',
                timeStyle: 'short',
              })}
            </p>
          )}
          {request.status === 'PENDING' && (
            <p style={{ marginTop: '1rem', lineHeight: 1.6 }}>
              Tu solicitud está en revisión. No hace falta enviar otra: el backend solo permite una solicitud
              pendiente por usuario. Cuando sea aprobada, <strong>cerrá sesión y volvé a entrar</strong> para
              actualizar tu token con el rol de prestador.
            </p>
          )}
          {request.status === 'APPROVED' && (
            <p style={{ marginTop: '1rem', lineHeight: 1.6 }}>
              Tu solicitud fue aprobada. <strong>Cerrá sesión e iniciá sesión de nuevo</strong> para acceder al{' '}
              <Link to="/provider">panel de prestador</Link>.
            </p>
          )}
          {request.status === 'REJECTED' && (
            <p style={{ marginTop: '1rem', lineHeight: 1.6 }}>
              Esta solicitud fue rechazada. Si tenés dudas, contactá al equipo de la plataforma.
            </p>
          )}
        </div>
      ) : (
        <div className="form-container" style={{ maxWidth: '640px', margin: '0 auto' }}>
          <div className="form-header">
            <h2>Enviar solicitud</h2>
            <p>
              Al confirmar, se crea un registro para que un administrador te asigne el rol de prestador. Si ya
              tenés una solicitud pendiente o conflicto de estado, el servidor responderá con un mensaje claro.
            </p>
          </div>
          <button
            type="button"
            className="btn btn-success btn-block"
            disabled={createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            {createMutation.isPending ? 'Enviando…' : 'Enviar solicitud'}
          </button>
        </div>
      )}
    </div>
  );
}
