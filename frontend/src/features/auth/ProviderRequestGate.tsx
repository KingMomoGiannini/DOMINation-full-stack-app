import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

/**
 * Solo usuarios autenticados con rol USER típico de "cliente": sin PROVIDER ni ADMIN.
 * El backend exige ROLE_USER en /auth/provider-requests.
 */
export function ProviderRequestGate({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, hasRole } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (hasRole('PROVIDER')) {
    return <Navigate to="/provider" replace />;
  }

  if (hasRole('ADMIN')) {
    return (
      <Navigate
        to="/access-denied"
        replace
        state={{ message: 'Los administradores no utilizan esta solicitud. Gestioná prestadores desde el panel admin.' }}
      />
    );
  }

  if (!hasRole('USER')) {
    return (
      <Navigate
        to="/access-denied"
        replace
        state={{
          message:
            'Esta solicitud está disponible solo para cuentas con rol de usuario. Si necesitás otro perfil, contactá soporte.',
        }}
      />
    );
  }

  return <>{children}</>;
}
