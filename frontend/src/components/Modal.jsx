export function Modal({ isOpen, title, children, onClose }) {
  if (!isOpen) return null;

  return (
    <div className="modal-backdrop" onClick={(e) => {
      if (e.target === e.currentTarget) onClose();
    }}>
      <div className="modal">
        {title && <h2>{title}</h2>}
        {children}
      </div>
    </div>
  );
}
