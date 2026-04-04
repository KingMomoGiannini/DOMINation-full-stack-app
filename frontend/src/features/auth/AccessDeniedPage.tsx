import { Link, useLocation } from 'react-router-dom';

export function AccessDeniedPage() {
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message;

  return (
    <div className="main-content">
      <div className="empty-state" style={{ maxWidth: '520px', margin: '0 auto' }}>
        <h2>Acceso no permitido</h2>
        <p style={{ marginTop: '1rem', lineHeight: 1.6 }}>
          {message ||
            'No tenés permisos para ver esta sección con tu cuenta actual. Si creés que es un error, contactá al administrador.'}
        </p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem', display: 'inline-block' }}>
          Volver al inicio
        </Link>
      </div>
    </div>
  );
}
