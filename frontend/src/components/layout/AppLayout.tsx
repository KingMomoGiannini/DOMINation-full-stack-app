import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../features/auth/AuthContext';

export function AppLayout() {
  const { isAuthenticated, username, hasRole, logout } = useAuth();

  const showUserBooking = hasRole('USER');
  const showProvider = hasRole('PROVIDER');
  const showAdmin = hasRole('ADMIN');
  const showProviderRequest =
    isAuthenticated && !hasRole('PROVIDER') && !hasRole('ADMIN') && hasRole('USER');

  return (
    <div className="app">
      <nav className="navbar">
        <div className="navbar-container">
          <h1>
            <Link to="/" className="navbar-brand-link">
              DOMI<span className="highlight">Nation</span>
            </Link>
          </h1>
          <div className="navbar-links">
            <Link to="/">Inicio</Link>
            {isAuthenticated ? (
              <>
                {showUserBooking && (
                  <>
                    <Link to="/reservations">Mis reservas</Link>
                    <Link to="/reservations/new">Nueva reserva</Link>
                  </>
                )}
                {showProvider && (
                  <>
                    <Link to="/provider">Panel prestador</Link>
                    <Link to="/provider/reservations">Reservas sucursales</Link>
                  </>
                )}
                {showAdmin && (
                  <NavLink
                    to="/admin/provider-requests"
                    className={({ isActive }) => (isActive ? 'navbar-link-active' : undefined)}
                  >
                    Moderación prestadores
                  </NavLink>
                )}
                {showProviderRequest && <Link to="/provider-request">Ser prestador</Link>}
                <div className="user-info">
                  <span>👤 {username}</span>
                  <button type="button" onClick={logout} className="btn-logout">
                    Cerrar sesión
                  </button>
                </div>
              </>
            ) : (
              <>
                <Link to="/login">Iniciar sesión</Link>
                <Link to="/register">Registrarse</Link>
              </>
            )}
          </div>
        </div>
      </nav>
      <Outlet />
    </div>
  );
}
