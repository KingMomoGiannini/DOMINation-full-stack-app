import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createBranch,
  createRoom,
  deleteBranch,
  getItems,
  getMyBranches,
  getProviderReservations,
  setBranchActive,
  updateBranch,
  type CreateBranchRequest,
  type CreateRoomRequest,
} from '../../../api';
import type { Branch, Item } from '../../../types/catalog';
import { ProviderAreaNav } from '../../../components/provider/ProviderAreaNav';
import { ProviderStatsSummary } from '../../../components/provider/ProviderStatsSummary';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { getApiErrorMessage } from '../../../utils/apiError';

export function ProviderDashboardPage() {
  const queryClient = useQueryClient();
  const [actionBanner, setActionBanner] = useState<{ message: string } | null>(null);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [showBranchForm, setShowBranchForm] = useState(false);
  const [showRoomForm, setShowRoomForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [branchForm, setBranchForm] = useState<CreateBranchRequest>({ name: '', address: '' });
  const [roomForm, setRoomForm] = useState<CreateRoomRequest>({ name: '', hourlyPrice: 0 });
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);

  const branchesQuery = useQuery({
    queryKey: ['providerBranches'],
    queryFn: getMyBranches,
  });

  const reservationsSummaryQuery = useQuery({
    queryKey: ['providerReservations'],
    queryFn: getProviderReservations,
  });

  const branchIds = useMemo(() => (branchesQuery.data ?? []).map((b) => b.id), [branchesQuery.data]);

  const roomsCountQuery = useQuery({
    queryKey: ['providerRoomsTotal', branchIds],
    queryFn: async () => {
      const lists = await Promise.all(branchIds.map((id) => getItems(id, 'ROOM')));
      return lists.reduce((acc, arr) => acc + arr.length, 0);
    },
    enabled: branchIds.length > 0 && branchesQuery.isSuccess,
  });

  const reservationStats = useMemo(() => {
    const list = reservationsSummaryQuery.data ?? [];
    const now = Date.now();
    let cancelled = 0;
    let upcoming = 0;
    let past = 0;
    for (const r of list) {
      if (r.status === 'CANCELLED') {
        cancelled++;
        continue;
      }
      const end = new Date(r.endAt).getTime();
      if (Number.isNaN(end)) continue;
      if (end >= now) upcoming++;
      else past++;
    }
    return { total: list.length, upcoming, past, cancelled };
  }, [reservationsSummaryQuery.data]);

  const branchStats = useMemo(() => {
    const list = branchesQuery.data ?? [];
    const active = list.filter((b) => b.active).length;
    return { total: list.length, active, inactive: list.length - active };
  }, [branchesQuery.data]);

  const roomsQuery = useQuery({
    queryKey: ['providerRooms', selectedId],
    queryFn: () => getItems(selectedId as number, 'ROOM'),
    enabled: selectedId != null && selectedId > 0,
  });

  const createBranchMut = useMutation({
    mutationFn: createBranch,
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['providerBranches'] });
      queryClient.invalidateQueries({ queryKey: ['providerReservations'] });
      queryClient.invalidateQueries({ queryKey: ['providerRoomsTotal'] });
      setActionBanner({ message: `Sucursal «${vars.name}» creada correctamente.` });
      setShowBranchForm(false);
      setBranchForm({ name: '', address: '' });
    },
  });

  const updateBranchMut = useMutation({
    mutationFn: ({ id, body }: { id: number; body: CreateBranchRequest }) => updateBranch(id, body),
    onSuccess: (_data, vars) => {
      queryClient.invalidateQueries({ queryKey: ['providerBranches'] });
      queryClient.invalidateQueries({ queryKey: ['providerReservations'] });
      setActionBanner({ message: `Datos de «${vars.body.name}» actualizados.` });
      setEditingId(null);
    },
  });

  const deleteBranchMut = useMutation({
    mutationFn: deleteBranch,
    onSuccess: (_d, deletedId) => {
      queryClient.invalidateQueries({ queryKey: ['providerBranches'] });
      queryClient.invalidateQueries({ queryKey: ['providerReservations'] });
      queryClient.invalidateQueries({ queryKey: ['providerRoomsTotal'] });
      queryClient.removeQueries({ queryKey: ['providerRooms', deletedId] });
      setSelectedId((cur) => (cur === deletedId ? null : cur));
      setDeleteTargetId(null);
      setActionBanner({ message: 'Sucursal eliminada del catálogo.' });
    },
  });

  const toggleActiveMut = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => setBranchActive(id, active),
    onSuccess: (_d, vars) => {
      queryClient.invalidateQueries({ queryKey: ['providerBranches'] });
      queryClient.invalidateQueries({ queryKey: ['providerReservations'] });
      setActionBanner({
        message: vars.active
          ? 'Sucursal activada: visible según reglas del catálogo.'
          : 'Sucursal desactivada.',
      });
    },
  });

  const createRoomMut = useMutation({
    mutationFn: ({ branchId, body }: { branchId: number; body: CreateRoomRequest }) => createRoom(branchId, body),
    onSuccess: (_d, vars) => {
      queryClient.invalidateQueries({ queryKey: ['providerRooms', vars.branchId] });
      queryClient.invalidateQueries({ queryKey: ['providerRoomsTotal'] });
      setActionBanner({
        message: `Sala «${vars.body.name}» creada en la sucursal seleccionada.`,
      });
      setShowRoomForm(false);
      setRoomForm({ name: '', hourlyPrice: 0 });
    },
  });

  const branches = branchesQuery.data ?? [];
  const rooms: Item[] = roomsQuery.data ?? [];

  const pageError = useMemo(() => {
    const err =
      branchesQuery.error ||
      roomsQuery.error ||
      createBranchMut.error ||
      updateBranchMut.error ||
      deleteBranchMut.error ||
      toggleActiveMut.error ||
      createRoomMut.error;
    return err ? getApiErrorMessage(err, 'Ocurrió un error al procesar la operación.') : null;
  }, [
    branchesQuery.error,
    roomsQuery.error,
    createBranchMut.error,
    updateBranchMut.error,
    deleteBranchMut.error,
    toggleActiveMut.error,
    createRoomMut.error,
  ]);

  const selectedBranch = branches.find((b) => b.id === selectedId) ?? null;
  const deleteTargetBranch = deleteTargetId != null ? branches.find((b) => b.id === deleteTargetId) : null;

  const startEdit = (b: Branch) => {
    setEditingId(b.id);
    setBranchForm({ name: b.name, address: b.address });
  };

  const submitEdit = (e: React.FormEvent, id: number) => {
    e.preventDefault();
    e.stopPropagation();
    updateBranchMut.mutate({ id, body: branchForm });
  };

  const busy =
    createBranchMut.isPending ||
    updateBranchMut.isPending ||
    deleteBranchMut.isPending ||
    toggleActiveMut.isPending ||
    createRoomMut.isPending;

  if (branchesQuery.isPending) {
    return (
      <div className="main-content">
        <Spinner label="Cargando tus sucursales…" />
      </div>
    );
  }

  return (
    <div className="main-content">
      <PageHeader
        title="Panel de"
        highlight="prestador"
        subtitle="Sucursales, estado activo/inactivo y salas (ítems tipo ROOM) según el catálogo."
      />

      <ProviderAreaNav />

      {actionBanner && (
        <div className="alert alert-success alert--stack" role="status">
          <strong>Listo</strong>
          <p style={{ marginTop: '0.35rem', marginBottom: 0 }}>{actionBanner.message}</p>
          <button
            type="button"
            className="btn btn-secondary"
            style={{ marginTop: '0.65rem' }}
            onClick={() => setActionBanner(null)}
          >
            Cerrar aviso
          </button>
        </div>
      )}

      {branches.length > 0 && (
        <ProviderStatsSummary
          branchesTotal={branchStats.total}
          branchesActive={branchStats.active}
          branchesInactive={branchStats.inactive}
          roomsTotal={roomsCountQuery.data ?? 0}
          reservationsTotal={reservationStats.total}
          reservationsUpcoming={reservationStats.upcoming}
          reservationsPast={reservationStats.past}
          reservationsCancelled={reservationStats.cancelled}
          roomsLoading={roomsCountQuery.isPending}
          reservationsLoading={reservationsSummaryQuery.isPending}
        />
      )}

      {pageError && <div className="alert alert-error">⚠️ {pageError}</div>}

      <section style={{ marginBottom: '2.5rem' }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '1rem',
            flexWrap: 'wrap',
            gap: '0.75rem',
          }}
        >
          <h2 style={{ fontFamily: 'Bebas Neue', letterSpacing: '2px', fontSize: '1.75rem' }}>Mis sucursales</h2>
          <button type="button" className="btn btn-success" onClick={() => setShowBranchForm((v) => !v)}>
            {showBranchForm ? 'Cerrar formulario' : '+ Nueva sucursal'}
          </button>
        </div>

        {showBranchForm && (
          <div className="form-container" style={{ marginBottom: '1.5rem' }}>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                createBranchMut.mutate(branchForm);
              }}
            >
              <div className="form-group">
                <label htmlFor="nb-name">Nombre</label>
                <input
                  id="nb-name"
                  value={branchForm.name}
                  onChange={(e) => setBranchForm((f) => ({ ...f, name: e.target.value }))}
                  required
                />
              </div>
              <div className="form-group">
                <label htmlFor="nb-address">Dirección</label>
                <input
                  id="nb-address"
                  value={branchForm.address}
                  onChange={(e) => setBranchForm((f) => ({ ...f, address: e.target.value }))}
                  required
                />
              </div>
              <button type="submit" className="btn btn-success btn-block" disabled={createBranchMut.isPending}>
                {createBranchMut.isPending ? 'Creando…' : 'Crear sucursal'}
              </button>
            </form>
          </div>
        )}

        {branches.length === 0 ? (
          <EmptyState
            title="No tenés sucursales"
            description="Creá la primera para poder cargar salas y recibir reservas."
          />
        ) : (
          <div className="cards-grid">
            {branches.map((branch) => (
              <div
                key={branch.id}
                className="card"
                style={{
                  cursor: 'pointer',
                  border: selectedId === branch.id ? '3px solid var(--primary)' : undefined,
                  textAlign: 'left',
                }}
                onClick={() => setSelectedId(branch.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    setSelectedId(branch.id);
                  }
                }}
                role="button"
                tabIndex={0}
              >
                <h3>{branch.name}</h3>
                <p>📍 {branch.address}</p>
                <p>
                  <span className={branch.active ? 'badge' : 'badge badge-secondary'}>
                    {branch.active ? 'Activa' : 'Inactiva'}
                  </span>
                </p>

                {editingId === branch.id ? (
                  <form
                    style={{ marginTop: '1rem' }}
                    onClick={(e) => e.stopPropagation()}
                    onSubmit={(e) => submitEdit(e, branch.id)}
                  >
                    <div className="form-group">
                      <label>Nombre</label>
                      <input
                        value={branchForm.name}
                        onChange={(e) => setBranchForm((f) => ({ ...f, name: e.target.value }))}
                        required
                      />
                    </div>
                    <div className="form-group">
                      <label>Dirección</label>
                      <input
                        value={branchForm.address}
                        onChange={(e) => setBranchForm((f) => ({ ...f, address: e.target.value }))}
                        required
                      />
                    </div>
                    <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                      <button type="submit" className="btn btn-success" disabled={updateBranchMut.isPending}>
                        Guardar
                      </button>
                      <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={() => setEditingId(null)}
                        disabled={updateBranchMut.isPending}
                      >
                        Cancelar
                      </button>
                    </div>
                  </form>
                ) : (
                  <div
                    style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '1rem' }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button type="button" className="btn btn-secondary" onClick={() => startEdit(branch)} disabled={busy}>
                      Editar
                    </button>
                    <button
                      type="button"
                      className="btn btn-primary"
                      onClick={() =>
                        toggleActiveMut.mutate({ id: branch.id, active: !branch.active })
                      }
                      disabled={busy}
                    >
                      {branch.active ? 'Desactivar' : 'Activar'}
                    </button>
                    <button
                      type="button"
                      className="btn btn-logout"
                      onClick={() => setDeleteTargetId(branch.id)}
                      disabled={busy}
                    >
                      Eliminar
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {selectedBranch && (
        <section style={{ marginTop: '2rem', paddingTop: '2rem', borderTop: '2px solid rgba(255,255,255,0.15)' }}>
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: '1rem',
              flexWrap: 'wrap',
              gap: '0.75rem',
            }}
          >
            <h2 style={{ fontFamily: 'Bebas Neue', letterSpacing: '2px', fontSize: '1.75rem' }}>
              Salas — {selectedBranch.name}
            </h2>
            <button type="button" className="btn btn-success" onClick={() => setShowRoomForm((v) => !v)}>
              {showRoomForm ? 'Cerrar' : '+ Nueva sala'}
            </button>
          </div>

          {roomsQuery.isFetching && !roomsQuery.isPending ? (
            <Spinner label="Actualizando salas…" />
          ) : null}

          {showRoomForm && (
            <div className="form-container" style={{ marginBottom: '1.5rem' }}>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!selectedId) return;
                  createRoomMut.mutate({
                    branchId: selectedId,
                    body: {
                      name: roomForm.name,
                      hourlyPrice: Number(roomForm.hourlyPrice),
                    },
                  });
                }}
              >
                <div className="form-group">
                  <label htmlFor="room-name">Nombre de la sala</label>
                  <input
                    id="room-name"
                    value={roomForm.name}
                    onChange={(e) => setRoomForm((f) => ({ ...f, name: e.target.value }))}
                    required
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="room-price">Precio por hora</label>
                  <input
                    id="room-price"
                    type="number"
                    min={0.01}
                    step={0.01}
                    value={roomForm.hourlyPrice || ''}
                    onChange={(e) =>
                      setRoomForm((f) => ({ ...f, hourlyPrice: parseFloat(e.target.value) || 0 }))
                    }
                    required
                  />
                </div>
                <button type="submit" className="btn btn-success btn-block" disabled={createRoomMut.isPending}>
                  {createRoomMut.isPending ? 'Creando…' : 'Crear sala'}
                </button>
              </form>
            </div>
          )}

          {rooms.length === 0 && !roomsQuery.isPending ? (
            <EmptyState
              title="No hay salas en esta sucursal"
              description="Las salas se listan como ítems ROOM del catálogo para esta sucursal."
            />
          ) : (
            <div className="cards-grid">
              {rooms.map((room) => (
                <div key={room.id} className="card" style={{ textAlign: 'left' }}>
                  <h3>🚪 {room.name}</h3>
                  <p>
                    <span className="badge">ROOM</span>{' '}
                    <span className="badge badge-secondary">{room.rentalMode}</span>
                  </p>
                  <p className="price">${room.basePrice.toLocaleString('es-AR')} / hora</p>
                  <p style={{ marginTop: '0.5rem', color: 'var(--gray-light)' }}>
                    {room.active ? 'Visible en catálogo (activo)' : 'Ítem inactivo en catálogo'}
                  </p>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      <ConfirmDialog
        open={deleteTargetId != null}
        title="¿Eliminar esta sucursal?"
        description={
          deleteTargetBranch
            ? `Se eliminará «${deleteTargetBranch.name}» del catálogo. Las reservas históricas pueden seguir mostrando su id; esta acción no las borra.`
            : 'Se elimina la sucursal en el catálogo.'
        }
        confirmLabel="Sí, eliminar"
        cancelLabel="Cancelar"
        danger
        loading={deleteBranchMut.isPending}
        onCancel={() => setDeleteTargetId(null)}
        onConfirm={() => {
          if (deleteTargetId != null) deleteBranchMut.mutate(deleteTargetId);
        }}
      />
    </div>
  );
}
