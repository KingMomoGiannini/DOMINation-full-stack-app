export interface ProviderStatsSummaryProps {
  branchesTotal: number;
  branchesActive: number;
  branchesInactive: number;
  roomsTotal: number;
  reservationsTotal: number;
  reservationsUpcoming: number;
  reservationsInProgress: number;
  reservationsCompleted: number;
  reservationsCancelled: number;
  roomsLoading: boolean;
  reservationsLoading: boolean;
}

export function ProviderStatsSummary({
  branchesTotal,
  branchesActive,
  branchesInactive,
  roomsTotal,
  reservationsTotal,
  reservationsUpcoming,
  reservationsInProgress,
  reservationsCompleted,
  reservationsCancelled,
  roomsLoading,
  reservationsLoading,
}: ProviderStatsSummaryProps) {
  return (
    <section className="provider-stats-summary" aria-label="Resumen del prestador">
      <h2 className="provider-stats-summary__title">Resumen</h2>
      <div className="provider-stats-grid">
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Sucursales</span>
          <span className="provider-stat-card__value">{branchesTotal}</span>
          <span className="provider-stat-card__hint">
            {branchesActive} activas · {branchesInactive} inactivas
          </span>
        </div>
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Salas (ROOM)</span>
          {roomsLoading ? (
            <span className="provider-stat-card__loading" aria-busy="true">
              Calculando...
            </span>
          ) : (
            <>
              <span className="provider-stat-card__value">{roomsTotal}</span>
              <span className="provider-stat-card__hint">Total en tus sucursales</span>
            </>
          )}
        </div>
        <div className="provider-stat-card">
          <span className="provider-stat-card__label">Reservas</span>
          {reservationsLoading ? (
            <span className="provider-stat-card__loading" aria-busy="true">
              Cargando...
            </span>
          ) : (
            <>
              <span className="provider-stat-card__value">{reservationsTotal}</span>
              <span className="provider-stat-card__hint">
                {reservationsUpcoming} próximas · {reservationsInProgress} en curso · {reservationsCompleted}{' '}
                finalizadas · {reservationsCancelled} canceladas
              </span>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
