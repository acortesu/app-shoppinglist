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
          className={`nav-tab ${tab === 'recipes' ? 'active' : ''}`}
          onClick={() => onTabChange('recipes')}
          style={tab === 'recipes' ? { '--screen-primary': 'var(--c3)' } : {}}
        >
          <span>🍳</span>
          <span className="nav-tab-label">Cocinar</span>
        </button>
        <button
          className={`nav-tab ${tab === 'planner' ? 'active' : ''}`}
          onClick={() => onTabChange('planner')}
          style={tab === 'planner' ? { '--screen-primary': 'var(--c4)' } : {}}
        >
          <span>📅</span>
          <span className="nav-tab-label">Semana</span>
        </button>
        <button
          className={`nav-tab ${tab === 'shopping' ? 'active' : ''}`}
          onClick={() => onTabChange('shopping')}
          style={tab === 'shopping' ? { '--screen-primary': 'var(--c2)' } : {}}
        >
          <span>🛒</span>
          <span className="nav-tab-label">Lista</span>
        </button>
      </nav>
    </div>
  );
}
