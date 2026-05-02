import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api';

export function RecipesPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);

  const filtered = useMemo(() => {
    if (!search.trim()) return recipes;
    const q = search.toLowerCase();
    return recipes.filter((r) => r.name.toLowerCase().includes(q));
  }, [recipes, search]);

  const loadRecipes = async () => {
    try {
      setBusy(true);
      const data = await api.listRecipes();
      setRecipes(data || []);
    } catch (err) {
      notifyError(err, 'recipes');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    if (!isActive) return;
    loadRecipes();
  }, [isActive]);

  const onDelete = async (id) => {
    try {
      setBusy(true);
      await api.deleteRecipe(id);
      notifySuccess('Receta eliminada');
      await loadRecipes();
    } catch (err) {
      notifyError(err, 'recipes');
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <header className="top-header">
        <h1>Mis Recetas</h1>
        <input
          className="input"
          placeholder="Buscar recetas..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button
          className="btn btn-primary"
          onClick={() => {
            setEditing(null);
            setShowForm(true);
          }}
        >
          + Nueva receta
        </button>
      </header>

      <div className="stack">
        {filtered.map((recipe, idx) => (
          <article key={recipe.id} className="card">
            <div className="row between">
              <div className="grow">
                <span className="number-item" style={{ fontSize: '32px', color: 'var(--c3)' }}>
                  {String(idx + 1).padStart(2, '0')}
                </span>
                <h3 style={{ marginTop: '8px' }}>{recipe.name}</h3>
              </div>
              <div className="icon-actions">
                <button onClick={() => { setEditing(recipe); setShowForm(true); }}>✎</button>
                <button onClick={() => onDelete(recipe.id)}>🗑</button>
              </div>
            </div>
            <p className="pill">{recipe.type}</p>
            <p className="tiny muted">{recipe.ingredients?.length || 0} ingredientes · {recipe.usageCount || 0} usos</p>
          </article>
        ))}
        {filtered.length === 0 && <p className="muted">No hay recetas todavía.</p>}
      </div>
    </section>
  );
}
