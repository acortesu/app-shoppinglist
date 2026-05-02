export function ShoppingItem({ item, onUpdate, onMove, onDrop, onRemove, draggingId, setDraggingId }) {
  return (
    <div
      draggable
      onDragStart={() => setDraggingId(item.id)}
      onDragEnd={() => setDraggingId('')}
      onDrop={(e) => {
        e.preventDefault();
        onDrop(draggingId, item.id);
      }}
      onDragOver={(e) => e.preventDefault()}
      style={{
        opacity: draggingId === item.id ? 0.5 : 1,
        background: draggingId && draggingId !== item.id ? 'var(--surface)' : 'transparent',
        cursor: 'grab',
        padding: '8px',
        borderRadius: '8px',
        transition: 'all 0.2s'
      }}
    >
      <div className="row between" style={{ alignItems: 'flex-start' }}>
        <div className="grow" style={{ minWidth: 0 }}>
          <div className="row gap-sm">
            <input
              type="checkbox"
              checked={item.bought}
              onChange={(e) => onUpdate(item.id, { bought: e.target.checked })}
              style={{ marginTop: '2px', cursor: 'pointer' }}
            />
            <div style={{ flex: 1, minWidth: 0 }}>
              <input
                type="text"
                className="inline-name"
                value={item.name}
                onChange={(e) => onUpdate(item.id, { name: e.target.value })}
                style={{ textDecoration: item.bought ? 'line-through' : 'none', opacity: item.bought ? 0.5 : 1 }}
              />
              {item.ingredientId && <p className="tiny muted" style={{ margin: '2px 0 0' }}>{item.ingredientId}</p>}
              {item.suggestedPackages > 0 && (
                <p className="tiny muted" style={{ margin: '2px 0 0' }}>
                  ~{item.suggestedPackages} paquete{item.suggestedPackages > 1 ? 's' : ''} de {item.packageAmount} {item.packageUnit}
                </p>
              )}
              {item.note && <p className="tiny muted" style={{ margin: '2px 0 0', fontStyle: 'italic' }}>"{item.note}"</p>}
            </div>
          </div>
        </div>

        <div className="row gap-sm" style={{ marginLeft: '8px', flexShrink: 0 }}>
          <span className="quantity-text">{item.quantity} {item.unit}</span>
          <div className="row gap-sm">
            <button
              className="icon-btn"
              onClick={() => onMove(item.id, -1)}
              title="Mover arriba"
              style={{ padding: '6px' }}
            >
              ↑
            </button>
            <button
              className="icon-btn"
              onClick={() => onMove(item.id, 1)}
              title="Mover abajo"
              style={{ padding: '6px' }}
            >
              ↓
            </button>
            <button
              className="icon-btn danger"
              onClick={() => onRemove(item.id)}
              title="Eliminar"
              style={{ padding: '6px' }}
            >
              🗑
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
