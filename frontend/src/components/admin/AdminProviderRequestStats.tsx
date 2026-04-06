import type { AdminProviderRequestSummary } from '../../api';

interface AdminProviderRequestStatsProps {
  summary: AdminProviderRequestSummary | undefined;
  loading: boolean;
}

export function AdminProviderRequestStats({ summary, loading }: AdminProviderRequestStatsProps) {
  const total = summary?.total ?? 0;
  const pending = summary?.pending ?? 0;
  const approved = summary?.approved ?? 0;
  const rejected = summary?.rejected ?? 0;

  return (
    <section className="admin-pr-stats provider-stats-summary" aria-label="Resumen de solicitudes">
      <h2 className="provider-stats-summary__title">Resumen</h2>
      <div className="provider-stats-grid">
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Total en sistema</span>
          {loading ? (
            <span className="provider-stat-card__loading">Cargando…</span>
          ) : (
            <span className="provider-stat-card__value">{total}</span>
          )}
          <span className="provider-stat-card__hint">Conteos globales (endpoint /summary).</span>
        </div>
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Pendientes</span>
          {loading ? (
            <span className="provider-stat-card__loading">Cargando…</span>
          ) : (
            <span className="provider-stat-card__value">{pending}</span>
          )}
        </div>
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Aprobadas</span>
          {loading ? (
            <span className="provider-stat-card__loading">Cargando…</span>
          ) : (
            <span className="provider-stat-card__value">{approved}</span>
          )}
        </div>
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Rechazadas</span>
          {loading ? (
            <span className="provider-stat-card__loading">Cargando…</span>
          ) : (
            <span className="provider-stat-card__value">{rejected}</span>
          )}
        </div>
      </div>
    </section>
  );
}
