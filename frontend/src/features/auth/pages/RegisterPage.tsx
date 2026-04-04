import { useMutation } from '@tanstack/react-query';
import { useNavigate, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { login, register as registerAccount } from '../../../api';
import { useAuth } from '../AuthContext';
import { getApiErrorMessage } from '../../../utils/apiError';

const schema = z.object({
  username: z.string().min(3, 'Mínimo 3 caracteres').max(50),
  email: z.string().email('Email inválido'),
  password: z.string().min(6, 'Mínimo 6 caracteres').max(100),
  roleType: z.enum(['USER', 'PROVIDER']),
});

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const { login: authLogin } = useAuth();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      username: '',
      email: '',
      password: '',
      roleType: 'USER',
    },
  });

  const mutation = useMutation({
    mutationFn: async (values: FormValues) => {
      await registerAccount(values);
      const auth = await login({ username: values.username, password: values.password });
      return { auth, username: values.username };
    },
    onSuccess: ({ auth, username }) => {
      if (auth.token) {
        authLogin(auth.token, username);
        navigate('/', { replace: true });
      }
    },
  });

  const onSubmit = (values: FormValues) => {
    mutation.mutate(values);
  };

  const serverError =
    mutation.error &&
    getApiErrorMessage(mutation.error, 'No pudimos completar el registro. Intentá de nuevo.');

  return (
    <div className="main-content">
      <div className="form-container">
        <div className="form-header">
          <h2>Crear cuenta</h2>
          <p>Unite a DOMINation y empezá a reservar</p>
        </div>

        {serverError && <div className="alert alert-error">⚠️ {serverError}</div>}

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <div className="form-group">
            <label htmlFor="username">Usuario</label>
            <input
              id="username"
              autoComplete="username"
              disabled={mutation.isPending}
              {...register('username')}
              placeholder="Elegí un nombre de usuario"
            />
            {errors.username && (
              <small style={{ color: 'var(--danger)' }}>{errors.username.message}</small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              disabled={mutation.isPending}
              {...register('email')}
              placeholder="tu@email.com"
            />
            {errors.email && (
              <small style={{ color: 'var(--danger)' }}>{errors.email.message}</small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              disabled={mutation.isPending}
              {...register('password')}
              placeholder="Mínimo 6 caracteres"
            />
            {errors.password && (
              <small style={{ color: 'var(--danger)' }}>{errors.password.message}</small>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="roleType">Tipo de cuenta</label>
            <select id="roleType" disabled={mutation.isPending} {...register('roleType')}>
              <option value="USER">Usuario (reservar)</option>
              <option value="PROVIDER">Prestador (gestionar sucursales)</option>
            </select>
            {errors.roleType && (
              <small style={{ color: 'var(--danger)' }}>{errors.roleType.message}</small>
            )}
          </div>

          <button type="submit" className="btn btn-success btn-block" disabled={mutation.isPending}>
            {mutation.isPending ? 'Creando cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <div className="form-footer">
          <p>
            ¿Ya tenés cuenta? <Link to="/login">Iniciá sesión</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
