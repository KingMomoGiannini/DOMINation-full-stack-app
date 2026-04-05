import type { ProviderRequestResponse } from '../../api';
import { countProviderRequestsByStatus } from '../../utils/adminProviderRequestUi';

interface AdminProviderRequestStatsProps {
  rows: ProviderRequestResponse[];
  loading: boolean;
}

export function AdminProviderRequestStats({ rows, loading }: AdminProviderRequestStatsProps) {
  const { total, pending, approved, rejected } = countProviderRequestsByStatus(rows);

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
          <span className="provider-stat-card__hint">Todas las solicitudes devueltas por el servidor en esta carga.</span>
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
