import axios from 'axios';

/**
 * Mensaje legible para el usuario a partir de errores de Axios / API.
 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status;
    const data = error.response?.data as Record<string, unknown> | string | undefined;

    if (status === 401) {
      return 'Tu sesión expiró o no tenés autorización para esta acción. Volvé a iniciar sesión.';
    }
    if (status === 403) {
      return 'No tenés permisos para realizar esta operación.';
    }
    if (status === 404) {
      return 'El recurso solicitado no existe o ya no está disponible.';
    }
    if (status === 409) {
      const conflictMsg =
        (data && typeof data === 'object' && typeof data.detail === 'string' && data.detail.trim()
          ? data.detail
          : null) ||
        (data && typeof data === 'object' && typeof data.message === 'string' && data.message.trim()
          ? data.message
          : null);
      return conflictMsg
        ? `Conflicto: ${conflictMsg}`
        : 'La operación no pudo completarse porque el estado actual del servidor no lo permite (por ejemplo, otro usuario reservó antes o la solicitud ya fue procesada).';
    }

    if (typeof data === 'string' && data.trim()) {
      return data;
    }
    if (data && typeof data === 'object') {
      const msg = data.message;
      if (typeof msg === 'string' && msg.trim()) {
        return msg;
      }
      const detail = data.detail;
      if (typeof detail === 'string' && detail.trim()) {
        return detail;
      }
      const title = data.title;
      if (typeof title === 'string' && title.trim()) {
        return title;
      }
    }
    if (error.message && !error.message.startsWith('Request failed')) {
      return error.message;
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}
