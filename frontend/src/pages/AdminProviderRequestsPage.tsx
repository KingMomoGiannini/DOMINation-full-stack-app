import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
  getAdminProviderRequests,
  approveProviderRequest,
  rejectProviderRequest,
  ProviderRequestResponse,
} from '../api/apiClient';

export default function AdminProviderRequestsPage() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();
  const [requests, setRequests] = useState<ProviderRequestResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('PENDING');

  useEffect(() => {
    if (!hasRole('ADMIN')) {
      navigate('/');
      return;
    }
    loadRequests();
  }, [hasRole, navigate, statusFilter]);

  const loadRequests = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await getAdminProviderRequests(statusFilter);
      setRequests(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar solicitudes');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (id: number) => {
    if (!confirm('¿Aprobar esta solicitud?')) return;
    try {
      setError(null);
      setSuccess(null);
      await approveProviderRequest(id);
      setSuccess('Solicitud aprobada exitosamente');
      loadRequests();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al aprobar solicitud');
    }
  };

  const handleReject = async (id: number) => {
    if (!confirm('¿Rechazar esta solicitud?')) return;
    try {
      setError(null);
      setSuccess(null);
      await rejectProviderRequest(id);
      setSuccess('Solicitud rechazada');
      loadRequests();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al rechazar solicitud');
    }
  };

  if (loading) {
    return <div className="main-content"><div className="loading">Cargando</div></div>;
  }

  return (
    <div className="main-content">
      <div className="section-header">
        <h1 className="page-title">
          Solicitudes de <span className="highlight">Prestador</span>
        </h1>
        <p>Gestión de solicitudes de usuarios para convertirse en prestadores</p>
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

      <div className="filters-container">
        <button
          className={`filter-btn ${statusFilter === 'PENDING' ? 'active' : ''}`}
          onClick={() => setStatusFilter('PENDING')}
        >
          Pendientes
        </button>
        <button
          className={`filter-btn ${statusFilter === 'APPROVED' ? 'active' : ''}`}
          onClick={() => setStatusFilter('APPROVED')}
        >
          Aprobadas
        </button>
        <button
          className={`filter-btn ${statusFilter === 'REJECTED' ? 'active' : ''}`}
          onClick={() => setStatusFilter('REJECTED')}
        >
          Rechazadas
        </button>
        <button
          className={`filter-btn ${statusFilter === '' ? 'active' : ''}`}
          onClick={() => setStatusFilter('')}
        >
          Todas
        </button>
      </div>

      {requests.length === 0 ? (
        <div className="empty-state">
          <h3>No hay solicitudes</h3>
          <p>No se encontraron solicitudes con el filtro seleccionado</p>
        </div>
      ) : (
        <div className="cards-grid">
          {requests.map((request) => (
            <div key={request.id} className="card">
              <h3>Solicitud #{request.id}</h3>
              <p>
                <strong>Usuario ID:</strong> {request.userId}
              </p>
              <p>
                <strong>Fecha:</strong>{' '}
                {new Date(request.createdAt || '').toLocaleDateString('es-ES', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </p>
              <p style={{ marginTop: '1rem' }}>
                <span
                  className={`badge ${
                    request.status === 'PENDING'
                      ? ''
                      : request.status === 'APPROVED'
                      ? 'badge-secondary'
                      : 'badge-secondary'
                  }`}
                >
                  {request.status === 'PENDING' && '⏳ PENDIENTE'}
                  {request.status === 'APPROVED' && '✓ APROBADA'}
                  {request.status === 'REJECTED' && '✗ RECHAZADA'}
                </span>
              </p>
              {request.status === 'PENDING' && (
                <div style={{ display: 'flex', gap: '0.5rem', marginTop: '1.5rem' }}>
                  <button
                    className="btn btn-success"
                    style={{ flex: 1 }}
                    onClick={() => handleApprove(request.id!)}
                  >
                    Aprobar
                  </button>
                  <button
                    className="btn btn-secondary"
                    style={{ flex: 1 }}
                    onClick={() => handleReject(request.id!)}
                  >
                    Rechazar
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

