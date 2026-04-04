import type { ReactNode } from 'react';

interface EmptyStateProps {
  title: string;
  description?: string;
  children?: ReactNode;
}

export function EmptyState({ title, description, children }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <h3>{title}</h3>
      {description ? <p>{description}</p> : null}
      {children ? <div style={{ marginTop: '1.5rem' }}>{children}</div> : null}
    </div>
  );
}
