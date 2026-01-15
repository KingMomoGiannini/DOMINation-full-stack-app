import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Home from './pages/Home';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CreateReservationPage from './pages/CreateReservationPage';
import ProviderDashboard from './pages/ProviderDashboard';
import ProviderRequestPage from './pages/ProviderRequestPage';
import AdminProviderRequestsPage from './pages/AdminProviderRequestsPage';
import './App.css';

function Navigation() {
  const { isAuthenticated, username, hasRole, logout } = useAuth();

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <h1>
          DOMI<span className="highlight">Nation</span>
        </h1>
        <div className="navbar-links">
          <Link to="/">Inicio</Link>
          {isAuthenticated ? (
            <>
              <Link to="/reservations/new">Crear Reserva</Link>
              {hasRole('PROVIDER') && <Link to="/provider">Mi Panel</Link>}
              {hasRole('ADMIN') && <Link to="/admin/provider-requests">Solicitudes Prestador</Link>}
              {!hasRole('ADMIN') && !hasRole('PROVIDER') && <Link to="/provider-request">Ser Prestador</Link>}
              <div className="user-info">
                <span>👤 {username}</span>
                <button onClick={logout} className="btn-logout">
                  Cerrar Sesión
                </button>
              </div>
            </>
          ) : (
            <Link to="/login">Iniciar Sesión</Link>
          )}
        </div>
      </div>
    </nav>
  );
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app">
          <Navigation />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/reservations/new" element={<CreateReservationPage />} />
            <Route path="/provider" element={<ProviderDashboard />} />
            <Route path="/provider-request" element={<ProviderRequestPage />} />
            <Route path="/admin/provider-requests" element={<AdminProviderRequestsPage />} />
          </Routes>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

