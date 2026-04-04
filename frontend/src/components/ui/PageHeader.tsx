interface PageHeaderProps {
  title: string;
  highlight?: string;
  subtitle?: string;
}

export function PageHeader({ title, highlight, subtitle }: PageHeaderProps) {
  return (
    <header className="section-header" style={{ marginBottom: '1.5rem' }}>
      <h1 className="page-title">
        {title}
        {highlight ? (
          <>
            {' '}
            <span className="highlight">{highlight}</span>
          </>
        ) : null}
      </h1>
      {subtitle ? <p style={{ color: 'var(--gray-light)', marginTop: '0.35rem' }}>{subtitle}</p> : null}
    </header>
  );
}
