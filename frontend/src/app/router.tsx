import { Routes, Route } from 'react-router-dom';
import { AppLayout } from '../components/layout/AppLayout';
import { RoleRoute } from '../features/auth/RoleRoute';
import { ProviderRequestGate } from '../features/auth/ProviderRequestGate';
import { AccessDeniedPage } from '../features/auth/AccessDeniedPage';
import { HomePage } from '../features/catalog/pages/HomePage';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { RegisterPage } from '../features/auth/pages/RegisterPage';
import { MyReservationsPage } from '../features/booking/pages/MyReservationsPage';
import { CreateReservationPage } from '../features/booking/pages/CreateReservationPage';
import { ProviderDashboardPage } from '../features/provider/pages/ProviderDashboardPage';
import { ProviderReservationsPage } from '../features/provider/pages/ProviderReservationsPage';
import { ProviderRequestPage } from '../features/provider/pages/ProviderRequestPage';
import { AdminProviderRequestsPage } from '../features/admin/pages/AdminProviderRequestsPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/access-denied" element={<AccessDeniedPage />} />

        <Route
          path="/reservations"
          element={
            <RoleRoute allow={['USER']}>
              <MyReservationsPage />
            </RoleRoute>
          }
        />
        <Route
          path="/reservations/new"
          element={
            <RoleRoute allow={['USER']}>
              <CreateReservationPage />
            </RoleRoute>
          }
        />

        <Route
          path="/provider"
          element={
            <RoleRoute allow={['PROVIDER']}>
              <ProviderDashboardPage />
            </RoleRoute>
          }
        />
        <Route
          path="/provider/reservations"
          element={
            <RoleRoute allow={['PROVIDER']}>
              <ProviderReservationsPage />
            </RoleRoute>
          }
        />

        <Route
          path="/provider-request"
          element={
            <ProviderRequestGate>
              <ProviderRequestPage />
            </ProviderRequestGate>
          }
        />

        <Route
          path="/admin/provider-requests"
          element={
            <RoleRoute allow={['ADMIN']}>
              <AdminProviderRequestsPage />
            </RoleRoute>
          }
        />
      </Route>
    </Routes>
  );
}
