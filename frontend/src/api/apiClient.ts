/**
 * Cliente HTTP simple para comunicación con el API Gateway
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const AUTH_BASE_URL = 'http://localhost:9000'; // Auth Service directo
const TOKEN_KEY = 'access_token';
const USER_KEY = 'current_user';

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
  
  // Guardar token y usuario
  setToken(data.token);
  setUser(userData.username);
  
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

