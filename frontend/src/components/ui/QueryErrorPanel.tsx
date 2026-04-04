import { getApiErrorMessage } from '../../utils/apiError';

interface QueryErrorPanelProps {
  error: unknown;
  fallback: string;
  title?: string;
  onRetry?: () => void;
}

/**
 * Panel de error para queries con mensaje unificado y reintento opcional.
 */
export function QueryErrorPanel({ error, fallback, title = 'No pudimos cargar los datos', onRetry }: QueryErrorPanelProps) {
  const message = getApiErrorMessage(error, fallback);

  return (
    <div className="alert alert-error query-error-panel" role="alert">
      <strong>{title}</strong>
      <p style={{ marginTop: '0.5rem', lineHeight: 1.5 }}>{message}</p>
      {onRetry ? (
        <button type="button" className="btn btn-primary" style={{ marginTop: '0.75rem' }} onClick={onRetry}>
          Reintentar
        </button>
      ) : null}
    </div>
  );
}
