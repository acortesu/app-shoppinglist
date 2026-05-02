export function Shell({ children, tab, onTabChange, busy, error, success, onLogout, requireAuth }) {
  return (
    <div className="mobile-shell">
      <div className="brand-stripe" />

      <main className="screen">
        {children}
      </main>

      {error && <div className="banner banner-error">{error}</div>}
      {success && <div className="banner banner-success">{success}</div>}
      {busy && <div className="busy">Cargando...</div>}

      {requireAuth && (
        <button className="logout-chip" onClick={onLogout}>
          Cerrar sesión
        </button>
      )}

      <nav className="bottom-nav">
        <button
          className={tab === 'recipes' ? 'active' : ''}
          onClick={() => onTabChange('recipes')}
        >
          🍽️ Recetas
        </button>
        <button
          className={tab === 'planner' ? 'active' : ''}
          onClick={() => onTabChange('planner')}
        >
          📅 Planificador
        </button>
        <button
          className={tab === 'shopping' ? 'active' : ''}
          onClick={() => onTabChange('shopping')}
        >
          🛒 Compras
        </button>
      </nav>
    </div>
  );
}
