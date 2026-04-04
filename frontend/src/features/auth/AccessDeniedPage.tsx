import { Link, useLocation } from 'react-router-dom';

export function AccessDeniedPage() {
  const location = useLocation();
  const message = (location.state as { message?: string } | null)?.message;

  return (
    <div className="main-content">
      <div className="empty-state" style={{ maxWidth: '560px', margin: '0 auto' }}>
        <h2>Acceso no permitido</h2>
        <p style={{ marginTop: '1rem', lineHeight: 1.65 }}>
          {message ||
            'No tenés permisos para ver esta sección con tu cuenta actual. Suele deberse a que el JWT no incluye el rol necesario (403 en API) o a reglas de navegación por rol.'}
        </p>
        <p style={{ marginTop: '0.75rem', color: 'var(--gray-light)', fontSize: '0.95rem', lineHeight: 1.5 }}>
          Si acabás de recibir un rol nuevo (por ejemplo prestador), probá{' '}
          <strong>cerrar sesión y volver a entrar</strong> para renovar el token.
        </p>
        <Link to="/" className="btn btn-primary" style={{ marginTop: '1.5rem', display: 'inline-block' }}>
          Volver al inicio
        </Link>
      </div>
    </div>
  );
}
