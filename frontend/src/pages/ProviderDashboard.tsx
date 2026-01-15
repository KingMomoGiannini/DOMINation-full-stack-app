import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
  getMyBranches,
  createBranch,
  deleteBranch,
  getItems,
  createRoom,
  Branch,
  Item,
  CreateBranchRequest,
  CreateRoomRequest,
} from '../api/apiClient';

export default function ProviderDashboard() {
  const { hasRole } = useAuth();
  const navigate = useNavigate();

  const [branches, setBranches] = useState<Branch[]>([]);
  const [selectedBranch, setSelectedBranch] = useState<Branch | null>(null);
  const [rooms, setRooms] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Formularios
  const [showBranchForm, setShowBranchForm] = useState(false);
  const [showRoomForm, setShowRoomForm] = useState(false);
  const [branchForm, setBranchForm] = useState<CreateBranchRequest>({
    name: '',
    address: '',
  });
  const [roomForm, setRoomForm] = useState<CreateRoomRequest>({
    name: '',
    hourlyPrice: 0,
  });

  // Verificar permisos
  useEffect(() => {
    if (!hasRole('PROVIDER')) {
      navigate('/');
    }
  }, [hasRole, navigate]);

  // Cargar sucursales
  useEffect(() => {
    loadBranches();
  }, []);

  // Cargar salas cuando se selecciona una sucursal
  useEffect(() => {
    if (selectedBranch) {
      loadRooms(selectedBranch.id);
    }
  }, [selectedBranch]);

  const loadBranches = async () => {
    try {
      setLoading(true);
      const data = await getMyBranches();
      setBranches(data);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar sucursales');
    } finally {
      setLoading(false);
    }
  };

  const loadRooms = async (branchId: number) => {
    try {
      const data = await getItems(branchId, 'ROOM');
      setRooms(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar salas');
    }
  };

  const handleCreateBranch = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await createBranch(branchForm);
      setSuccess('Sucursal creada exitosamente');
      setBranchForm({ name: '', address: '' });
      setShowBranchForm(false);
      loadBranches();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al crear sucursal');
    }
  };

  const handleDeleteBranch = async (id: number) => {
    if (!confirm('¿Estás seguro de eliminar esta sucursal?')) return;
    try {
      await deleteBranch(id);
      setSuccess('Sucursal eliminada exitosamente');
      if (selectedBranch?.id === id) {
        setSelectedBranch(null);
        setRooms([]);
      }
      loadBranches();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al eliminar sucursal');
    }
  };

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedBranch) return;
    try {
      await createRoom(selectedBranch.id, roomForm);
      setSuccess('Sala creada exitosamente');
      setRoomForm({ name: '', hourlyPrice: 0 });
      setShowRoomForm(false);
      loadRooms(selectedBranch.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al crear sala');
    }
  };

  if (loading) {
    return <div className="main-content"><div className="loading">Cargando</div></div>;
  }

  return (
    <div className="main-content">
      <div className="section-header">
        <h1 className="page-title">
          Panel de <span className="highlight">Prestador</span>
        </h1>
        <p>Gestiona tus sucursales y salas</p>
      </div>

      {error && (
        <div className="alert alert-error">
          ⚠️ {error}
        </div>
      )}

      {success && (
        <div className="alert alert-success">
          ✓ {success}
        </div>
      )}

      {/* Sección de Sucursales */}
      <div style={{ marginBottom: '3rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 className="section-header">
            <span style={{ fontSize: '2rem', color: 'white', fontFamily: 'Bebas Neue', letterSpacing: '2px' }}>
              Mis Sucursales
            </span>
          </h2>
          <button
            className="btn btn-success"
            onClick={() => setShowBranchForm(!showBranchForm)}
          >
            {showBranchForm ? 'Cancelar' : '+ Nueva Sucursal'}
          </button>
        </div>

        {showBranchForm && (
          <div className="form-container" style={{ marginBottom: '2rem' }}>
            <form onSubmit={handleCreateBranch}>
              <div className="form-group">
                <label htmlFor="branchName">Nombre de la Sucursal</label>
                <input
                  type="text"
                  id="branchName"
                  value={branchForm.name}
                  onChange={(e) => setBranchForm({ ...branchForm, name: e.target.value })}
                  placeholder="Ej: DOMINation Centro"
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="branchAddress">Dirección</label>
                <input
                  type="text"
                  id="branchAddress"
                  value={branchForm.address}
                  onChange={(e) => setBranchForm({ ...branchForm, address: e.target.value })}
                  placeholder="Ej: Av. Corrientes 1234, CABA"
                  required
                />
              </div>
              <button type="submit" className="btn btn-success btn-block">
                Crear Sucursal
              </button>
            </form>
          </div>
        )}

        {branches.length === 0 ? (
          <div className="empty-state">
            <h3>No tienes sucursales</h3>
            <p>Crea tu primera sucursal para comenzar a ofrecer espacios</p>
          </div>
        ) : (
          <div className="cards-grid">
            {branches.map((branch) => (
              <div
                key={branch.id}
                className={`card ${selectedBranch?.id === branch.id ? 'active' : ''}`}
                style={{
                  cursor: 'pointer',
                  border: selectedBranch?.id === branch.id ? '3px solid var(--primary)' : undefined,
                }}
                onClick={() => setSelectedBranch(branch)}
              >
                <h3>{branch.name}</h3>
                <p>📍 {branch.address}</p>
                <p>
                  <span className={branch.active ? 'badge' : 'badge badge-secondary'}>
                    {branch.active ? 'Activa' : 'Inactiva'}
                  </span>
                </p>
                <button
                  className="btn btn-secondary"
                  style={{ marginTop: '1rem' }}
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDeleteBranch(branch.id);
                  }}
                >
                  Eliminar
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Sección de Salas */}
      {selectedBranch && (
        <div style={{ marginTop: '3rem', paddingTop: '3rem', borderTop: '2px solid rgba(255, 255, 255, 0.2)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h2 className="section-header">
              <span style={{ fontSize: '2rem', color: 'white', fontFamily: 'Bebas Neue', letterSpacing: '2px' }}>
                Salas de {selectedBranch.name}
              </span>
            </h2>
            <button
              className="btn btn-success"
              onClick={() => setShowRoomForm(!showRoomForm)}
            >
              {showRoomForm ? 'Cancelar' : '+ Nueva Sala'}
            </button>
          </div>

          {showRoomForm && (
            <div className="form-container" style={{ marginBottom: '2rem' }}>
              <form onSubmit={handleCreateRoom}>
                <div className="form-group">
                  <label htmlFor="roomName">Nombre de la Sala</label>
                  <input
                    type="text"
                    id="roomName"
                    value={roomForm.name}
                    onChange={(e) => setRoomForm({ ...roomForm, name: e.target.value })}
                    placeholder="Ej: Sala Ensayo 1"
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="hourlyPrice">Precio por Hora ($)</label>
                  <input
                    type="number"
                    id="hourlyPrice"
                    value={roomForm.hourlyPrice}
                    onChange={(e) => setRoomForm({ ...roomForm, hourlyPrice: parseFloat(e.target.value) })}
                    placeholder="Ej: 8500"
                    min="0"
                    step="0.01"
                    required
                  />
                </div>
                <button type="submit" className="btn btn-success btn-block">
                  Crear Sala
                </button>
              </form>
            </div>
          )}

          {rooms.length === 0 ? (
            <div className="empty-state">
              <h3>No hay salas en esta sucursal</h3>
              <p>Crea tu primera sala para comenzar a recibir reservas</p>
            </div>
          ) : (
            <div className="cards-grid">
              {rooms.map((room) => (
                <div key={room.id} className="card">
                  <h3>🚪 {room.name}</h3>
                  <p>
                    <span className="badge">SALA</span>
                    <span className="badge badge-secondary">EXCLUSIVA</span>
                  </p>
                  <p className="price">
                    ${room.basePrice.toLocaleString()} / hora
                  </p>
                  <p style={{ marginTop: '0.5rem', color: 'var(--gray-light)' }}>
                    {room.active ? '✓ Disponible' : '✗ No disponible'}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

