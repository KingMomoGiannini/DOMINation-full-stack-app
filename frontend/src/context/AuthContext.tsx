import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { getToken, getUser, getUserRoles, getUserId, logout as apiLogout, isAuthenticated as checkAuth, hasRole as checkRole } from '../api/apiClient';

interface AuthContextType {
  isAuthenticated: boolean;
  username: string | null;
  roles: string[];
  userId: number | null;
  hasRole: (role: string) => boolean;
  login: (token: string, username: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(checkAuth());
  const [username, setUsername] = useState<string | null>(getUser());
  const [roles, setRoles] = useState<string[]>(getUserRoles());
  const [userId, setUserId] = useState<number | null>(getUserId());

  useEffect(() => {
    setIsAuthenticated(checkAuth());
    setUsername(getUser());
    setRoles(getUserRoles());
    setUserId(getUserId());
  }, []);

  const login = (token: string, user: string) => {
    setIsAuthenticated(true);
    setUsername(user);
    setRoles(getUserRoles());
    setUserId(getUserId());
  };

  const logout = () => {
    apiLogout();
    setIsAuthenticated(false);
    setUsername(null);
    setRoles([]);
    setUserId(null);
  };

  return (
    <AuthContext.Provider value={{ 
      isAuthenticated, 
      username, 
      roles,
      userId,
      hasRole: checkRole,
      login, 
      logout 
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};


