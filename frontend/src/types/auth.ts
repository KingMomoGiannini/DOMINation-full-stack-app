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
  token: string | null;
}
