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
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="1"></circle>
            <path d="M12 2v6m0 8v6"></path>
            <path d="M4.22 4.22l4.24 4.24m5.08 0l4.24-4.24"></path>
            <path d="M4.22 19.78l4.24-4.24m5.08 0l4.24 4.24"></path>
          </svg>
          <span className="nav-tab-label">Cocinar</span>
        </button>
        <button
          className={`nav-tab ${tab === 'planner' ? 'active' : ''}`}
          onClick={() => onTabChange('planner')}
          style={tab === 'planner' ? { '--screen-primary': 'var(--c4)' } : {}}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
            <line x1="16" y1="2" x2="16" y2="6"></line>
            <line x1="8" y1="2" x2="8" y2="6"></line>
            <line x1="3" y1="10" x2="21" y2="10"></line>
          </svg>
          <span className="nav-tab-label">Semana</span>
        </button>
        <button
          className={`nav-tab ${tab === 'shopping' ? 'active' : ''}`}
          onClick={() => onTabChange('shopping')}
          style={tab === 'shopping' ? { '--screen-primary': 'var(--c2)' } : {}}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="9" cy="21" r="1"></circle>
            <circle cx="20" cy="21" r="1"></circle>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path>
          </svg>
          <span className="nav-tab-label">Lista</span>
        </button>
      </nav>
    </div>
  );
}
