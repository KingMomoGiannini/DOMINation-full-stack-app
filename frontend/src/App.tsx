import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Home from './pages/Home';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import CreateReservationPage from './pages/CreateReservationPage';
import './App.css';

function Navigation() {
  const { isAuthenticated, username, logout } = useAuth();

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
          </Routes>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;

