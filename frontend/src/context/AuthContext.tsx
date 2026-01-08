import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { getToken, getUser, logout as apiLogout, isAuthenticated as checkAuth } from '../api/apiClient';

interface AuthContextType {
  isAuthenticated: boolean;
  username: string | null;
  login: (token: string, username: string) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [isAuthenticated, setIsAuthenticated] = useState(checkAuth());
  const [username, setUsername] = useState<string | null>(getUser());

  useEffect(() => {
    setIsAuthenticated(checkAuth());
    setUsername(getUser());
  }, []);

  const login = (token: string, user: string) => {
    setIsAuthenticated(true);
    setUsername(user);
  };

  const logout = () => {
    apiLogout();
    setIsAuthenticated(false);
    setUsername(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, username, login, logout }}>
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


