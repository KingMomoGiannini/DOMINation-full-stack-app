import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  approveProviderRequest,
  getAdminProviderRequests,
  rejectProviderRequest,
  type ProviderRequestResponse,
} from '../../../api';
import { AdminProviderRequestStats } from '../../../components/admin/AdminProviderRequestStats';
import { PageHeader } from '../../../components/ui/PageHeader';
import { Spinner } from '../../../components/ui/Spinner';
import { EmptyState } from '../../../components/ui/EmptyState';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { QueryErrorPanel } from '../../../components/ui/QueryErrorPanel';
import { getApiErrorMessage } from '../../../utils/apiError';
import {
  filterProviderRequestsByTab,
  filterProviderRequestsByUserQuery,
  sortProviderRequests,
  type AdminRequestSortMode,
  type AdminRequestStatusTab,
} from '../../../utils/adminProviderRequestUi';

function statusLabel(s?: string): string {
  if (s === 'PENDING') return 'Pendiente';
  if (s === 'APPROVED') return 'Aprobada';
  if (s === 'REJECTED') return 'Rechazada';
  return s ?? '—';
}

function statusBadgeClass(s?: string): string {
  if (s === 'PENDING') return 'admin-pr-badge admin-pr-badge--pending';
  if (s === 'APPROVED') return 'admin-pr-badge admin-pr-badge--approved';
  if (s === 'REJECTED') return 'admin-pr-badge admin-pr-badge--rejected';
  return 'admin-pr-badge admin-pr-badge--unknown';
}

export function AdminProviderRequestsPage() {
  const queryClient = useQueryClient();
  const [statusTab, setStatusTab] = useState<AdminRequestStatusTab>('PENDING');
  const [userQuery, setUserQuery] = useState('');
  const [sortMode, setSortMode] = useState<AdminRequestSortMode>('CREATED_DESC');
  const [confirmApprove, setConfirmApprove] = useState<ProviderRequestResponse | null>(null);
  const [confirmReject, setConfirmReject] = useState<ProviderRequestResponse | null>(null);
  const [actionBanner, setActionBanner] = useState<string | null>(null);

  const listQuery = useQuery({
    queryKey: ['adminProviderRequests'],
    queryFn: () => getAdminProviderRequests(),
  });

  const allRows: ProviderRequestResponse[] = listQuery.data ?? [];

  const userFilterInvalid = userQuery.trim().length > 0 && !/^\d+$/.test(userQuery.trim());

  const { byUser, displayRows, tabCount } = useMemo(() => {
    const tabbed = filterProviderRequestsByTab(allRows, statusTab);
    const userFiltered = userFilterInvalid ? tabbed : filterProviderRequestsByUserQuery(tabbed, userQuery);
    const sorted = sortProviderRequests(userFiltered, sortMode);
    return {
      byUser: userFiltered,
      displayRows: sorted,
      tabCount: tabbed.length,
    };
  }, [allRows, statusTab, userQuery, sortMode, userFilterInvalid]);

  const approveMut = useMutation({
    mutationFn: approveProviderRequest,
    onSuccess: (_void, id) => {
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequests'] });
      setConfirmApprove(null);
      setActionBanner(
        `Solicitud #${id} aprobada. El usuario recibió el rol de prestador y debe volver a iniciar sesión para actualizar el token.`
      );
    },
    onError: () => {
      setActionBanner(null);
    },
  });

  const rejectMut = useMutation({
    mutationFn: rejectProviderRequest,
    onSuccess: (_void, id) => {
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequests'] });
      setConfirmReject(null);
      setActionBanner(`Solicitud #${id} rechazada. Quedó registrada con estado rechazado.`);
    },
    onError: () => {
      setActionBanner(null);
    },
  });

  const approveErrorHint =
    confirmApprove && approveMut.error
      ? getApiErrorMessage(approveMut.error, 'No pudimos aprobar la solicitud.')
      : null;
  const rejectErrorHint =
    confirmReject && rejectMut.error
      ? getApiErrorMessage(rejectMut.error, 'No pudimos rechazar la solicitud.')
      : null;

  const tabButtons: { key: AdminRequestStatusTab; label: string }[] = [
    { key: 'PENDING', label: 'Pendientes' },
    { key: 'APPROVED', label: 'Aprobadas' },
    { key: 'REJECTED', label: 'Rechazadas' },
    { key: 'ALL', label: 'Todas' },
  ];

  const counts = useMemo(() => {
    let p = 0,
      a = 0,
      r = 0;
    for (const row of allRows) {
      if (row.status === 'PENDING') p += 1;
      else if (row.status === 'APPROVED') a += 1;
      else if (row.status === 'REJECTED') r += 1;
    }
    return { p, a, r, all: allRows.length };
  }, [allRows]);

  const tabCountBadge = (key: AdminRequestStatusTab) => {
    if (key === 'PENDING') return counts.p;
    if (key === 'APPROVED') return counts.a;
    if (key === 'REJECTED') return counts.r;
    return counts.all;
  };

  return (
    <div className="main-content">
      <PageHeader
        title="Moderación ·"
        highlight="solicitudes prestador"
        subtitle="Revisá pedidos de rol PROVIDER. Las acciones aplican a la solicitud indicada; los totales reflejan la última carga del servidor."
      />

      <p className="admin-area-intro">
        Área administración · Solo visible con rol <code>ADMIN</code>. Acceso desde la barra: «Moderación prestadores».
      </p>

      {actionBanner && (
        <div className="alert alert-success alert--stack" role="status">
          <strong>Listo</strong>
          <p style={{ marginTop: '0.35rem', marginBottom: 0 }}>{actionBanner}</p>
          <button
            type="button"
            className="btn btn-secondary"
            style={{ marginTop: '0.65rem' }}
            onClick={() => setActionBanner(null)}
          >
            Cerrar
          </button>
        </div>
      )}

      {listQuery.isError && (
        <QueryErrorPanel
          error={listQuery.error}
          fallback="No pudimos cargar las solicitudes de prestador."
          title="Error al cargar solicitudes"
          onRetry={() => listQuery.refetch()}
        />
      )}

      <AdminProviderRequestStats rows={allRows} loading={listQuery.isPending} />

      {!listQuery.isError && (
        <>
          <div className="admin-pr-toolbar">
            <div className="filters-container admin-pr-tabs" role="tablist" aria-label="Filtrar por estado">
              {tabButtons.map(({ key, label }) => (
                <button
                  key={key}
                  type="button"
                  role="tab"
                  aria-selected={statusTab === key}
                  className={`filter-btn ${statusTab === key ? 'active' : ''}`}
                  onClick={() => setStatusTab(key)}
                >
                  {label}
                  <span className="admin-pr-tab-count">{tabCountBadge(key)}</span>
                </button>
              ))}
            </div>

            <div className="reservation-filters admin-pr-filters">
              <div className="reservation-filters__field">
                <label htmlFor="admin-pr-user-filter">Usuario (ID numérico)</label>
                <input
                  id="admin-pr-user-filter"
                  type="text"
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="Ej. 42"
                  value={userQuery}
                  onChange={(e) => setUserQuery(e.target.value)}
                  className="admin-pr-input"
                />
              </div>
              <div className="reservation-filters__field">
                <label htmlFor="admin-pr-sort">Orden</label>
                <select
                  id="admin-pr-sort"
                  value={sortMode}
                  onChange={(e) => setSortMode(e.target.value as AdminRequestSortMode)}
                >
                  <option value="CREATED_DESC">Más recientes primero</option>
                  <option value="CREATED_ASC">Más antiguas primero</option>
                </select>
              </div>
              <p className="reservation-filters__meta admin-pr-filters__meta">
                Mostrando <strong>{displayRows.length}</strong> de <strong>{tabCount}</strong> en esta pestaña
                {userFilterInvalid ? (
                  <>
                    {' '}
                    · <span className="admin-pr-hint-warn">El filtro por usuario acepta solo dígitos.</span>
                  </>
                ) : null}
                {userQuery.trim() && !userFilterInvalid && byUser.length === 0 && tabCount > 0 ? (
                  <>
                    {' '}
                    · Ningún usuario coincide con «{userQuery.trim()}» en esta pestaña.
                  </>
                ) : null}
              </p>
            </div>
          </div>

          {listQuery.isPending ? (
            <Spinner label="Cargando solicitudes…" />
          ) : displayRows.length === 0 ? (
            <EmptyState
              title={allRows.length === 0 ? 'No hay solicitudes' : 'Nada que mostrar con estos filtros'}
              description={
                allRows.length === 0
                  ? 'Cuando los usuarios envíen pedidos de rol prestador, aparecerán acá.'
                  : 'Probá otra pestaña, vaciá el filtro por usuario u ordená distinto.'
              }
            >
              {allRows.length > 0 && (userQuery.trim() || statusTab !== 'PENDING') ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => {
                    setUserQuery('');
                    setStatusTab('ALL');
                  }}
                >
                  Ver todas las solicitudes
                </button>
              ) : null}
            </EmptyState>
          ) : (
            <ul className="admin-pr-list cards-grid" style={{ listStyle: 'none', marginTop: '1rem' }}>
              {displayRows.map((req) => (
                <li key={req.id ?? `${req.userId}-${req.createdAt}`}>
                  <article className="card admin-pr-card">
                    <header className="admin-pr-card__header">
                      <div className="admin-pr-card__titles">
                        <h3 className="admin-pr-card__title">Solicitud #{req.id ?? '—'}</h3>
                        <p className="admin-pr-card__subtitle">Usuario interno · ID {req.userId ?? '—'}</p>
                      </div>
                      <span className={statusBadgeClass(req.status)}>{statusLabel(req.status)}</span>
                    </header>

                    <dl className="admin-pr-meta">
                      <div>
                        <dt>Alta</dt>
                        <dd>
                          {req.createdAt
                            ? new Date(req.createdAt).toLocaleString('es-AR', {
                                dateStyle: 'full',
                                timeStyle: 'short',
                              })
                            : '—'}
                        </dd>
                      </div>
                      {req.message ? (
                        <div className="admin-pr-meta--full">
                          <dt>Nota / mensaje</dt>
                          <dd>{req.message}</dd>
                        </div>
                      ) : null}
                    </dl>

                    {req.status === 'PENDING' ? (
                      <footer className="admin-pr-card__actions">
                        <button
                          type="button"
                          className="btn btn-success"
                          onClick={() => {
                            setActionBanner(null);
                            approveMut.reset();
                            setConfirmApprove(req);
                          }}
                        >
                          Aprobar
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary"
                          onClick={() => {
                            setActionBanner(null);
                            rejectMut.reset();
                            setConfirmReject(req);
                          }}
                        >
                          Rechazar
                        </button>
                      </footer>
                    ) : (
                      <p className="admin-pr-card__readonly">Sin acciones: esta solicitud ya fue procesada.</p>
                    )}
                  </article>
                </li>
              ))}
            </ul>
          )}
        </>
      )}

      <ConfirmDialog
        open={confirmApprove != null}
        title="¿Aprobar esta solicitud?"
        description={
          confirmApprove
            ? `Vas a aprobar la solicitud #${confirmApprove.id ?? '—'} del usuario ID ${confirmApprove.userId ?? '—'}. Se asignará el rol de prestador en el servidor.`
            : undefined
        }
        confirmLabel="Confirmar aprobación"
        errorHint={approveErrorHint}
        loading={approveMut.isPending}
        onCancel={() => {
          setConfirmApprove(null);
          approveMut.reset();
        }}
        onConfirm={() => {
          if (confirmApprove?.id != null) approveMut.mutate(confirmApprove.id);
        }}
      />

      <ConfirmDialog
        open={confirmReject != null}
        title="¿Rechazar esta solicitud?"
        description={
          confirmReject
            ? `Vas a rechazar la solicitud #${confirmReject.id ?? '—'} del usuario ID ${confirmReject.userId ?? '—'}. El estado pasará a rechazado y no se podrá deshacer desde esta pantalla.`
            : undefined
        }
        confirmLabel="Confirmar rechazo"
        danger
        errorHint={rejectErrorHint}
        loading={rejectMut.isPending}
        onCancel={() => {
          setConfirmReject(null);
          rejectMut.reset();
        }}
        onConfirm={() => {
          if (confirmReject?.id != null) rejectMut.mutate(confirmReject.id);
        }}
      />
    </div>
  );
}
