import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { getProviderReservations } from '../../../api';
import type { Reservation } from '../../../types/booking';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { getApiErrorMessage } from '../../../utils/apiError';

function formatRange(startAt: string, endAt: string): string {
  try {
    const s = new Date(startAt);
    const e = new Date(endAt);
    const df = new Intl.DateTimeFormat('es-AR', { dateStyle: 'short', timeStyle: 'short' });
    return `${df.format(s)} → ${df.format(e)}`;
  } catch {
    return `${startAt} → ${endAt}`;
  }
}

export function ProviderReservationsPage() {
  const listQuery = useQuery({
    queryKey: ['providerReservations'],
    queryFn: getProviderReservations,
  });

  const rows: Reservation[] = listQuery.data ?? [];
  const errorMsg =
    listQuery.error &&
    getApiErrorMessage(listQuery.error, 'No pudimos cargar las reservas de tus sucursales.');

  return (
    <div className="main-content">
      <PageHeader
        title="Reservas en"
        highlight="mis sucursales"
        subtitle="Listado según el backend (reservas vinculadas a tus sucursales). Solo lectura: cancelación la gestiona el cliente en su cuenta."
      />

      <p style={{ marginBottom: '1rem' }}>
        <Link to="/provider">← Volver al panel de prestador</Link>
      </p>

      {errorMsg && <div className="alert alert-error">⚠️ {errorMsg}</div>}

      {listQuery.isPending ? (
        <Spinner label="Cargando reservas…" />
      ) : rows.length === 0 ? (
        <EmptyState
          title="No hay reservas para mostrar"
          description="Cuando haya reservas en tus sucursales, aparecerán acá."
        />
      ) : (
        <div className="cards-grid" style={{ gridTemplateColumns: '1fr' }}>
          {rows.map((r) => (
            <div key={r.id} className="card" style={{ textAlign: 'left' }}>
              <h3>Reserva #{r.id}</h3>
              <p style={{ marginTop: '0.5rem' }}>{formatRange(r.startAt, r.endAt)}</p>
              <p>
                Sucursal: <strong>{r.branchId}</strong> — Cliente: <strong>{r.customerId}</strong>
              </p>
              <p>
                Estado: <span className="badge badge-secondary">{r.status}</span>
              </p>
              {r.lines?.length > 0 && (
                <ul style={{ marginTop: '0.75rem', paddingLeft: '1.25rem' }}>
                  {r.lines.map((line) => (
                    <li key={line.id}>
                      Ítem #{line.itemId} — cant. {line.quantity} — ${line.price?.toLocaleString('es-AR')}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
