interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
  danger?: boolean;
  /** Mensaje de error (p. ej. 409) mostrado antes de los botones. */
  errorHint?: string | null;
}

export function ConfirmDialog({
  open,
  title,
  description,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  onConfirm,
  onCancel,
  loading = false,
  danger = false,
  errorHint = null,
}: ConfirmDialogProps) {
  if (!open) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.65)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 200,
        padding: '1rem',
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
    >
      <div
        className="form-container"
        style={{ maxWidth: '440px', width: '100%', background: 'var(--secondary)', color: '#fff' }}
      >
        <h3 id="confirm-dialog-title" style={{ marginBottom: '0.75rem' }}>
          {title}
        </h3>
        {description ? (
          <p style={{ marginBottom: '1.25rem', color: 'var(--gray-light)', lineHeight: 1.5 }}>{description}</p>
        ) : null}
        {errorHint ? (
          <p
            style={{
              marginBottom: '1rem',
              padding: '0.65rem 0.85rem',
              borderRadius: 8,
              background: 'rgba(255,0,0,0.12)',
              border: '1px solid rgba(255,80,80,0.45)',
              color: '#ffb3b3',
              fontFamily: 'Arial, sans-serif',
              fontSize: '0.9rem',
              lineHeight: 1.45,
            }}
            role="alert"
          >
            {errorHint}
          </p>
        ) : null}
        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', flexWrap: 'wrap' }}>
          <button type="button" className="btn btn-primary" onClick={onCancel} disabled={loading}>
            {cancelLabel}
          </button>
          <button
            type="button"
            className={danger ? 'btn btn-logout' : 'btn btn-success'}
            onClick={onConfirm}
            disabled={loading}
          >
            {loading ? 'Procesando…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
