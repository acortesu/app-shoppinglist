export function ShoppingItem({ item, onUpdate, onRemove }) {
  return (
    <div className={`shopping-item ${item.bought ? 'shopping-item-bought' : ''}`}>
      <button
        className={`shopping-check ${item.bought ? 'shopping-check-done' : ''}`}
        onClick={() => onUpdate(item.id, { bought: !item.bought })}
        aria-label={item.bought ? 'Marcar como no comprado' : 'Marcar como comprado'}
      >
        {item.bought && (
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        )}
      </button>

      <div className="shopping-item-text">
        <span className="shopping-item-name">{item.name}</span>
        {item.note && <span className="shopping-item-note">"{item.note}"</span>}
        {item.suggestedPackages > 0 && (
          <span className="shopping-item-note">
            ~{item.suggestedPackages} paquete{item.suggestedPackages > 1 ? 's' : ''} de {item.packageAmount} {item.packageUnit}
          </span>
        )}
      </div>

      <span className="shopping-item-qty">
        {item.quantity} <span className="shopping-item-unit">{item.unit?.toLowerCase()}</span>
      </span>

      <button className="shopping-item-remove" onClick={() => onRemove(item.id)} aria-label="Eliminar">
        ×
      </button>
    </div>
  );
}
