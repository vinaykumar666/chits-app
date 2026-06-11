interface Props {
  type: 'success' | 'error' | 'info';
  message: string;
  onClose?: () => void;
}

export default function AlertBanner({ type, message, onClose }: Props) {
  const cls = type === 'success' ? 'alert-success' : type === 'error' ? 'alert-danger' : 'alert-info';
  return (
    <div className={`alert ${cls} alert-dismissible fade show`} role="alert">
      {message}
      {onClose && (
        <button type="button" className="btn-close" aria-label="Close" onClick={onClose} />
      )}
    </div>
  );
}
