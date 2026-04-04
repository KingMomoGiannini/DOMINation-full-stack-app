import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import { createProviderRequest, getMyProviderRequest } from '../../../api';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { getApiErrorMessage } from '../../../utils/apiError';
import { useAuth } from '../../auth/AuthContext';

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
  const navigate = useNavigate();
  const { logout } = useAuth();

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

  const goRenewSession = () => {
    logout();
    navigate('/login?providerApproved=1');
  };

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
        <div className="card" style={{ maxWidth: '680px', margin: '0 auto', textAlign: 'left' }}>
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
            <>
              <p style={{ marginTop: '1rem', lineHeight: 1.65 }}>
                Tu solicitud está en revisión. No envíes otra mientras esté pendiente: el servidor rechaza
                duplicados.
              </p>
              <div className="alert alert-info alert--stack" style={{ marginTop: '1.25rem' }}>
                <strong>Cuando te aprueben</strong>
                <p style={{ marginTop: '0.35rem', fontWeight: 500 }}>
                  El rol se actualiza en la base de datos, pero <strong>tu sesión actual sigue con el JWT viejo</strong>{' '}
                  (sin <code>ROLE_PROVIDER</code>). Tenés que cerrar sesión y volver a entrar para ver «Panel
                  prestador» y «Reservas sucursales». No hay refresh token en esta versión de la app.
                </p>
              </div>
            </>
          )}

          {request.status === 'APPROVED' && (
            <>
              <div className="alert alert-warning alert--stack" style={{ marginTop: '1rem' }}>
                <strong>Importante: renová tu sesión</strong>
                <p style={{ marginTop: '0.35rem', fontWeight: 500 }}>
                  La solicitud figura aprobada, pero hasta que no inicies sesión de nuevo el navegador puede seguir
                  usando un token emitido <em>antes</em> del cambio de roles. Eso es normal con JWT stateless.
                </p>
              </div>
              <ol
                style={{
                  marginTop: '1rem',
                  paddingLeft: '1.25rem',
                  lineHeight: 1.7,
                  fontFamily: 'Arial, sans-serif',
                }}
              >
                <li>Pulsa el botón de abajo (o cerrá sesión desde la barra superior).</li>
                <li>Iniciá sesión con el mismo usuario.</li>
                <li>Comprobá en la barra los enlaces «Panel prestador» y «Reservas sucursales».</li>
              </ol>
              <div style={{ marginTop: '1.25rem', display: 'flex', flexWrap: 'wrap', gap: '0.75rem' }}>
                <button type="button" className="btn btn-success" onClick={goRenewSession}>
                  Cerrar sesión e ir al login
                </button>
                <Link to="/" className="btn btn-secondary" style={{ display: 'inline-block', textAlign: 'center' }}>
                  Volver al inicio
                </Link>
              </div>
            </>
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
              Se crea un registro para que un administrador te asigne el rol de prestador. Si ya tenés una pendiente
              o ya sos prestador, el servidor responde con conflicto (409) y un mensaje claro.
            </p>
          </div>
          <div className="alert alert-info alert--stack" style={{ marginBottom: '1.25rem' }}>
            <strong>Después de la aprobación</strong>
            <span style={{ marginTop: '0.35rem', fontWeight: 500 }}>
              Vas a necesitar <strong>cerrar sesión y volver a entrar</strong> para que el token incluya{' '}
              <code>ROLE_PROVIDER</code>. Podés hacerlo desde acá una vez aprobado o desde el menú superior.
            </span>
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
