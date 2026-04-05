import { NavLink } from 'react-router-dom';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  `provider-area-nav__link${isActive ? ' provider-area-nav__link--active' : ''}`;

/**
 * Navegación consistente entre pantallas del área prestador (sin nuevas rutas).
 */
export function ProviderAreaNav() {
  return (
    <nav className="provider-area-nav" aria-label="Área prestador">
      <NavLink to="/provider" end className={linkClass}>
        Sucursales y salas
      </NavLink>
      <NavLink to="/provider/reservations" className={linkClass}>
        Reservas
      </NavLink>
    </nav>
  );
}
