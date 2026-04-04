import { useMemo } from 'react';
import axios from 'axios';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { checkAvailability, createReservation, getBranches, getItems } from '../../../api';
import type { CreateReservationRequest } from '../../../types/booking';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { getApiErrorMessage } from '../../../utils/apiError';

const schema = z
  .object({
    branchId: z.string().min(1, 'Elegí una sucursal'),
    itemId: z.string().min(1, 'Elegí un ítem'),
    quantity: z.coerce.number().int().min(1, 'Mínimo 1'),
    startAt: z.string().min(1, 'Indicá inicio'),
    endAt: z.string().min(1, 'Indicá fin'),
  })
  .refine(
    (data) => {
      if (!data.startAt || !data.endAt) return true;
      return new Date(data.endAt) > new Date(data.startAt);
    },
    { message: 'El fin debe ser posterior al inicio', path: ['endAt'] }
  );

type FormValues = z.infer<typeof schema>;

type ConflictPayload = { conflicts?: { itemId?: number; reason?: string; detail?: string }[] };

function isConflictPayload(e: unknown): e is ConflictPayload {
  return typeof e === 'object' && e !== null && 'conflicts' in e;
}

export function CreateReservationPage() {
  const queryClient = useQueryClient();

  const branchesQuery = useQuery({
    queryKey: ['branches'],
    queryFn: getBranches,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      branchId: '',
      itemId: '',
      quantity: 1,
      startAt: '',
      endAt: '',
    },
  });

  const branchIdStr = useWatch({ control, name: 'branchId' });
  const branchIdNum = branchIdStr ? parseInt(branchIdStr, 10) : NaN;

  const itemsQuery = useQuery({
    queryKey: ['items', branchIdNum],
    queryFn: () => getItems(branchIdNum),
    enabled: Number.isFinite(branchIdNum) && branchIdNum > 0,
  });

  const items = itemsQuery.data ?? [];

  const mutation = useMutation({
    mutationFn: async (payload: CreateReservationRequest) => {
      const availability = await checkAvailability(payload);
      if (!availability.available) {
        const err = new Error(
          'No hay disponibilidad con los datos ingresados. Revisá fechas, sucursal o cantidad.'
        ) as Error & { conflicts?: typeof availability.conflicts };
        err.conflicts = availability.conflicts ?? [];
        throw err;
      }
      return createReservation(payload);
    },
    onSuccess: () => {
      reset({
        branchId: '',
        itemId: '',
        quantity: 1,
        startAt: '',
        endAt: '',
      });
      queryClient.invalidateQueries({ queryKey: ['myReservations'] });
    },
  });

  const onSubmit = (values: FormValues) => {
    mutation.reset();
    const item = items.find((i) => i.id === parseInt(values.itemId, 10));
    const startMs = new Date(values.startAt).getTime();
    const endMs = new Date(values.endAt).getTime();
    const now = Date.now();

    if (startMs < now - 60_000) {
      setError('startAt', {
        type: 'custom',
        message: 'La fecha y hora de inicio no pueden estar en el pasado.',
      });
      return;
    }
    if (endMs <= startMs) {
      setError('endAt', { type: 'custom', message: 'El fin debe ser posterior al inicio.' });
      return;
    }
    if (item?.rentalMode === 'TIME_EXCLUSIVE' && Number(values.quantity) !== 1) {
      setError('quantity', {
        type: 'custom',
        message: 'Para salas (uso exclusivo por franja), la cantidad debe ser 1.',
      });
      return;
    }

    const payload: CreateReservationRequest = {
      branchId: parseInt(values.branchId, 10),
      startAt: values.startAt,
      endAt: values.endAt,
      lines: [{ itemId: parseInt(values.itemId, 10), quantity: values.quantity }],
    };
    mutation.mutate(payload);
  };

  const isAvailabilityRejection =
    mutation.error != null && isConflictPayload(mutation.error);

  const isHttp409 =
    mutation.error != null &&
    axios.isAxiosError(mutation.error) &&
    mutation.error.response?.status === 409;

  const serverError = mutation.error
    ? getApiErrorMessage(mutation.error, 'No pudimos completar la reserva.')
    : null;

  const conflicts = useMemo(() => {
    if (mutation.error && isConflictPayload(mutation.error)) {
      return mutation.error.conflicts ?? [];
    }
    return [];
  }, [mutation.error]);

  const loadingBootstrap = branchesQuery.isPending;

  if (loadingBootstrap) {
    return (
      <div className="main-content">
        <Spinner label="Cargando formulario…" />
      </div>
    );
  }

  const branches = branchesQuery.data ?? [];

  return (
    <div className="main-content">
      <div className="form-container" style={{ maxWidth: '650px' }}>
        <div className="form-header">
          <h2>Nueva reserva</h2>
          <p>Verificamos disponibilidad antes de confirmar tu reserva</p>
        </div>

        {branchesQuery.error && (
          <div className="alert alert-error">
            ⚠️{' '}
            {getApiErrorMessage(branchesQuery.error, 'No pudimos cargar las sucursales.')}
          </div>
        )}

        {mutation.isSuccess && (
          <div className="alert alert-success">
            <strong>Reserva confirmada.</strong>
            <p style={{ marginTop: '0.35rem' }}>
              Ya podés verla en «Mis reservas». Si necesitás otra franja, completá el formulario de nuevo.
            </p>
          </div>
        )}

        {isAvailabilityRejection && (
          <div className="alert alert-error" role="alert">
            <strong>Disponibilidad: no alcanza para esta combinación</strong>
            <p style={{ marginTop: '0.5rem' }}>
              El chequeo previo indica que no hay cupo o la franja no está libre. Ajustá fechas, sucursal, ítem o
              cantidad.
            </p>
            {conflicts.length > 0 && (
              <ul style={{ marginTop: '0.75rem', paddingLeft: '1.25rem', lineHeight: 1.5 }}>
                {conflicts.map((c, i) => (
                  <li key={i}>
                    {c.reason || c.detail || `Ítem ${c.itemId ?? '—'}`}
                  </li>
                ))}
              </ul>
            )}
          </div>
        )}

        {mutation.error != null && !isAvailabilityRejection && (
          <div className="alert alert-error" role="alert">
            <strong>
              {isHttp409 ? 'Conflicto al guardar la reserva' : 'No pudimos completar la reserva'}
            </strong>
            <p style={{ marginTop: '0.5rem' }}>{serverError}</p>
            {isHttp409 && (
              <p style={{ marginTop: '0.5rem', color: 'var(--gray-light)', fontSize: '0.95rem' }}>
                A veces el cupo cambia entre el chequeo y la confirmación. Probá otra franja o actualizá la página
                y reintentá.
              </p>
            )}
          </div>
        )}

        {branches.length === 0 ? (
          <EmptyState title="No hay sucursales disponibles" description="No podés crear una reserva sin sucursales en catálogo." />
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} noValidate>
            <div className="form-group">
              <label htmlFor="branchId">Sucursal *</label>
              <select id="branchId" disabled={mutation.isPending} {...register('branchId')}>
                <option value="">Seleccioná una sucursal</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
              {errors.branchId && (
                <small style={{ color: 'var(--danger)' }}>{errors.branchId.message}</small>
              )}
            </div>

            {branchIdStr && (
              <div className="form-group">
                <label htmlFor="itemId">Ítem *</label>
                {itemsQuery.isPending ? (
                  <Spinner label="Cargando ítems…" />
                ) : items.length === 0 ? (
                  <EmptyState title="Sin ítems en esta sucursal" description="Elegí otra sucursal o probá más tarde." />
                ) : (
                  <>
                    <select id="itemId" disabled={mutation.isPending} {...register('itemId')}>
                      <option value="">Seleccioná un ítem</option>
                      {items.map((item) => (
                        <option key={item.id} value={item.id}>
                          {item.name} — ${item.basePrice} (stock {item.quantityTotal})
                        </option>
                      ))}
                    </select>
                    {errors.itemId && (
                      <small style={{ color: 'var(--danger)' }}>{errors.itemId.message}</small>
                    )}
                  </>
                )}
              </div>
            )}

            <div className="form-group">
              <label htmlFor="startAt">Inicio *</label>
              <input id="startAt" type="datetime-local" disabled={mutation.isPending} {...register('startAt')} />
              {errors.startAt && (
                <small style={{ color: 'var(--danger)' }}>{errors.startAt.message}</small>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="endAt">Fin *</label>
              <input id="endAt" type="datetime-local" disabled={mutation.isPending} {...register('endAt')} />
              {errors.endAt && (
                <small style={{ color: 'var(--danger)' }}>{errors.endAt.message}</small>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="quantity">Cantidad *</label>
              <input id="quantity" type="number" min={1} disabled={mutation.isPending} {...register('quantity')} />
              {errors.quantity && (
                <small style={{ color: 'var(--danger)' }}>{errors.quantity.message}</small>
              )}
              <small style={{ color: '#7f8c8d', display: 'block', marginTop: '0.25rem' }}>
                Para salas (TIME_EXCLUSIVE), usá cantidad 1.
              </small>
            </div>

            <button type="submit" className="btn btn-success btn-block" disabled={mutation.isPending}>
              {mutation.isPending ? 'Verificando y reservando…' : 'Verificar disponibilidad y reservar'}
            </button>
          </form>
        )}

        <div className="alert alert-info" style={{ marginTop: '2rem' }}>
          <strong>Notas:</strong>
          <ul style={{ paddingLeft: '1.5rem', marginTop: '0.75rem', lineHeight: 1.7 }}>
            <li>Primero validamos disponibilidad; si todo ok, se crea la reserva.</li>
            <li>Las fechas deben ser coherentes (fin posterior al inicio).</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
