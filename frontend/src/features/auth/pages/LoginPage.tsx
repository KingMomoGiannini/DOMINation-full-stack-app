import { useEffect, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate, useSearchParams, Link, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { login } from '../../../api';
import { useAuth } from '../AuthContext';
import { getApiErrorMessage } from '../../../utils/apiError';

const schema = z.object({
  username: z.string().min(1, 'Ingresá tu usuario'),
  password: z.string().min(1, 'Ingresá tu contraseña'),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const { login: authLogin } = useAuth();
  const [sessionMsg, setSessionMsg] = useState<string | null>(null);
  const [providerRenewMsg, setProviderRenewMsg] = useState<string | null>(null);

  const from = (location.state as { from?: string } | null)?.from;

  useEffect(() => {
    if (searchParams.get('expired') === '1') {
      setSessionMsg('Tu sesión expiró o ya no es válida. Volvé a iniciar sesión.');
      searchParams.delete('expired');
      setSearchParams(searchParams, { replace: true });
    }
    if (searchParams.get('providerApproved') === '1') {
      setProviderRenewMsg(
        'Tu rol de prestador ya está activo en el servidor. Iniciá sesión de nuevo para obtener un JWT actualizado y ver el panel de prestador.'
      );
      searchParams.delete('providerApproved');
      setSearchParams(searchParams, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { username: '', password: '' },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data, variables) => {
      if (data.token) {
        authLogin(data.token, variables.username);
        navigate(from || '/', { replace: true });
      }
    },
  });

  const onSubmit = (values: FormValues) => {
    mutation.mutate(values);
  };

  const serverError =
    mutation.error &&
    getApiErrorMessage(mutation.error, 'No pudimos iniciar sesión. Revisá usuario y contraseña.');

  return (
    <div className="main-content">
      <div className="form-container">
        <div className="form-header">
          <h2>Iniciar sesión</h2>
          <p>Accedé a DOMINation para gestionar tus reservas</p>
        </div>

        {sessionMsg && <div className="alert alert-error">⚠️ {sessionMsg}</div>}
        {providerRenewMsg && <div className="alert alert-info alert--stack">{providerRenewMsg}</div>}
        {serverError && <div className="alert alert-error">⚠️ {serverError}</div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              disabled={mutation.isPending}
              {...register('username')}
              placeholder="Tu usuario"
            />
            {errors.username && (
              <small style={{ color: 'var(--danger)' }}>{errors.username.message}</small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              disabled={mutation.isPending}
              {...register('password')}
              placeholder="Tu contraseña"
            />
            {errors.password && (
              <small style={{ color: 'var(--danger)' }}>{errors.password.message}</small>
            )}
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={mutation.isPending}>
            {mutation.isPending ? 'Iniciando sesión…' : 'Iniciar sesión'}
          </button>
        </form>

        <div className="form-footer">
          <p>
            ¿No tenés cuenta? <Link to="/register">Registrate acá</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
