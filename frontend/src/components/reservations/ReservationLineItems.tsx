import type { ReservationLine } from '../../types/booking';

interface ReservationLineItemsProps {
  lines: ReservationLine[];
}

/**
 * El DTO de línea solo incluye itemId, no el nombre del ítem (ver gaps en doc del sprint).
 */
export function ReservationLineItems({ lines }: ReservationLineItemsProps) {
  if (!lines?.length) return null;

  return (
    <div className="reservation-lines">
      <p className="reservation-lines__title">Detalle</p>
      <ul className="reservation-lines__list">
        {lines.map((line) => (
          <li key={line.id} className="reservation-lines__item">
            <span className="reservation-lines__main">
              Cantidad {line.quantity}
              {line.price != null && (
                <>
                  {' '}
                  · Total línea ${line.price.toLocaleString('es-AR')}
                </>
              )}
            </span>
            <span className="reservation-lines__meta">Ítem catálogo #{line.itemId}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
