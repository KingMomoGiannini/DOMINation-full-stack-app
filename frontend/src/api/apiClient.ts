/**
 * Cliente HTTP simple para comunicación con el API Gateway
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const AUTH_BASE_URL = 'http://localhost:9000'; // Auth Service directo
const TOKEN_KEY = 'access_token';
const USER_KEY = 'current_user';
const USER_ROLES_KEY = 'user_roles';
const USER_ID_KEY = 'user_id';

interface RequestOptions {
  method?: string;
  body?: unknown;
  requiresAuth?: boolean;
}

/**
 * Obtiene el token de localStorage
 */
export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

/**
 * Guarda el token en localStorage
 */
export const setToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

/**
 * Elimina el token de localStorage
 */
export const removeToken = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(USER_ROLES_KEY);
  localStorage.removeItem(USER_ID_KEY);
};

/**
 * Guarda información del usuario
 */
export const setUser = (username: string): void => {
  localStorage.setItem(USER_KEY, username);
};

/**
 * Obtiene información del usuario
 */
export const getUser = (): string | null => {
  return localStorage.getItem(USER_KEY);
};

/**
 * Guarda los roles del usuario
 */
export const setUserRoles = (roles: string[]): void => {
  localStorage.setItem(USER_ROLES_KEY, JSON.stringify(roles));
};

/**
 * Obtiene los roles del usuario
 */
export const getUserRoles = (): string[] => {
  const roles = localStorage.getItem(USER_ROLES_KEY);
  return roles ? JSON.parse(roles) : [];
};

/**
 * Guarda el userId del usuario
 */
export const setUserId = (userId: number): void => {
  localStorage.setItem(USER_ID_KEY, userId.toString());
};

/**
 * Obtiene el userId del usuario
 */
export const getUserId = (): number | null => {
  const userId = localStorage.getItem(USER_ID_KEY);
  return userId ? parseInt(userId) : null;
};

/**
 * Verifica si el usuario tiene un rol específico
 */
export const hasRole = (role: string): boolean => {
  const roles = getUserRoles();
  return roles.includes(`ROLE_${role}`) || roles.includes(role);
};

/**
 * Decodifica un JWT (sin validar firma, solo para leer claims)
 */
const decodeJWT = (token: string): any => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error('Error decodificando JWT:', error);
    return null;
  }
};

/**
 * Verifica si el usuario está autenticado
 */
export const isAuthenticated = (): boolean => {
  return !!getToken();
};

/**
 * Realiza un request HTTP al backend
 */
export const apiRequest = async <T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> => {
  const { method = 'GET', body, requiresAuth = false } = options;

  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  // Agregar Authorization header si se requiere auth
  if (requiresAuth) {
    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const config: RequestInit = {
    method,
    headers,
  };

  if (body && method !== 'GET') {
    config.body = JSON.stringify(body);
  }

  const url = `${API_BASE_URL}${endpoint}`;

  try {
    const response = await fetch(url, config);

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.detail || `Error ${response.status}: ${response.statusText}`);
    }

    // Si la respuesta es 204 No Content, retornar null
    if (response.status === 204) {
      return null as T;
    }

    return await response.json();
  } catch (error) {
    console.error('API Request Error:', error);
    throw error;
  }
};

// ===== API Methods =====

// ----- Auth Service -----

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  roleType: 'USER' | 'PROVIDER';
}

export interface AuthResponse {
  message: string;
  token: string;
}

export const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
  const response = await fetch(`${AUTH_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al iniciar sesión');
  }

  const data = await response.json();
  
  // Decodificar JWT para obtener roles y userId
  const decoded = decodeJWT(data.token);
  if (decoded) {
    setUserRoles(decoded.authorities || []);
    setUserId(decoded.userId);
  }
  
  // Guardar token y usuario
  setToken(data.token);
  setUser(credentials.username);
  
  return data;
};

export const register = async (userData: RegisterRequest): Promise<AuthResponse> => {
  const response = await fetch(`${AUTH_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al registrarse');
  }

  const data = await response.json();
  
  // El registro exitoso no retorna token, se debe hacer login después
  return data;
};

export const logout = (): void => {
  removeToken();
};

// ----- Catalog Service -----

export interface Branch {
  id: number;
  name: string;
  address: string;
  active: boolean;
  providerId?: number;
}

export interface Item {
  id: number;
  branchId: number;
  name: string;
  type: 'ROOM' | 'INSTRUMENT' | 'ACCESSORY' | 'OTHER';
  rentalMode: 'TIME_EXCLUSIVE' | 'TIME_QUANTITY';
  basePrice: number;
  active: boolean;
  quantityTotal: number;
}

export const getBranches = async (): Promise<Branch[]> => {
  return apiRequest<Branch[]>('/api/catalog/branches');
};

export const getItems = async (branchId?: number, type?: string): Promise<Item[]> => {
  let endpoint = '/api/catalog/items';
  const params = new URLSearchParams();
  
  if (branchId) params.append('branchId', branchId.toString());
  if (type) params.append('type', type);
  
  if (params.toString()) {
    endpoint += `?${params.toString()}`;
  }
  
  return apiRequest<Item[]>(endpoint);
};

// ----- Booking Service -----

export interface Reservation {
  id: number;
  customerId: string;
  branchId: number;
  startAt: string;
  endAt: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  createdAt: string;
  lines: ReservationLine[];
}

export interface ReservationLine {
  id: number;
  itemId: number;
  quantity: number;
  price: number;
}

export interface CreateReservationRequest {
  branchId: number;
  startAt: string;
  endAt: string;
  lines: {
    itemId: number;
    quantity: number;
  }[];
}

export const getMyReservations = async (): Promise<Reservation[]> => {
  return apiRequest<Reservation[]>('/api/booking/my/reservations', { requiresAuth: true });
};

export const createReservation = async (
  request: CreateReservationRequest
): Promise<Reservation> => {
  return apiRequest<Reservation>('/api/booking/reservations', {
    method: 'POST',
    body: request,
    requiresAuth: true,
  });
};

// ----- Provider Endpoints -----

export interface CreateBranchRequest {
  name: string;
  address: string;
}

export interface CreateRoomRequest {
  name: string;
  hourlyPrice: number;
}

export const getMyBranches = async (): Promise<Branch[]> => {
  return apiRequest<Branch[]>('/api/catalog/provider/branches', { requiresAuth: true });
};

export const createBranch = async (request: CreateBranchRequest): Promise<Branch> => {
  return apiRequest<Branch>('/api/catalog/provider/branches', {
    method: 'POST',
    body: request,
    requiresAuth: true,
  });
};

export const updateBranch = async (id: number, request: CreateBranchRequest): Promise<Branch> => {
  return apiRequest<Branch>(`/api/catalog/provider/branches/${id}`, {
    method: 'PUT',
    body: request,
    requiresAuth: true,
  });
};

export const deleteBranch = async (id: number): Promise<void> => {
  return apiRequest<void>(`/api/catalog/provider/branches/${id}`, {
    method: 'DELETE',
    requiresAuth: true,
  });
};

export const createRoom = async (branchId: number, request: CreateRoomRequest): Promise<Item> => {
  return apiRequest<Item>(`/api/catalog/provider/branches/${branchId}/rooms`, {
    method: 'POST',
    body: request,
    requiresAuth: true,
  });
};

// ----- Provider Request Endpoints -----

export interface ProviderRequestResponse {
  id?: number;
  userId?: number;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt?: string;
  message?: string;
}

export const createProviderRequest = async (): Promise<ProviderRequestResponse> => {
  const token = getToken();
  if (!token) throw new Error('No autenticado');
  
  const response = await fetch(`${AUTH_BASE_URL}/auth/provider-requests`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al crear solicitud');
  }

  return await response.json();
};

export const getMyProviderRequest = async (): Promise<ProviderRequestResponse | null> => {
  const token = getToken();
  if (!token) throw new Error('No autenticado');
  
  const response = await fetch(`${AUTH_BASE_URL}/auth/provider-requests/me`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    return null;
  }

  const data = await response.json();
  if (data.message === 'No tienes solicitudes') {
    return null;
  }
  return data;
};

// ----- Admin Provider Request Endpoints -----

export const getAdminProviderRequests = async (status?: string): Promise<ProviderRequestResponse[]> => {
  const token = getToken();
  if (!token) throw new Error('No autenticado');
  
  let url = `${AUTH_BASE_URL}/admin/provider-requests`;
  if (status) {
    url += `?status=${status}`;
  }
  
  const response = await fetch(url, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al obtener solicitudes');
  }

  return await response.json();
};

export const approveProviderRequest = async (id: number): Promise<void> => {
  const token = getToken();
  if (!token) throw new Error('No autenticado');
  
  const response = await fetch(`${AUTH_BASE_URL}/admin/provider-requests/${id}/approve`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al aprobar solicitud');
  }
};

export const rejectProviderRequest = async (id: number): Promise<void> => {
  const token = getToken();
  if (!token) throw new Error('No autenticado');
  
  const response = await fetch(`${AUTH_BASE_URL}/admin/provider-requests/${id}/reject`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || 'Error al rechazar solicitud');
  }
};

