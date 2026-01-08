import { useEffect, useState } from 'react';
import { getBranches, getItems, Branch, Item } from '../api/apiClient';

export default function Home() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedBranch, setSelectedBranch] = useState<number | undefined>(undefined);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [branchesData, itemsData] = await Promise.all([
        getBranches(),
        getItems()
      ]);
      setBranches(branchesData);
      setItems(itemsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar datos');
      console.error('Error loading data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleBranchFilter = async (branchId?: number) => {
    setSelectedBranch(branchId);
    setLoading(true);
    setError(null);
    try {
      const itemsData = await getItems(branchId);
      setItems(itemsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar items');
    } finally {
      setLoading(false);
    }
  };

  const getItemTypeLabel = (type: string): string => {
    const labels: Record<string, string> = {
      ROOM: 'Sala',
      INSTRUMENT: 'Instrumento',
      ACCESSORY: 'Accesorio',
      OTHER: 'Otro'
    };
    return labels[type] || type;
  };

  const getRentalModeLabel = (mode: string): string => {
    const labels: Record<string, string> = {
      TIME_EXCLUSIVE: 'Exclusivo',
      TIME_QUANTITY: 'Por cantidad'
    };
    return labels[mode] || mode;
  };

  if (loading) {
    return (
      <div className="main-content">
        <div className="loading">Cargando...</div>
      </div>
    );
  }

  return (
    <div className="main-content">
      <h1 className="page-title">
        Descubre Nuestras <span className="highlight">Salas de Ensayo</span>
      </h1>

      {error && (
        <div className="alert alert-error">
          ⚠️ {error}
        </div>
      )}

      {/* Sucursales */}
      <section>
        <div className="section-header">
          <h2>📍 Nuestras Sucursales</h2>
        </div>
        
        <div className="filters-container">
          <button 
            className={`filter-btn ${selectedBranch === undefined ? 'active' : ''}`}
            onClick={() => handleBranchFilter(undefined)}
          >
            Todas
          </button>
          {branches.map(branch => (
            <button 
              key={branch.id}
              className={`filter-btn ${selectedBranch === branch.id ? 'active' : ''}`}
              onClick={() => handleBranchFilter(branch.id)}
            >
              {branch.name}
            </button>
          ))}
        </div>

        <div className="cards-grid">
          {branches.map(branch => (
            <div key={branch.id} className="card">
              <h3>📍 {branch.name}</h3>
              <p>📮 {branch.address}</p>
              <span className="badge">✓ Activa</span>
            </div>
          ))}
        </div>
      </section>

      {/* Items */}
      <section style={{ marginTop: '4rem' }}>
        <div className="section-header">
          <h2>🎹 Equipamiento Disponible</h2>
          {selectedBranch && (
            <p>Mostrando resultados para la sucursal seleccionada</p>
          )}
        </div>
        
        {items.length === 0 ? (
          <div className="empty-state">
            <h3>No hay items disponibles</h3>
            <p>Intenta seleccionar otra sucursal o verifica más tarde</p>
          </div>
        ) : (
          <div className="cards-grid">
            {items.map(item => (
              <div key={item.id} className="card">
                <h3>{item.name}</h3>
                <div style={{ marginBottom: '1rem' }}>
                  <span className="badge">{getItemTypeLabel(item.type)}</span>
                  <span className="badge badge-secondary">
                    {getRentalModeLabel(item.rentalMode)}
                  </span>
                </div>
                <p>
                  📦 Stock disponible: <strong>{item.quantityTotal}</strong>
                </p>
                <div className="price">
                  ${item.basePrice.toLocaleString('es-AR')} / hora
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <div className="alert alert-info" style={{ marginTop: '3rem' }}>
        💡 <strong>¿Listo para reservar?</strong> Inicia sesión para acceder a nuestro sistema de reservas online.
      </div>
    </div>
  );
}

