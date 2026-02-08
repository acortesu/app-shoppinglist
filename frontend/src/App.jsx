import { useEffect, useMemo, useState } from 'react';
import { api } from './api';

const TABS = ['recipes', 'planner', 'shopping'];
const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const PERIODS = ['WEEK', 'FORTNIGHT'];
const UNITS = ['GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON', 'PIECE', 'PINCH', 'TO_TASTE'];
const REQUIRE_AUTH = (import.meta.env.VITE_REQUIRE_AUTH ?? 'true') !== 'false';
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

function isoDate(date) {
  const d = new Date(date);
  const month = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${d.getFullYear()}-${month}-${day}`;
}

function toPrettyDate(iso) {
  return new Date(`${iso}T00:00:00`).toLocaleDateString('es-CR', {
    weekday: 'long',
    day: 'numeric',
    month: 'short'
  });
}

function startOfWeekMonday(date = new Date()) {
  const d = new Date(date);
  const day = d.getDay();
  const delta = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + delta);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date, days) {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

function App() {
  const [tab, setTab] = useState('recipes');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(!REQUIRE_AUTH || api.hasAuthToken());

  const notifyError = (err) => {
    if (err?.code === 'UNAUTHORIZED' && REQUIRE_AUTH) {
      api.setAuthToken('');
      setIsAuthenticated(false);
      setError('Sesión expirada o token inválido. Inicia sesión nuevamente.');
      setTimeout(() => setError(''), 4000);
      return;
    }
    setError(err?.payload?.error || err?.message || 'Error inesperado');
    setTimeout(() => setError(''), 3500);
  };

  const notifySuccess = (msg) => {
    setSuccess(msg);
    setTimeout(() => setSuccess(''), 2500);
  };

  const handleLogin = (token) => {
    api.setAuthToken(token);
    setIsAuthenticated(true);
    notifySuccess('Sesión iniciada');
  };

  const handleLogout = () => {
    api.setAuthToken('');
    setIsAuthenticated(false);
    notifySuccess('Sesión cerrada');
  };

  if (REQUIRE_AUTH && !isAuthenticated) {
    return (
      <div className="mobile-shell">
        <main className="screen">
          <AuthGate onLogin={handleLogin} />
        </main>
        {error && <div className="banner banner-error">{error}</div>}
        {success && <div className="banner banner-success">{success}</div>}
      </div>
    );
  }

  return (
    <div className="mobile-shell">
      <main className="screen">
        {tab === 'recipes' && (
          <RecipesPage setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        )}
        {tab === 'planner' && (
          <PlannerPage setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        )}
        {tab === 'shopping' && (
          <ShoppingPage setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        )}
      </main>

      {error && <div className="banner banner-error">{error}</div>}
      {success && <div className="banner banner-success">{success}</div>}
      {busy && <div className="busy">Cargando...</div>}
      {REQUIRE_AUTH && (
        <button className="logout-chip" onClick={handleLogout}>
          Cerrar sesión
        </button>
      )}

      <nav className="bottom-nav">
        <button className={tab === 'recipes' ? 'active' : ''} onClick={() => setTab('recipes')}>Recetas</button>
        <button className={tab === 'planner' ? 'active' : ''} onClick={() => setTab('planner')}>Planificador</button>
        <button className={tab === 'shopping' ? 'active' : ''} onClick={() => setTab('shopping')}>Compras</button>
      </nav>
    </div>
  );
}

function AuthGate({ onLogin }) {
  const [token, setToken] = useState(localStorage.getItem('appcompras_id_token') || '');
  const [localError, setLocalError] = useState('');
  const [googleReady, setGoogleReady] = useState(false);
  const googleBtnId = 'google-signin-button';

  useEffect(() => {
    if (!GOOGLE_CLIENT_ID) {
      return;
    }

    const initGoogle = () => {
      if (!window.google?.accounts?.id) {
        return;
      }

      window.google.accounts.id.initialize({
        client_id: GOOGLE_CLIENT_ID,
        callback: (response) => {
          if (!response?.credential) {
            setLocalError('Google no devolvió credencial. Intenta nuevamente.');
            return;
          }
          onLogin(response.credential);
        },
        auto_select: false,
        cancel_on_tap_outside: true,
        ux_mode: 'popup'
      });

      const btnContainer = document.getElementById(googleBtnId);
      if (btnContainer) {
        btnContainer.innerHTML = '';
        window.google.accounts.id.renderButton(btnContainer, {
          type: 'standard',
          theme: 'filled_black',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          width: 320
        });
        setGoogleReady(true);
      }
    };

    if (window.google?.accounts?.id) {
      initGoogle();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = initGoogle;
    script.onerror = () => setLocalError('No se pudo cargar Google Sign-In.');
    document.head.appendChild(script);
  }, [onLogin]);

  const submit = (e) => {
    e.preventDefault();
    const cleaned = token.replace(/^Bearer\s+/i, '').trim();
    if (!cleaned) {
      setLocalError('Pega un Google id_token válido.');
      return;
    }
    onLogin(cleaned);
  };

  return (
    <section className="auth-gate">
      <div className="auth-card">
        <h1>Iniciar sesión</h1>
        <p className="muted">Entra con Google para usar el MVP autenticado por usuario.</p>
        {GOOGLE_CLIENT_ID ? (
          <div className="google-zone">
            <div id={googleBtnId} />
            {!googleReady && <p className="muted tiny">Cargando Google Sign-In...</p>}
          </div>
        ) : (
          <p className="auth-error">
            Falta <code>VITE_GOOGLE_CLIENT_ID</code> en <code>frontend/.env</code>.
          </p>
        )}
        <p className="muted tiny">Fallback manual (si lo necesitas): pega aquí el <code>id_token</code>.</p>
        <form onSubmit={submit} className="stack no-pad">
          <textarea
            className="input textarea"
            placeholder="eyJhbGciOiJSUzI1NiIsImtpZCI6..."
            value={token}
            onChange={(e) => {
              setToken(e.target.value);
              if (localError) setLocalError('');
            }}
          />
          {localError && <p className="auth-error">{localError}</p>}
          <button className="btn btn-primary" type="submit">Entrar</button>
        </form>
        <p className="muted tiny">
          Tip: si copiaste <code>Bearer ...</code>, la app lo limpia automáticamente.
        </p>
      </div>
    </section>
  );
}

function RecipesPage({ setBusy, notifyError, notifySuccess }) {
  const [recipes, setRecipes] = useState([]);
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);

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
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    loadRecipes();
  }, []);

  const onDelete = async (id) => {
    try {
      setBusy(true);
      await api.deleteRecipe(id);
      notifySuccess('Receta eliminada');
      await loadRecipes();
    } catch (err) {
      notifyError(err);
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
        {filtered.map((recipe) => (
          <article key={recipe.id} className="card">
            <div className="row between">
              <h3>{recipe.name}</h3>
              <div className="icon-actions">
                <button onClick={() => { setEditing(recipe); setShowForm(true); }}>✎</button>
                <button onClick={() => onDelete(recipe.id)}>🗑</button>
              </div>
            </div>
            <p className="pill">{recipe.type}</p>
            <p>{recipe.ingredients?.length || 0} ingredientes</p>
            <p>{recipe.usageCount || 0} usos</p>
          </article>
        ))}
        {filtered.length === 0 && <p className="muted">No hay recetas todavía.</p>}
      </div>

      {showForm && (
        <RecipeFormModal
          initial={editing}
          onClose={() => setShowForm(false)}
          onSaved={async () => {
            setShowForm(false);
            await loadRecipes();
            notifySuccess(editing ? 'Receta actualizada' : 'Receta creada');
          }}
          setBusy={setBusy}
          notifyError={notifyError}
        />
      )}
    </section>
  );
}

function RecipeFormModal({ initial, onClose, onSaved, setBusy, notifyError }) {
  const [name, setName] = useState(initial?.name || '');
  const [type, setType] = useState(initial?.type || 'LUNCH');
  const [preparation, setPreparation] = useState(initial?.preparation || '');
  const [notes, setNotes] = useState(initial?.notes || '');
  const [tags, setTags] = useState(initial?.tags?.join(', ') || '');
  const [ingredients, setIngredients] = useState(
    initial?.ingredients?.length
      ? initial.ingredients.map((it) => ({
          ingredientId: it.ingredientId || '',
          quantity: String(it.quantity ?? ''),
          unit: it.unit || 'GRAM',
          query: '',
          options: []
        }))
      : [{ ingredientId: '', quantity: '', unit: 'GRAM', query: '', options: [] }]
  );

  const updateIngredient = (idx, patch) => {
    setIngredients((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)));
  };

  const searchIngredients = async (idx, q) => {
    updateIngredient(idx, { query: q });
    if (q.trim().length < 2) {
      updateIngredient(idx, { options: [] });
      return;
    }
    try {
      const options = await api.listIngredients(q.trim());
      updateIngredient(idx, { options: options || [] });
    } catch {
      updateIngredient(idx, { options: [] });
    }
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      name,
      type,
      preparation: preparation || null,
      notes: notes || null,
      tags: tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
      ingredients: ingredients
        .filter((it) => it.ingredientId && it.quantity)
        .map((it) => ({ ingredientId: it.ingredientId, quantity: Number(it.quantity), unit: it.unit }))
    };

    try {
      setBusy(true);
      if (initial?.id) {
        await api.updateRecipe(initial.id, payload);
      } else {
        await api.createRecipe(payload);
      }
      await onSaved();
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="modal-backdrop">
      <form className="modal" onSubmit={onSubmit}>
        <div className="row between"><h2>{initial ? 'Editar Receta' : 'Nueva Receta'}</h2><button type="button" onClick={onClose}>✕</button></div>
        <label>Nombre</label>
        <input className="input" value={name} onChange={(e) => setName(e.target.value)} required />

        <label>Tipo</label>
        <select className="input" value={type} onChange={(e) => setType(e.target.value)}>
          {MEAL_TYPES.map((m) => <option key={m} value={m}>{m}</option>)}
        </select>

        <label>Ingredientes</label>
        {ingredients.map((it, idx) => (
          <div key={idx} className="ingredient-box">
            <input
              className="input"
              placeholder="Nombre del ingrediente"
              value={it.query || it.ingredientId}
              onChange={(e) => searchIngredients(idx, e.target.value)}
            />
            {it.options?.length > 0 && (
              <div className="suggestions">
                {it.options.slice(0, 6).map((opt) => (
                  <button
                    type="button"
                    key={opt.id}
                    className="suggestion"
                    onClick={() => updateIngredient(idx, { ingredientId: opt.id, query: opt.name, options: [] })}
                  >
                    {opt.name}
                  </button>
                ))}
              </div>
            )}
            <div className="row">
              <input
                className="input"
                placeholder="Cantidad"
                type="number"
                min="0.01"
                step="0.01"
                value={it.quantity}
                onChange={(e) => updateIngredient(idx, { quantity: e.target.value })}
                required
              />
              <select className="input" value={it.unit} onChange={(e) => updateIngredient(idx, { unit: e.target.value })}>
                {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
              </select>
            </div>
          </div>
        ))}
        <button type="button" className="btn" onClick={() => setIngredients((prev) => [...prev, { ingredientId: '', quantity: '', unit: 'GRAM', query: '', options: [] }])}>+ Agregar ingrediente</button>

        <label>Instrucciones</label>
        <textarea className="input textarea" value={preparation} onChange={(e) => setPreparation(e.target.value)} />
        <label>Notas</label>
        <textarea className="input textarea" value={notes} onChange={(e) => setNotes(e.target.value)} />
        <label>Tags (separados por coma)</label>
        <input className="input" value={tags} onChange={(e) => setTags(e.target.value)} />

        <button className="btn btn-primary" type="submit">{initial ? 'Guardar cambios' : 'Crear receta'}</button>
      </form>
    </div>
  );
}

function PlannerPage({ setBusy, notifyError, notifySuccess }) {
  const [period, setPeriod] = useState('WEEK');
  const [startDate, setStartDate] = useState(isoDate(startOfWeekMonday()));
  const [recipes, setRecipes] = useState([]);
  const [plans, setPlans] = useState([]);
  const [slots, setSlots] = useState({});
  const [planId, setPlanId] = useState('');

  const dayCount = period === 'WEEK' ? 7 : 14;
  const days = useMemo(
    () => Array.from({ length: dayCount }, (_, i) => isoDate(addDays(new Date(`${startDate}T00:00:00`), i))),
    [startDate, period]
  );

  const recipeNameById = useMemo(() => Object.fromEntries(recipes.map((r) => [r.id, r.name])), [recipes]);

  const load = async () => {
    try {
      setBusy(true);
      const [recipeData, planData] = await Promise.all([api.listRecipes(), api.listPlans()]);
      setRecipes(recipeData || []);
      setPlans(planData || []);
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    const current = plans.find((p) => p.startDate === startDate && p.period === period);
    if (!current) {
      setPlanId('');
      setSlots({});
      return;
    }
    setPlanId(current.id);
    const map = {};
    (current.slots || []).forEach((s) => {
      map[`${s.date}|${s.mealType}`] = s.recipeId;
    });
    setSlots(map);
  }, [plans, startDate, period]);

  const setSlot = (date, mealType, recipeId) => {
    const key = `${date}|${mealType}`;
    setSlots((prev) => ({ ...prev, [key]: recipeId || '' }));
  };

  const savePlan = async () => {
    const payloadSlots = [];
    days.forEach((d) => {
      MEAL_TYPES.forEach((mealType) => {
        const recipeId = slots[`${d}|${mealType}`];
        if (recipeId) payloadSlots.push({ date: d, mealType, recipeId });
      });
    });

    try {
      setBusy(true);
      const payload = { startDate, period, slots: payloadSlots };
      if (planId) {
        await api.updatePlan(planId, payload);
      } else {
        await api.createPlan(payload);
      }
      await load();
      notifySuccess('Plan guardado');
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <section>
      <header className="top-header">
        <h1>Planificador</h1>
        <div className="row">
          {PERIODS.map((p) => (
            <button key={p} className={`pill-btn ${period === p ? 'active' : ''}`} onClick={() => setPeriod(p)}>{p === 'WEEK' ? 'Semanal' : 'Quincenal'}</button>
          ))}
        </div>
        <div className="row between">
          <button className="btn" onClick={() => setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), -dayCount)))}>←</button>
          <strong>{toPrettyDate(startDate)} - {toPrettyDate(days[days.length - 1])}</strong>
          <button className="btn" onClick={() => setStartDate(isoDate(addDays(new Date(`${startDate}T00:00:00`), dayCount)))}>→</button>
        </div>
        <button className="link-btn" onClick={() => setStartDate(isoDate(startOfWeekMonday()))}>Ir a hoy</button>
      </header>

      <div className="stack">
        {days.map((date) => (
          <article key={date} className="card">
            <h3 className="capitalize">{toPrettyDate(date)}</h3>
            {MEAL_TYPES.map((mealType) => {
              const key = `${date}|${mealType}`;
              const recipeId = slots[key] || '';
              return (
                <div key={key} className="row between slot-row">
                  <div>
                    <div className="muted">{mealType}</div>
                    <div>{recipeId ? recipeNameById[recipeId] : 'Sin planificar'}</div>
                  </div>
                  <select className="slot-select" value={recipeId} onChange={(e) => setSlot(date, mealType, e.target.value)}>
                    <option value="">+</option>
                    {recipes.map((r) => (
                      <option key={r.id} value={r.id}>{r.name}</option>
                    ))}
                  </select>
                </div>
              );
            })}
          </article>
        ))}
      </div>

      <button className="btn btn-primary sticky-cta" onClick={savePlan}>Guardar plan</button>
    </section>
  );
}

function ShoppingPage({ setBusy, notifyError, notifySuccess }) {
  const [plans, setPlans] = useState([]);
  const [draft, setDraft] = useState(null);
  const [selectedPlanId, setSelectedPlanId] = useState('');

  const load = async () => {
    try {
      setBusy(true);
      const [allPlans, allDrafts] = await Promise.all([api.listPlans(), api.listShoppingLists()]);
      setPlans(allPlans || []);
      const latest = allDrafts?.[0] || null;
      setDraft(latest);
      setSelectedPlanId(latest?.planId || allPlans?.[0]?.id || '');
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const regenerate = async () => {
    if (!selectedPlanId) {
      notifyError(new Error('Selecciona un plan para generar lista'));
      return;
    }

    try {
      setBusy(true);
      const created = await api.generateShoppingList(selectedPlanId, `gen-${Date.now()}`);
      setDraft(created);
      notifySuccess('Lista generada');
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  const updateItem = (id, patch) => {
    setDraft((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        items: prev.items.map((it) => (it.id === id ? { ...it, ...patch } : it))
      };
    });
  };

  const removeItem = (id) => {
    setDraft((prev) => {
      if (!prev) return prev;
      return { ...prev, items: prev.items.filter((it) => it.id !== id) };
    });
  };

  const addManual = () => {
    setDraft((prev) => {
      const base = prev || { id: '', planId: selectedPlanId || null, items: [] };
      const nextIndex = base.items.length;
      return {
        ...base,
        items: [
          ...base.items,
          {
            id: `tmp-${Date.now()}-${nextIndex}`,
            ingredientId: null,
            name: 'Nuevo item',
            quantity: 1,
            unit: 'PIECE',
            suggestedPackages: null,
            packageAmount: null,
            packageUnit: null,
            manual: true,
            bought: false,
            note: '',
            sortOrder: nextIndex
          }
        ]
      };
    });
  };

  const saveDraft = async () => {
    if (!draft?.id) {
      notifyError(new Error('Primero genera una lista desde un plan'));
      return;
    }

    const items = (draft.items || []).map((it, idx) => ({
      id: it.id,
      ingredientId: it.ingredientId,
      name: it.name,
      quantity: Number(it.quantity),
      unit: it.unit,
      suggestedPackages: it.suggestedPackages,
      packageAmount: it.packageAmount,
      packageUnit: it.packageUnit,
      manual: Boolean(it.manual),
      bought: Boolean(it.bought),
      note: it.note || null,
      sortOrder: idx
    }));

    try {
      setBusy(true);
      const updated = await api.updateShoppingList(draft.id, items);
      setDraft(updated);
      notifySuccess('Cambios guardados');
    } catch (err) {
      notifyError(err);
    } finally {
      setBusy(false);
    }
  };

  const unbought = (draft?.items || []).filter((it) => !it.bought).sort((a, b) => a.sortOrder - b.sortOrder);
  const bought = (draft?.items || []).filter((it) => it.bought).sort((a, b) => a.sortOrder - b.sortOrder);

  return (
    <section>
      <header className="top-header">
        <h1>Lista de Compras</h1>
        <div className="row">
          <select className="input" value={selectedPlanId} onChange={(e) => setSelectedPlanId(e.target.value)}>
            <option value="">Seleccionar plan</option>
            {plans.map((p) => (
              <option key={p.id} value={p.id}>{p.startDate} ({p.period})</option>
            ))}
          </select>
        </div>
        <div className="row">
          <button className="btn grow" onClick={regenerate}>↻ Regenerar desde plan</button>
          <button className="btn btn-primary" onClick={addManual}>+</button>
        </div>
      </header>

      <div className="stack">
        <h4 className="section-title">POR COMPRAR ({unbought.length})</h4>
        {unbought.map((item) => (
          <ShoppingItem key={item.id} item={item} onChange={updateItem} onDelete={removeItem} />
        ))}

        <h4 className="section-title">COMPRADO ({bought.length})</h4>
        {bought.map((item) => (
          <ShoppingItem key={item.id} item={item} onChange={updateItem} onDelete={removeItem} />
        ))}

        {!draft && <p className="muted">Genera una lista desde un plan para empezar.</p>}
      </div>

      <button className="btn btn-primary sticky-cta" onClick={saveDraft}>Guardar cambios</button>
    </section>
  );
}

function ShoppingItem({ item, onChange, onDelete }) {
  return (
    <article className="card">
      <div className="row between">
        <label className="row gap-sm">
          <input type="checkbox" checked={!!item.bought} onChange={(e) => onChange(item.id, { bought: e.target.checked })} />
          <input className="inline-name" value={item.name} onChange={(e) => onChange(item.id, { name: e.target.value })} />
        </label>
        <button onClick={() => onDelete(item.id)}>🗑</button>
      </div>
      <div className="row gap-sm">
        <input className="input" type="number" min="0.01" step="0.01" value={item.quantity} onChange={(e) => onChange(item.id, { quantity: e.target.value })} />
        <select className="input" value={item.unit} onChange={(e) => onChange(item.id, { unit: e.target.value })}>
          {UNITS.map((u) => <option key={u} value={u}>{u}</option>)}
        </select>
      </div>
      <input className="input" placeholder="Nota" value={item.note || ''} onChange={(e) => onChange(item.id, { note: e.target.value })} />
    </article>
  );
}

export default App;
