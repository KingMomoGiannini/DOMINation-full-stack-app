import type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth';
import { persistSessionFromToken } from './authStorage';
import { http } from './http';

export type { AuthResponse, LoginRequest, RegisterRequest } from '../types/auth';

export const login = async (credentials: LoginRequest): Promise<AuthResponse> => {
  const { data } = await http.post<AuthResponse>('/auth/login', credentials);
  if (data.token) {
    persistSessionFromToken(credentials.username, data.token);
  }
  return data;
};

export const register = async (userData: RegisterRequest): Promise<AuthResponse> => {
  const { data } = await http.post<AuthResponse>('/auth/register', userData);
  return data;
};
