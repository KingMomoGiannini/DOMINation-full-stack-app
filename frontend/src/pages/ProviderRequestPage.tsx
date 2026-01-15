import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import { createProviderRequest, getMyProviderRequest, ProviderRequestResponse } from '../api/apiClient';

export default function ProviderRequestPage() {
  const { isAuthenticated, hasRole } = useAuth();
  const navigate = useNavigate();
  const [request, setRequest] = useState<ProviderRequestResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    loadRequest();
  }, [isAuthenticated, navigate]);

  const loadRequest = async () => {
    try {
      setLoading(true);
      const data = await getMyProviderRequest();
      setRequest(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar solicitud');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRequest = async () => {
    try {
      setError(null);
      setSuccess(null);
      await createProviderRequest();
      setSuccess('Solicitud enviada exitosamente. Un administrador la revisará pronto.');
      loadRequest();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al crear solicitud');
    }
  };

  if (loading) {
    return <div className="main-content"><div className="loading">Cargando</div></div>;
  }

  if (hasRole('PROVIDER')) {
    return (
      <div className="main-content">
        <div className="empty-state">
          <h3>Ya eres prestador</h3>
          <p>Puedes gestionar tus sucursales desde "Mi Panel"</p>
        </div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <div className="section-header">
        <h1 className="page-title">
          Solicitud de <span className="highlight">Prestador</span>
        </h1>
        <p>Conviértete en prestador y ofrece tus espacios</p>
      </div>

      {error && (
        <div className="alert alert-error">
          ⚠️ {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          ✓ {success}
        </div>
      )}

      {request ? (
        <div className="card" style={{ maxWidth: '600px', margin: '0 auto' }}>
          <h3>Estado de tu solicitud</h3>
          <p style={{ marginTop: '1rem' }}>
            <span className={`badge ${
              request.status === 'PENDING' ? '' : 
              request.status === 'APPROVED' ? 'badge-secondary' : 
              'badge-secondary'
            }`}>
              {request.status === 'PENDING' && '⏳ PENDIENTE'}
              {request.status === 'APPROVED' && '✓ APROBADA'}
              {request.status === 'REJECTED' && '✗ RECHAZADA'}
            </span>
          </p>
          <p style={{ marginTop: '1rem', color: 'var(--gray-light)' }}>
            Fecha de solicitud: {new Date(request.createdAt || '').toLocaleDateString()}
          </p>
          {request.status === 'PENDING' && (
            <p style={{ marginTop: '1rem', color: 'white' }}>
              Tu solicitud está siendo revisada por un administrador. 
              Te notificaremos cuando sea aprobada.
            </p>
          )}
          {request.status === 'APPROVED' && (
            <p style={{ marginTop: '1rem', color: 'white' }}>
              ¡Tu solicitud fue aprobada! Cierra sesión e inicia nuevamente para acceder a tu panel de prestador.
            </p>
          )}
          {request.status === 'REJECTED' && (
            <p style={{ marginTop: '1rem', color: 'white' }}>
              Tu solicitud fue rechazada. Contacta con soporte para más información.
            </p>
          )}
        </div>
      ) : (
        <div className="form-container" style={{ maxWidth: '600px', margin: '0 auto' }}>
          <div className="form-header">
            <h2>Solicitar cuenta de prestador</h2>
            <p>Podrás ofrecer tus espacios para alquiler una vez aprobada tu solicitud</p>
          </div>
          <div style={{ textAlign: 'center', padding: '2rem 0' }}>
            <p style={{ color: 'white', marginBottom: '2rem', fontSize: '1.1rem' }}>
              Al enviar esta solicitud, un administrador revisará tu perfil y te otorgará 
              permisos de prestador para que puedas crear sucursales y salas.
            </p>
            <button
              className="btn btn-success btn-block"
              onClick={handleCreateRequest}
            >
              Enviar Solicitud
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

