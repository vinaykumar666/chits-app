import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import Layout from '../components/Layout';
import Topbar from '../components/Topbar';

export default function HelpPage() {
  const { t } = useTranslation();
  const user = useAuthStore((s) => s.user);
  const role = user?.role ?? 'MEMBER';

  const content = (
    <>
      <div className="card mb-4">
        <div className="card-header"><i className="bi bi-info-circle me-2" />How YGC Chits Work</div>
        <div className="card-body">
          <p>A chit fund is a rotating savings scheme where members contribute a fixed monthly amount. Each month, one member wins the pooled amount through a bidding auction.</p>
          <ul>
            <li><strong>Join:</strong> Browse open chits and submit a join request with your reason.</li>
            <li><strong>Pay:</strong> Upload QR payment screenshot each month before the due date.</li>
            <li><strong>Bid:</strong> During open auctions, place a bid to win the lump-sum payout.</li>
            <li><strong>Exit:</strong> Request early exit or complete the chit cycle for settlement.</li>
          </ul>
        </div>
      </div>
      <div className="card mb-4">
        <div className="card-header"><i className="bi bi-shield-check me-2" />Rules & Best Practices</div>
        <div className="card-body">
          <ul>
            <li>Pay on time to avoid late fines and maintain your trust rating.</li>
            <li>Keep your Aadhaar verified for full platform access.</li>
            <li>Never share your password or OTP with anyone.</li>
            <li>Contact admin for payment disputes within 48 hours.</li>
          </ul>
        </div>
      </div>
      <div className="card">
        <div className="card-header"><i className="bi bi-telephone me-2" />Support</div>
        <div className="card-body">
          <p className="mb-2">Admin support line:</p>
          <a href="tel:+918919508889" className="btn btn-warning"><i className="bi bi-telephone me-2" />+91 8919508889</a>
        </div>
      </div>
    </>
  );

  if (!user) {
    return (
      <div className="auth-page">
        <div className="auth-card card p-4 mx-auto" style={{ maxWidth: 720 }}>
          <h4 className="mb-3">{t('nav.help')}</h4>
          {content}
        </div>
      </div>
    );
  }

  return (
    <Layout role={role}>
      <Topbar title={<><i className="bi bi-question-circle text-warning me-2" />{t('nav.help')}</>} />
      {content}
    </Layout>
  );
}
