import { useContext, useEffect, useMemo, useState } from 'react';
import { api } from '../../api';
import { RecipeModalContext } from '../../contexts/RecipeModalContext';
import { RecipeFormModal } from './RecipeFormModal';

const NUMBER_COLORS = ['var(--c3)', 'var(--c1)', 'var(--c4)', 'var(--c5)', 'var(--c6)', 'var(--c2)'];
const TYPE_COLORS = { BREAKFAST: 'var(--c2)', LUNCH: 'var(--c1)', DINNER: 'var(--c4)' };
const MEAL_LABEL = { BREAKFAST: 'Desayuno', LUNCH: 'Almuerzo', DINNER: 'Cena' };
const TAG_COLORS = ['var(--c5)', 'var(--c6)', 'var(--c2)', 'var(--c1)'];

export function RecipesPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const { showForm, setShowForm, editing, setEditing, openNewRecipe, openEditRecipe, closeForm } = useContext(RecipeModalContext);
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState('');
  const [showSearch, setShowSearch] = useState(false);
  const [activeFilter, setActiveFilter] = useState(null);

  const todayLabel = new Date().toLocaleDateString('es-CR', {
    weekday: 'long',
    day: 'numeric',
    month: 'short'
  }).toUpperCase();

  const uniqueTags = useMemo(() => {
    const set = new Set();
    recipes.forEach((r) => r.tags?.forEach((t) => set.add(t)));
    return [...set];
  }, [recipes]);

  const filtered = useMemo(() => {
    let list = recipes;

    if (activeFilter) {
      if (['BREAKFAST', 'LUNCH', 'DINNER'].includes(activeFilter)) {
        list = list.filter((r) => r.type === activeFilter);
      } else if (activeFilter.startsWith('tag:')) {
        const tag = activeFilter.slice(4);
        list = list.filter((r) => r.tags?.includes(tag));
      }
    }

    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((r) => r.name.toLowerCase().includes(q));
    }

    return list;
  }, [recipes, activeFilter, search]);

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

  const typesWithRecipes = ['BREAKFAST', 'LUNCH', 'DINNER'].filter((t) =>
    recipes.some((r) => r.type === t)
  );

  return (
    <section>
      <header className="recipe-header">
        <div className="recipe-header-top">
          <span className="recipe-date">{todayLabel}</span>
          <button
            className="recipe-search-btn"
            onClick={() => setShowSearch((s) => !s)}
            title="Buscar"
          >
            🔍
          </button>
        </div>

        <h1 className="recipe-title">
          Tus recetas,
          <span className="recipe-title-accent">a la mano.</span>
        </h1>

        {showSearch && (
          <input
            className="input recipe-search-bar"
            placeholder="Buscar recetas..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            autoFocus
          />
        )}

        <div className="filter-row">
          <button
            className={`filter-pill ${!activeFilter ? '' : 'inactive'}`}
            style={{ background: '#111111' }}
            onClick={() => setActiveFilter(null)}
          >
            Todas · {recipes.length}
          </button>

          {typesWithRecipes.map((t) => (
            <button
              key={t}
              className={`filter-pill ${activeFilter === t ? '' : 'inactive'}`}
              style={{ background: TYPE_COLORS[t] }}
              onClick={() => setActiveFilter(t)}
            >
              {MEAL_LABEL[t]}
            </button>
          ))}

          {uniqueTags.map((tag, i) => (
            <button
              key={tag}
              className={`filter-pill ${activeFilter === 'tag:' + tag ? '' : 'inactive'}`}
              style={{ background: TAG_COLORS[i % TAG_COLORS.length] }}
              onClick={() => setActiveFilter('tag:' + tag)}
            >
              {tag}
            </button>
          ))}
        </div>
      </header>

      <div className="recipe-list">
        {filtered.map((recipe, idx) => (
          <div
            key={recipe.id}
            className="recipe-list-item"
            onClick={() => openEditRecipe(recipe)}
          >
            <span
              className="recipe-number"
              style={{ color: NUMBER_COLORS[idx % NUMBER_COLORS.length] }}
            >
              {String(idx + 1).padStart(2, '0')}
            </span>
            <div className="recipe-info">
              <p className="recipe-name">{recipe.name}</p>
              <p className="recipe-meta">
                {MEAL_LABEL[recipe.type]} · {recipe.ingredients?.length || 0} ingredientes
              </p>
            </div>
            <span className="recipe-arrow">→</span>
          </div>
        ))}
        {filtered.length === 0 && (
          <p className="muted" style={{ padding: '24px 0' }}>No hay recetas.</p>
        )}
      </div>

      <button
        className="fab"
        onClick={openNewRecipe}
        title="Nueva receta"
      >
        +
      </button>

      <RecipeFormModal
        isOpen={showForm}
        initial={editing}
        onClose={closeForm}
        onSaved={async () => {
          closeForm();
          await loadRecipes();
          notifySuccess(editing ? 'Receta actualizada' : 'Receta creada');
        }}
        setBusy={setBusy}
        notifyError={notifyError}
      />
    </section>
  );
}
