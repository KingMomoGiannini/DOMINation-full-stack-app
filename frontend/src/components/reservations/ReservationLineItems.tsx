import type { ReservationLine } from '../../types/booking';

interface ReservationLineItemsProps {
  lines: ReservationLine[];
}

export function ReservationLineItems({ lines }: ReservationLineItemsProps) {
  if (!lines?.length) return null;

  return (
    <div className="reservation-lines">
      <p className="reservation-lines__title">Detalle</p>
      <ul className="reservation-lines__list">
        {lines.map((line) => {
          const name = line.itemName?.trim();
          const meta = name ? name : `Ítem catálogo #${line.itemId}`;
          return (
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
              <span className="reservation-lines__meta">{meta}</span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
