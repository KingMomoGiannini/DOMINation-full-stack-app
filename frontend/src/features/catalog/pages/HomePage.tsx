import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getBranches, getItems } from '../../../api';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { getApiErrorMessage } from '../../../utils/apiError';
import { useAuth } from '../../auth/AuthContext';

export function HomePage() {
  const { isAuthenticated } = useAuth();
  const [selectedBranch, setSelectedBranch] = useState<number | undefined>(undefined);

  const branchesQuery = useQuery({
    queryKey: ['branches'],
    queryFn: getBranches,
  });

  const itemsQuery = useQuery({
    queryKey: ['items', selectedBranch],
    queryFn: () => getItems(selectedBranch),
  });

  const getItemTypeLabel = (type: string): string => {
    const labels: Record<string, string> = {
      ROOM: 'Sala',
      INSTRUMENT: 'Instrumento',
      ACCESSORY: 'Accesorio',
      OTHER: 'Otro',
    };
    return labels[type] || type;
  };

  const getRentalModeLabel = (mode: string): string => {
    const labels: Record<string, string> = {
      TIME_EXCLUSIVE: 'Exclusivo',
      TIME_QUANTITY: 'Por cantidad',
    };
    return labels[mode] || mode;
  };

  const branches = branchesQuery.data ?? [];
  const items = itemsQuery.data ?? [];
  const loading = branchesQuery.isPending || itemsQuery.isPending;
  const rawError = branchesQuery.error ?? itemsQuery.error;
  const error = rawError
    ? getApiErrorMessage(
        rawError,
        'No pudimos cargar el catálogo. Intentá de nuevo en unos minutos.'
      )
    : null;

  if (loading) {
    return (
      <div className="main-content">
        <Spinner label="Cargando catálogo…" />
      </div>
    );
  }

  return (
    <div className="main-content">
      <h1 className="page-title">
        Descubrí nuestras <span className="highlight">salas de ensayo</span>
      </h1>

      {error && <div className="alert alert-error">⚠️ {error}</div>}

      <section>
        <div className="section-header">
          <h2>📍 Sucursales</h2>
        </div>

        <div className="filters-container">
          <button
            type="button"
            className={`filter-btn ${selectedBranch === undefined ? 'active' : ''}`}
            onClick={() => setSelectedBranch(undefined)}
          >
            Todas
          </button>
          {branches.map((branch) => (
            <button
              key={branch.id}
              type="button"
              className={`filter-btn ${selectedBranch === branch.id ? 'active' : ''}`}
              onClick={() => setSelectedBranch(branch.id)}
            >
              {branch.name}
            </button>
          ))}
        </div>

        {branches.length === 0 ? (
          <EmptyState
            title="No hay sucursales"
            description="Cuando el catálogo tenga sucursales activas, las verás acá."
          />
        ) : (
          <div className="cards-grid">
            {branches.map((branch) => (
              <div key={branch.id} className="card">
                <h3>📍 {branch.name}</h3>
                <p>📮 {branch.address}</p>
                <span className="badge">{branch.active ? '✓ Activa' : 'Inactiva'}</span>
              </div>
            ))}
          </div>
        )}
      </section>

      <section style={{ marginTop: '4rem' }}>
        <div className="section-header">
          <h2>🎹 Equipamiento</h2>
          {selectedBranch !== undefined && <p>Filtrado por sucursal seleccionada</p>}
        </div>

        {itemsQuery.isFetching && !itemsQuery.isPending ? (
          <Spinner label="Actualizando ítems…" />
        ) : items.length === 0 ? (
          <EmptyState
            title="No hay ítems para mostrar"
            description="Probá otro filtro de sucursal o volvé más tarde."
          />
        ) : (
          <div className="cards-grid">
            {items.map((item) => (
              <div key={item.id} className="card">
                <h3>{item.name}</h3>
                <div style={{ marginBottom: '1rem' }}>
                  <span className="badge">{getItemTypeLabel(item.type)}</span>
                  <span className="badge badge-secondary">{getRentalModeLabel(item.rentalMode)}</span>
                </div>
                <p>
                  📦 Stock total: <strong>{item.quantityTotal}</strong>
                </p>
                <div className="price">${item.basePrice.toLocaleString('es-AR')} / hora</div>
              </div>
            ))}
          </div>
        )}
      </section>

      {!isAuthenticated && (
        <div className="alert alert-info" style={{ marginTop: '3rem' }}>
          💡 <strong>¿Querés reservar?</strong>{' '}
          <Link to="/login">Iniciá sesión</Link> o <Link to="/register">creá una cuenta</Link>.
        </div>
      )}
    </div>
  );
}
