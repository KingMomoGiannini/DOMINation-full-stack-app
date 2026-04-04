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
