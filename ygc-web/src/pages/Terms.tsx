import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import Layout from '../components/Layout';
import Topbar from '../components/Topbar';

export default function TermsPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const role = user?.role ?? 'MEMBER';

  const content = (
    <div className="card">
      <div className="card-header"><i className="bi bi-file-text me-2" />YGC Internal — Terms & Conditions</div>
      <div className="card-body terms-body">
        <h6>1. Membership</h6>
        <p>By registering, you agree to participate in chit groups administered by YGC Internal. Membership is subject to admin approval.</p>
        <h6>2. Payments</h6>
        <p>Members must pay the monthly chit amount by the due date. Late payments incur fines as per chit rules. Payment proof must be uploaded via the platform.</p>
        <h6>3. Auctions</h6>
        <p>Winning bids are binding. The winning member receives the payout minus admin commission and member dividends as calculated by the system.</p>
        <h6>4. Early Exit</h6>
        <p>Early exit requests are subject to admin approval and may include penalties as per the chit agreement.</p>
        <h6>5. Data & Privacy</h6>
        <p>Your personal information (including Aadhaar) is used solely for chit fund administration and fraud prevention, in accordance with applicable laws.</p>
        <h6>6. Disputes</h6>
        <p>All disputes shall be resolved by YGC Internal administration. Decisions are final within the internal chit framework.</p>
        <p className="text-muted small mt-4">Last updated: June 2026 · YGC Internal v4.0</p>
      </div>
    </div>
  );

  if (!user) {
    return (
      <div className="auth-page">
        <div className="auth-card card p-4 mx-auto" style={{ maxWidth: 720 }}>
          <h4 className="mb-3">{t('nav.terms')}</h4>
          {content}
        </div>
      </div>
    );
  }

  return (
    <Layout role={role}>
      <Topbar title={<><i className="bi bi-file-text text-warning me-2" />{t('nav.terms')}</>} />
      {content}
    </Layout>
  );
}
