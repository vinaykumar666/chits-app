import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import ProtectedRoute from './auth/ProtectedRoute';
import LoginPage from './pages/Login';
import RegisterPage from './pages/Register';
import ChangePasswordPage from './pages/ChangePassword';
import MemberDashboard from './pages/member/Dashboard';
import MemberChitsPage from './pages/member/Chits';
import MembershipDetailPage from './pages/member/MembershipDetail';
import AdminDashboard from './pages/admin/Dashboard';
import AdminChitsPage from './pages/admin/Chits';
import AdminChitDetailPage from './pages/admin/ChitDetail';
import AdminMembersPage from './pages/admin/Members';
import AdminPaymentsPage from './pages/admin/Payments';
import AdminAuctionsPage from './pages/admin/Auctions';
import AdminSettlementsPage from './pages/admin/Settlements';
import CommissionReportPage from './pages/admin/CommissionReport';
import AnnouncementsPage from './pages/admin/Announcements';
import AuditPage from './pages/admin/Audit';
import ChitHistoryPage from './pages/admin/ChitHistory';
import { useAuthStore } from './store/authStore';

function HomeRedirect() {
  const { isAuthenticated, user } = useAuthStore();
  if (!isAuthenticated || !user) return <Navigate to="/login" replace />;
  if (user.firstLogin) return <Navigate to="/change-password" replace />;
  return <Navigate to={user.role === 'ADMIN' ? '/admin/dashboard' : '/member/dashboard'} replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomeRedirect />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/change-password" element={<ChangePasswordPage />} />

        <Route element={<ProtectedRoute role="MEMBER" />}>
          <Route path="/member/dashboard" element={<MemberDashboard />} />
          <Route path="/member/chits" element={<MemberChitsPage />} />
          <Route path="/member/memberships/:id" element={<MembershipDetailPage />} />
        </Route>

        <Route element={<ProtectedRoute role="ADMIN" />}>
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/admin/chits" element={<AdminChitsPage />} />
          <Route path="/admin/chits/:id" element={<AdminChitDetailPage />} />
          <Route path="/admin/members" element={<AdminMembersPage />} />
          <Route path="/admin/payments" element={<AdminPaymentsPage />} />
          <Route path="/admin/auctions" element={<AdminAuctionsPage />} />
          <Route path="/admin/settlements" element={<AdminSettlementsPage />} />
          <Route path="/admin/reports/commission" element={<CommissionReportPage />} />
          <Route path="/admin/announcements" element={<AnnouncementsPage />} />
          <Route path="/admin/audit" element={<AuditPage />} />
          <Route path="/admin/chit-history" element={<ChitHistoryPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
