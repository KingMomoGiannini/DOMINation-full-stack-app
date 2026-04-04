import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

interface RoleRouteProps {
  /** Al menos uno de estos roles (OR). Comparación vía hasRole (ROLE_X en JWT). */
  allow: string[];
  children: React.ReactNode;
}

export function RoleRoute({ allow, children }: RoleRouteProps) {
  const { isAuthenticated, hasRole } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  const allowed = allow.some((r) => hasRole(r));
  if (!allowed) {
    return <Navigate to="/access-denied" replace />;
  }

  return <>{children}</>;
}
