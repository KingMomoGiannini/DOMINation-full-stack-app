import axios from 'axios';
import { getToken, removeToken } from './authStorage';

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const publicAuthPaths = new Set(['/auth/login', '/auth/register']);

export const http = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

http.interceptors.request.use((config) => {
  const path = (config.url ?? '').split('?')[0];
  const token = getToken();
  if (token && path && !publicAuthPaths.has(path)) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (res) => res,
  (error) => {
    if (!axios.isAxiosError(error) || error.response?.status !== 401) {
      return Promise.reject(error);
    }
    const path = (error.config?.url ?? '').split('?')[0];
    if (path && publicAuthPaths.has(path)) {
      return Promise.reject(error);
    }
    removeToken();
    const next = `/login?expired=1`;
    if (!window.location.pathname.startsWith('/login')) {
      window.location.assign(next);
    }
    return Promise.reject(error);
  }
);
