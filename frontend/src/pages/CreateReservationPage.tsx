import { useEffect, useState } from 'react';
import { getBranches, getItems, createReservation, Branch, Item, CreateReservationRequest } from '../api/apiClient';

export default function CreateReservationPage() {
  const [branches, setBranches] = useState<Branch[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const [formData, setFormData] = useState({
    branchId: '',
    startAt: '',
    endAt: '',
    itemId: '',
    quantity: '1'
  });

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (formData.branchId) {
      loadItems(parseInt(formData.branchId));
    }
  }, [formData.branchId]);

  const loadInitialData = async () => {
    setLoading(true);
    try {
      const branchesData = await getBranches();
      setBranches(branchesData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar datos');
    } finally {
      setLoading(false);
    }
  };

  const loadItems = async (branchId: number) => {
    try {
      const itemsData = await getItems(branchId);
      setItems(itemsData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al cargar items');
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    // Validaciones básicas
    if (!formData.branchId || !formData.startAt || !formData.endAt || !formData.itemId) {
      setError('Por favor completa todos los campos obligatorios');
      return;
    }

    setSubmitting(true);

    try {
      const request: CreateReservationRequest = {
        branchId: parseInt(formData.branchId),
        startAt: formData.startAt,
        endAt: formData.endAt,
        lines: [
          {
            itemId: parseInt(formData.itemId),
            quantity: parseInt(formData.quantity)
          }
        ]
      };

      await createReservation(request);
      setSuccess(true);
      
      // Reset form
      setFormData({
        branchId: '',
        startAt: '',
        endAt: '',
        itemId: '',
        quantity: '1'
      });
      setItems([]);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error al crear la reserva');
    } finally {
      setSubmitting(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
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
      <div className="form-container" style={{ maxWidth: '650px' }}>
        <div className="form-header">
          <h2>Nueva Reserva</h2>
          <p>Reserva tu sala o equipamiento preferido</p>
        </div>

        {error && (
          <div className="alert alert-error">
            {error}
          </div>
        )}

        {success && (
          <div className="alert alert-success">
            ✅ ¡Reserva creada exitosamente! Tu reserva ha sido confirmada.
          </div>
        )}

        <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="branchId">Sucursal *</label>
              <select
                id="branchId"
                name="branchId"
                value={formData.branchId}
                onChange={handleChange}
                required
              >
                <option value="">Selecciona una sucursal</option>
                {branches.map(branch => (
                  <option key={branch.id} value={branch.id}>
                    {branch.name}
                  </option>
                ))}
              </select>
            </div>

            {formData.branchId && (
              <div className="form-group">
                <label htmlFor="itemId">Item a reservar *</label>
                <select
                  id="itemId"
                  name="itemId"
                  value={formData.itemId}
                  onChange={handleChange}
                  required
                >
                  <option value="">Selecciona un item</option>
                  {items.map(item => (
                    <option key={item.id} value={item.id}>
                      {item.name} - ${item.basePrice} (Stock: {item.quantityTotal})
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="form-group">
              <label htmlFor="startAt">Fecha y hora de inicio *</label>
              <input
                type="datetime-local"
                id="startAt"
                name="startAt"
                value={formData.startAt}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="endAt">Fecha y hora de fin *</label>
              <input
                type="datetime-local"
                id="endAt"
                name="endAt"
                value={formData.endAt}
                onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label htmlFor="quantity">Cantidad *</label>
              <input
                type="number"
                id="quantity"
                name="quantity"
                value={formData.quantity}
                onChange={handleChange}
                min="1"
                required
              />
              <small style={{ color: '#7f8c8d', display: 'block', marginTop: '0.25rem' }}>
                Para items TIME_EXCLUSIVE (salas), usar cantidad 1
              </small>
            </div>

          <button 
            type="submit" 
            className="btn btn-success btn-block"
            disabled={submitting}
          >
            {submitting ? '⏳ Procesando...' : '✓ Confirmar Reserva'}
          </button>
        </form>

        <div className="alert alert-info" style={{ marginTop: '2rem' }}>
          <strong>💡 Información importante:</strong>
          <ul style={{ paddingLeft: '1.5rem', marginTop: '0.75rem', lineHeight: '1.7' }}>
            <li>Las fechas deben ser futuras</li>
            <li>Para salas, no puede haber solapamiento con otras reservas</li>
            <li>Para instrumentos, verificamos el stock disponible</li>
            <li>El precio se calcula automáticamente</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

