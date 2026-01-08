# 🚀 Guía de Configuración - DOMINation V2 Frontend

## Requisitos Previos

- Node.js 18+ instalado
- Backend services corriendo:
  - Auth Service en puerto 9000
  - Gateway en puerto 8080

## Instalación

### 1. Instalar Dependencias

```bash
cd frontend
npm install
```

### 2. Configurar Variables de Entorno

Crea un archivo `.env` en la carpeta `frontend`:

```properties
VITE_API_BASE_URL=http://localhost:8080
```

### 3. Ejecutar en Modo Desarrollo

```bash
npm run dev
```

La aplicación estará disponible en: `http://localhost:5173`

## Estructura del Frontend

```
frontend/
├── src/
│   ├── api/
│   │   └── apiClient.ts       # Cliente HTTP con métodos de API
│   ├── context/
│   │   └── AuthContext.tsx    # Contexto de autenticación
│   ├── pages/
│   │   ├── Home.tsx           # Página principal (pública)
│   │   ├── LoginPage.tsx      # Formulario de login
│   │   ├── RegisterPage.tsx   # Formulario de registro
│   │   └── CreateReservationPage.tsx  # Crear reserva (protegida)
│   ├── App.tsx                # Componente principal
│   ├── App.css                # Estilos globales
│   └── main.tsx               # Entry point
├── index.html                 # HTML base
├── package.json               # Dependencias
└── vite.config.ts             # Config de Vite
```

## Funcionalidades

### Públicas (Sin Login)

- ✅ Ver sucursales disponibles
- ✅ Explorar items y equipamiento
- ✅ Filtrar items por sucursal
- ✅ Ver precios y disponibilidad

### Protegidas (Con Login)

- ✅ Crear nuevas reservas
- ✅ Ver mis reservas
- ✅ Gestionar perfil (próximamente)

## Uso de la Aplicación

### 1. Registro de Usuario

1. Abre `http://localhost:5173/register`
2. Completa el formulario:
   - **Usuario**: Elige un nombre único
   - **Email**: Ingresa tu email
   - **Contraseña**: Mínimo 6 caracteres
3. Haz clic en "Crear Cuenta"
4. Serás redirigido automáticamente al inicio con sesión iniciada

### 2. Iniciar Sesión

1. Haz clic en "Iniciar Sesión" en la navbar
2. Ingresa tus credenciales
3. Haz clic en "Iniciar Sesión"
4. Tu nombre de usuario aparecerá en la navbar

### 3. Navegar Catálogo

1. En la página principal verás:
   - Listado de sucursales
   - Filtros por sucursal
   - Items disponibles con precios
2. Puedes filtrar items por sucursal haciendo clic en los botones

### 4. Crear Reserva

1. Haz clic en "Crear Reserva" en la navbar (requiere login)
2. Selecciona:
   - **Sucursal**: Donde quieres reservar
   - **Item**: Sala o equipamiento
   - **Fecha inicio**: Cuándo empieza
   - **Fecha fin**: Cuándo termina
   - **Cantidad**: Número de unidades
3. Haz clic en "Confirmar Reserva"
4. El sistema validará:
   - Disponibilidad de horario
   - Stock suficiente
   - Que las fechas sean futuras

### 5. Cerrar Sesión

1. Haz clic en "Cerrar Sesión" en la navbar
2. Tu token será eliminado
3. Serás redirigido a la vista pública

## Desarrollo

### Scripts Disponibles

```bash
# Desarrollo con hot reload
npm run dev

# Build para producción
npm run build

# Preview del build
npm run preview

# Linting
npm run lint
```

### Agregar Nueva Página

1. Crea el archivo en `src/pages/MiPagina.tsx`
2. Define el componente:
```typescript
export default function MiPagina() {
  return (
    <div className="main-content">
      <h1 className="page-title">Mi Página</h1>
      {/* Contenido */}
    </div>
  );
}
```

3. Agrega la ruta en `App.tsx`:
```typescript
<Route path="/mi-pagina" element={<MiPagina />} />
```

4. Agrega el link en la navbar si es necesario

### Usar Autenticación en Componentes

```typescript
import { useAuth } from '../context/AuthContext';

function MiComponente() {
  const { isAuthenticated, username, logout } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" />;
  }
  
  return <div>Bienvenido, {username}!</div>;
}
```

### Llamar a API

```typescript
import { apiRequest } from '../api/apiClient';

// Request pública
const branches = await apiRequest('/api/catalog/branches');

// Request protegida
const reservations = await apiRequest('/api/booking/my/reservations', {
  requiresAuth: true
});
```

## Estilos

La aplicación usa CSS moderno con:

- **Variables CSS** para colores y espaciados
- **Gradientes** para fondos y elementos
- **Glassmorphism** para cards y formularios
- **Animaciones** sutiles en hover y transiciones
- **Responsive** design para móviles

### Variables CSS Disponibles

```css
--primary: #6366f1;
--primary-dark: #4f46e5;
--secondary: #ec4899;
--success: #10b981;
--danger: #ef4444;
--dark: #1e293b;
--light: #f8fafc;
```

### Clases Útiles

- `.card` - Card con glassmorphism
- `.btn-primary` - Botón primario con gradiente
- `.btn-success` - Botón de éxito
- `.badge` - Badge para etiquetas
- `.alert-error` - Mensaje de error
- `.alert-success` - Mensaje de éxito
- `.form-container` - Contenedor de formularios

## Troubleshooting

### Problema: "Network Error"

**Causa**: Backend no está corriendo o CORS no configurado

**Solución**:
```bash
# Verificar que el gateway esté corriendo
curl http://localhost:8080/actuator/health

# Verificar que el auth-service esté corriendo
curl http://localhost:9000/actuator/health
```

### Problema: "Token expirado"

**Causa**: El token JWT ha expirado (1 hora)

**Solución**: Vuelve a iniciar sesión

### Problema: "Cannot read property 'useContext' of undefined"

**Causa**: Intentando usar `useAuth` fuera del `AuthProvider`

**Solución**: Asegúrate de que tu componente esté dentro de `<AuthProvider>`

### Problema: Estilos no se aplican

**Causa**: Falta la fuente Inter o CSS no está cargando

**Solución**:
```bash
# Limpia cache y reinstala
rm -rf node_modules
npm install
npm run dev
```

## Build para Producción

### 1. Construir

```bash
npm run build
```

Esto generará la carpeta `dist/` con archivos optimizados.

### 2. Servir con Nginx

```nginx
server {
    listen 80;
    server_name domination.com;
    root /var/www/domination/dist;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api/ {
        proxy_pass http://localhost:8080;
    }
}
```

### 3. Configurar Variables de Entorno

Actualiza `.env.production`:

```properties
VITE_API_BASE_URL=https://api.domination.com
```

## Próximos Pasos

- [ ] Agregar internacionalización (i18n)
- [ ] Implementar dark mode
- [ ] Agregar página de perfil de usuario
- [ ] Implementar sistema de notificaciones
- [ ] Agregar tests unitarios
- [ ] Agregar tests E2E con Playwright
- [ ] Optimizar imágenes y assets
- [ ] Implementar PWA (Progressive Web App)


