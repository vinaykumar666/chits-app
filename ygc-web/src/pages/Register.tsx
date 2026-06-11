import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/auth';
import { getErrorMessage } from '../api/client';

export default function RegisterPage() {
  const [form, setForm] = useState({ email: '', fullName: '', phone: '', address: '' });
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);
    try {
      const { data } = await authApi.register(form);
      setMessage(data.message);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2 className="text-center mb-4">Register</h2>
        {message && <div className="alert alert-success">{message}</div>}
        {error && <div className="alert alert-danger">{error}</div>}
        <form onSubmit={handleSubmit}>
          {(['email', 'fullName', 'phone', 'address'] as const).map((field) => (
            <div className="mb-3" key={field}>
              <label className="form-label text-capitalize">{field === 'fullName' ? 'Full Name' : field}</label>
              <input
                type={field === 'email' ? 'email' : 'text'}
                className="form-control"
                required={field === 'email' || field === 'fullName'}
                value={form[field]}
                onChange={(e) => setForm({ ...form, [field]: e.target.value })}
              />
            </div>
          ))}
          <button type="submit" className="btn btn-warning w-100 fw-bold" disabled={loading}>Register</button>
        </form>
        <p className="text-center mt-3 mb-0"><Link to="/login">Back to login</Link></p>
      </div>
    </div>
  );
}
