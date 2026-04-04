const TOKEN_KEY = 'access_token';
const USER_KEY = 'current_user';
const USER_ROLES_KEY = 'user_roles';
const USER_ID_KEY = 'user_id';

export const getToken = (): string | null => localStorage.getItem(TOKEN_KEY);

export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(USER_ROLES_KEY);
  localStorage.removeItem(USER_ID_KEY);
};

export const setUser = (username: string): void => {
  localStorage.setItem(USER_KEY, username);
};

export const getUser = (): string | null => localStorage.getItem(USER_KEY);

export const setUserRoles = (roles: string[]): void => {
  localStorage.setItem(USER_ROLES_KEY, JSON.stringify(roles));
};

export const getUserRoles = (): string[] => {
  const roles = localStorage.getItem(USER_ROLES_KEY);
  return roles ? JSON.parse(roles) : [];
};

export const setUserId = (userId: number): void => {
  localStorage.setItem(USER_ID_KEY, userId.toString());
};

export const getUserId = (): number | null => {
  const userId = localStorage.getItem(USER_ID_KEY);
  return userId ? parseInt(userId, 10) : null;
};

export const hasRole = (role: string): boolean => {
  const roles = getUserRoles();
  return roles.includes(`ROLE_${role}`) || roles.includes(role);
};

export const isAuthenticated = (): boolean => !!getToken();

const decodeJWT = (token: string): Record<string, unknown> | null => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload) as Record<string, unknown>;
  } catch {
    return null;
  }
};

/**
 * Persiste token y claims mínimos del JWT en localStorage.
 */
export const persistSessionFromToken = (username: string, token: string): void => {
  const decoded = decodeJWT(token);
  if (decoded) {
    const authorities = decoded.authorities;
    if (Array.isArray(authorities) && authorities.every((a) => typeof a === 'string')) {
      setUserRoles(authorities as string[]);
    }
    const uid = decoded.userId;
    if (typeof uid === 'number') {
      setUserId(uid);
    } else if (typeof uid === 'string') {
      setUserId(parseInt(uid, 10));
    }
  }
  setToken(token);
  setUser(username);
};

export const logout = (): void => {
  removeToken();
};
