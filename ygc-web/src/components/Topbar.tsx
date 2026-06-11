import NotificationBell from './NotificationBell';

interface Props {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  actions?: React.ReactNode;
}

export default function Topbar({ title, subtitle, actions }: Props) {
  return (
    <div className="topbar">
      <div>
        <h4>{title}</h4>
        {subtitle && <div className="sub">{subtitle}</div>}
      </div>
      <div className="topbar-actions d-flex align-items-center gap-2">
        {actions}
        <NotificationBell />
      </div>
    </div>
  );
}
