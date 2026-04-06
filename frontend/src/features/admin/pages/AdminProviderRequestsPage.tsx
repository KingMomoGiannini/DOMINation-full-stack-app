import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  approveProviderRequest,
  getAdminProviderRequestSummary,
  getAdminProviderRequestsPage,
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

type AdminRequestSortMode = 'CREATED_DESC' | 'CREATED_ASC';
type AdminRequestStatusTab = 'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED';

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
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [confirmApprove, setConfirmApprove] = useState<ProviderRequestResponse | null>(null);
  const [confirmReject, setConfirmReject] = useState<ProviderRequestResponse | null>(null);
  const [actionBanner, setActionBanner] = useState<string | null>(null);

  const userFilterInvalid = userQuery.trim().length > 0 && !/^\d+$/.test(userQuery.trim());
  const userIdParsed = useMemo(() => {
    const t = userQuery.trim();
    if (!t || !/^\d+$/.test(t)) return undefined;
    const n = Number(t);
    return Number.isSafeInteger(n) ? n : undefined;
  }, [userQuery]);

  const listParams = useMemo(
    () => ({
      page,
      size: pageSize,
      status: statusTab,
      userId: userIdParsed,
      sort: sortMode === 'CREATED_ASC' ? 'createdAt,asc' : 'createdAt,desc',
    }),
    [page, pageSize, statusTab, userIdParsed, sortMode]
  );

  useEffect(() => {
    setPage(0);
  }, [statusTab, userQuery, sortMode, pageSize]);

  const summaryQuery = useQuery({
    queryKey: ['adminProviderRequestSummary'],
    queryFn: getAdminProviderRequestSummary,
  });

  const listQuery = useQuery({
    queryKey: ['adminProviderRequests', listParams],
    queryFn: () => getAdminProviderRequestsPage(listParams),
    placeholderData: (p) => p,
  });

  const displayRows = listQuery.data?.content ?? [];
  const pg = listQuery.data;

  const approveMut = useMutation({
    mutationFn: approveProviderRequest,
    onSuccess: (_void, id) => {
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequests'] });
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequestSummary'] });
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
      queryClient.invalidateQueries({ queryKey: ['adminProviderRequestSummary'] });
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

  const counts = summaryQuery.data;

  const tabCountBadge = (key: AdminRequestStatusTab) => {
    if (!counts) return '—';
    if (key === 'PENDING') return counts.pending;
    if (key === 'APPROVED') return counts.approved;
    if (key === 'REJECTED') return counts.rejected;
    return counts.total;
  };

  const listInitialLoading = listQuery.isPending && !listQuery.data;

  return (
    <div className="main-content">
      <PageHeader
        title="Moderación ·"
        highlight="solicitudes prestador"
        subtitle="Listado paginado en servidor: filtro por estado, ID de usuario exacto y orden por fecha de alta. Los totales del resumen vienen de un endpoint liviano."
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

      <AdminProviderRequestStats summary={summaryQuery.data} loading={summaryQuery.isPending} />

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
                <label htmlFor="admin-pr-user-filter">Usuario (ID exacto)</label>
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
                  <option value="CREATED_DESC">Alta: más recientes primero</option>
                  <option value="CREATED_ASC">Alta: más antiguas primero</option>
                </select>
              </div>
              <div className="reservation-filters__field">
                <label htmlFor="admin-pr-page-size">Por página</label>
                <select
                  id="admin-pr-page-size"
                  value={String(pageSize)}
                  onChange={(e) => setPageSize(Number(e.target.value))}
                >
                  <option value="10">10</option>
                  <option value="20">20</option>
                  <option value="50">50</option>
                </select>
              </div>
              <p className="reservation-filters__meta admin-pr-filters__meta">
                Página <strong>{(pg?.number ?? 0) + 1}</strong> de <strong>{Math.max(pg?.totalPages ?? 1, 1)}</strong> ·{' '}
                <strong>{pg?.numberOfElements ?? displayRows.length}</strong> fila(s) en esta página ·{' '}
                <strong>{pg?.totalElements ?? 0}</strong> resultado(s) con filtros actuales
                {userFilterInvalid ? (
                  <>
                    {' '}
                    · <span className="admin-pr-hint-warn">El filtro por usuario acepta solo dígitos (ID exacto).</span>
                  </>
                ) : null}
                {listQuery.isFetching && !listInitialLoading ? (
                  <>
                    {' '}
                    · <span className="admin-pr-hint-warn">Actualizando…</span>
                  </>
                ) : null}
              </p>
            </div>
          </div>

          <nav className="pagination-bar" aria-label="Páginas de solicitudes">
            <button
              type="button"
              className="btn btn-secondary"
              disabled={pg?.first !== false}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
            >
              Anterior
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              disabled={pg?.last !== false}
              onClick={() => setPage((p) => p + 1)}
            >
              Siguiente
            </button>
          </nav>

          {listInitialLoading ? (
            <Spinner label="Cargando solicitudes…" />
          ) : (pg?.totalElements ?? 0) === 0 ? (
            <EmptyState
              title={summaryQuery.data?.total === 0 ? 'No hay solicitudes' : 'Nada coincide con estos filtros'}
              description={
                summaryQuery.data?.total === 0
                  ? 'Cuando los usuarios envíen pedidos de rol prestador, aparecerán acá.'
                  : 'Probá otra pestaña, quitá el filtro por usuario o cambiá la página.'
              }
            >
              {summaryQuery.data != null && summaryQuery.data.total > 0 ? (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => {
                    setUserQuery('');
                    setStatusTab('ALL');
                    setPage(0);
                  }}
                >
                  Quitar filtro de usuario y ver todas
                </button>
              ) : null}
            </EmptyState>
          ) : displayRows.length === 0 ? (
            <EmptyState
              title="Página sin filas"
              description="Esta página está vacía. Volvé atrás o cambiá el tamaño de página."
            >
              <button type="button" className="btn btn-primary" onClick={() => setPage(0)}>
                Ir a la primera página
              </button>
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
