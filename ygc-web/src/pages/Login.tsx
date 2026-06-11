import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { getErrorMessage } from '../api/client';
import { useAuthStore } from '../store/authStore';
import LanguageSwitcher from '../components/LanguageSwitcher';

export default function LoginPage() {
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(email, password);
      if (user.firstLogin) navigate('/change-password');
      else navigate(user.role === 'ADMIN' ? '/admin/dashboard' : '/member/dashboard');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="text-center mb-4">
          <i className="bi bi-gem text-warning fs-1" />
          <h2 className="mt-2">{t('app.name')}</h2>
          <p className="text-muted">{t('login.title')}</p>
        </div>
        <div className="mb-3 auth-lang">
          <LanguageSwitcher />
        </div>
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label">{t('login.email')}</label>
            <input type="email" className="form-control" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="username" />
          </div>
          <div className="mb-3">
            <label className="form-label">{t('login.password')}</label>
            <input type="password" className="form-control" value={password} onChange={(e) => setPassword(e.target.value)} required autoComplete="current-password" />
          </div>
          <button type="submit" className="btn btn-warning w-100" disabled={loading}>{loading ? t('common.loading') : t('login.btn')}</button>
        </form>
        <p className="text-center mt-3 mb-0 small">
          {t('login.new_member')} <Link to="/register">{t('login.register_here')}</Link>
        </p>
      </div>
    </div>
  );
}
