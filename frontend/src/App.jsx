import { useEffect, useMemo, useState } from 'react';
import { api } from './api';

const TABS = ['recipes', 'planner', 'shopping'];
const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER'];
const PERIODS = ['WEEK', 'FORTNIGHT'];
const UNITS = ['GRAM', 'KILOGRAM', 'MILLILITER', 'LITER', 'CUP', 'TABLESPOON', 'TEASPOON', 'PIECE', 'PINCH', 'TO_TASTE'];
const REQUIRE_AUTH = (import.meta.env.VITE_REQUIRE_AUTH ?? 'true') !== 'false';
const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
const TOKEN_STORAGE_KEY = 'appcompras_id_token';

const API_ERROR_MESSAGES = {
  global: {
    VALIDATION_ERROR: 'Revisa los datos del formulario.',
    INVALID_TYPE: 'Formato inválido en uno o más campos.',
    RESOURCE_NOT_FOUND: 'No se encontró el recurso solicitado.',
    FORBIDDEN: 'No tienes permisos para esta acción.',
    UNSUPPORTED_API_VERSION: 'Versión de API inválida. Reintenta.',
    INTERNAL_ERROR: 'Error interno del servidor. Intenta otra vez.'
  },
  recipe_form: {
    INGREDIENT_NOT_FOUND: 'Ingrediente no encontrado en catálogo.',
    INVALID_INGREDIENT_UNIT: 'Unidad inválida para ese ingrediente.',
    VALIDATION_ERROR: 'Completa los campos obligatorios de la receta.'
  },
  planner: {
    PLAN_RECIPE_NOT_FOUND: 'La receta del slot no existe o no es tuya.',
    PLAN_SLOT_OUT_OF_RANGE: 'Hay slots fuera del rango de fechas del plan.',
    PLAN_DUPLICATE_SLOT: 'Hay slots duplicados para fecha y comida.'
  },
  shopping: {
    SHOPPING_ITEM_INGREDIENT_REQUIRED: 'Ítem inválido: falta ingredientId para item no manual.',
    SHOPPING_ITEM_PACKAGE_FIELDS_INCOMPLETE: 'Completa suggestedPackages, packageAmount y packageUnit juntos.',
    SHOPPING_ITEM_INVALID_SUGGESTED_PACKAGES: 'suggestedPackages debe ser mayor a 0.',
    SHOPPING_ITEM_INVALID_PACKAGE_AMOUNT: 'packageAmount debe ser mayor a 0.',
    SHOPPING_ITEM_NOTE_TOO_LONG: 'La nota supera el máximo permitido.',
    SHOPPING_ITEM_INVALID_SORT_ORDER: 'sortOrder inválido.',
    RESOURCE_NOT_FOUND: 'Draft de compra no encontrado.'
  },
  auth: {
    UNAUTHORIZED: 'Sesión no válida. Inicia sesión nuevamente.',
    FORBIDDEN: 'Tu usuario no tiene permisos para este recurso.'
  }
};

function decodeJwtPayload(token) {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const normalized = payload.padEnd(payload.length + ((4 - payload.length % 4) % 4), '=');
    return JSON.parse(atob(normalized));
  } catch {
    return null;
  }
}

function validateIdToken(token) {
  const cleaned = token.replace(/^Bearer\s+/i, '').trim();
  const payload = decodeJwtPayload(cleaned);
  if (!payload) {
    return { ok: false, message: 'El token no tiene formato JWT válido.' };
  }

  const nowSec = Math.floor(Date.now() / 1000);
  if (!payload.exp || payload.exp <= nowSec + 30) {
    return { ok: false, message: 'El token está expirado o por expirar.' };
  }

  if (GOOGLE_CLIENT_ID && payload.aud !== GOOGLE_CLIENT_ID) {
    return { ok: false, message: 'El token no coincide con el Google Client ID configurado.' };
  }

  if (payload.iss && payload.iss !== 'https://accounts.google.com' && payload.iss !== 'accounts.google.com') {
    return { ok: false, message: 'Issuer inválido para token de Google.' };
  }

  return { ok: true, token: cleaned, payload };
}

function readValidSessionToken() {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (!token) return '';
  const validation = validateIdToken(token);
  if (!validation.ok) {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    return '';
  }
  return validation.token;
}

function mapApiError(err, context = 'global') {
  const code = err?.code || 'UNKNOWN_ERROR';
  const payloadMessage = err?.payload?.error;
  const byContext = API_ERROR_MESSAGES[context]?.[code];
  const byGlobal = API_ERROR_MESSAGES.global[code];
  return byContext || byGlobal || payloadMessage || err?.message || 'Error inesperado';
}

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
  const [isAuthenticated, setIsAuthenticated] = useState(!REQUIRE_AUTH || Boolean(readValidSessionToken()));

  const notifyError = (err, context = 'global') => {
    if (err?.code === 'UNAUTHORIZED' && REQUIRE_AUTH) {
      api.setAuthToken('');
      setIsAuthenticated(false);
      setError(mapApiError(err, 'auth'));
      setTimeout(() => setError(''), 4000);
      return;
    }
    setError(mapApiError(err, context));
    setTimeout(() => setError(''), 3500);
  };

  const notifySuccess = (msg) => {
    setSuccess(msg);
    setTimeout(() => setSuccess(''), 2500);
  };

  const handleLogin = (token) => {
    const validation = validateIdToken(token);
    if (!validation.ok) {
      setError(validation.message);
      setTimeout(() => setError(''), 4000);
      return;
    }
    api.setAuthToken(validation.token);
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
        <section className={tab === 'recipes' ? '' : 'hidden'}>
          <RecipesPage isActive={tab === 'recipes'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        </section>
        <section className={tab === 'planner' ? '' : 'hidden'}>
          <PlannerPage isActive={tab === 'planner'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        </section>
        <section className={tab === 'shopping' ? '' : 'hidden'}>
          <ShoppingPage isActive={tab === 'shopping'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
        </section>
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
        ux_mode: 'popup',
        itp_support: true
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

      // One Tap / FedCM availability can vary by browser policies.
      // We keep the classic Google button as the primary path and treat
      // One Tap prompt status as non-blocking signal only.
      window.google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          // Non-blocking: do not surface as error if button sign-in still works.
          console.debug('Google One Tap not active:', {
            notDisplayed: notification.isNotDisplayed?.(),
            skipped: notification.isSkippedMoment?.(),
            reason: notification.getNotDisplayedReason?.() || notification.getSkippedReason?.()
          });
        }
      });
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
        {localError && <p className="auth-error">{localError}</p>}
      </div>
    </section>
  );
}

function RecipesPage({ isActive, setBusy, notifyError, notifySuccess }) {
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
      notifyError(err, 'recipes');
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
          options: [],
          allowedUnits: null
        }))
      : [{ ingredientId: '', quantity: '', unit: 'GRAM', query: '', options: [], allowedUnits: null }]
  );
  const [formError, setFormError] = useState('');
  const [invalidIngredientIndexes, setInvalidIngredientIndexes] = useState([]);

  const updateIngredient = (idx, patch) => {
    if (formError) setFormError('');
    if (invalidIngredientIndexes.length) setInvalidIngredientIndexes([]);
    setIngredients((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)));
  };

  const searchIngredients = async (idx, q) => {
    updateIngredient(idx, { query: q, ingredientId: '', allowedUnits: null });
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
    setFormError('');
    setInvalidIngredientIndexes([]);

    const normalizedIngredients = ingredients.map((it) => {
      const identifier = (it.ingredientId || it.query || '').trim();
      return {
        ingredientId: identifier,
        quantity: Number(it.quantity),
        unit: it.unit,
        allowedUnits: it.allowedUnits
      };
    });

    const missingIdx = normalizedIngredients
      .map((it, idx) => (!it.ingredientId || !Number.isFinite(it.quantity) || it.quantity <= 0 ? idx : -1))
      .filter((idx) => idx >= 0);

    if (missingIdx.length > 0) {
      setInvalidIngredientIndexes(missingIdx);
      setFormError('Revisa los ingredientes: nombre y cantidad son obligatorios.');
      return;
    }

    const invalidUnitIdx = normalizedIngredients
      .map((it, idx) => (Array.isArray(it.allowedUnits) && it.allowedUnits.length > 0 && !it.allowedUnits.includes(it.unit) ? idx : -1))
      .filter((idx) => idx >= 0);

    if (invalidUnitIdx.length > 0) {
      setInvalidIngredientIndexes(invalidUnitIdx);
      setFormError('La unidad seleccionada no está permitida para uno o más ingredientes.');
      return;
    }

    const payload = {
      name,
      type,
      preparation: preparation || null,
      notes: notes || null,
      tags: tags
        .split(',')
        .map((t) => t.trim())
        .filter(Boolean),
      ingredients: normalizedIngredients.map(({ ingredientId, quantity, unit }) => ({ ingredientId, quantity, unit }))
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
      const backendMessage = err?.payload?.error || err?.message || '';
      if (err?.code === 'INGREDIENT_NOT_FOUND') {
        const match = backendMessage.match(/Unknown ingredient:\s*([^\.]+)/i);
        const unknown = match?.[1]?.trim().toLowerCase();
        const idx = ingredients.findIndex((it) => {
          const candidate = (it.ingredientId || it.query || '').trim().toLowerCase();
          return candidate && candidate === unknown;
        });
        setInvalidIngredientIndexes(idx >= 0 ? [idx] : []);
        setFormError('Ingrediente no encontrado. Selecciónalo desde catálogo o créalo como custom.');
      } else if (err?.code === 'INVALID_INGREDIENT_UNIT') {
        setFormError('La unidad no es válida para uno de los ingredientes seleccionados.');
      } else if (err?.code === 'VALIDATION_ERROR') {
        setFormError('Completa los campos obligatorios de la receta.');
      } else {
        notifyError(err, 'recipe_form');
      }
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
          <div key={idx} className={`ingredient-box ${invalidIngredientIndexes.includes(idx) ? 'ingredient-box-invalid' : ''}`}>
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
                    onClick={() => {
                      const allowedUnits = (opt.allowedUnits || []).map(String);
                      const nextUnit = allowedUnits.includes(it.unit) ? it.unit : (allowedUnits[0] || 'GRAM');
                      updateIngredient(idx, {
                        ingredientId: opt.id,
                        query: opt.name,
                        unit: nextUnit,
                        allowedUnits,
                        options: []
                      });
                    }}
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
                {(Array.isArray(it.allowedUnits) && it.allowedUnits.length > 0 ? it.allowedUnits : UNITS).map((u) => <option key={u} value={u}>{u}</option>)}
              </select>
            </div>
          </div>
        ))}
        <button type="button" className="btn" onClick={() => setIngredients((prev) => [...prev, { ingredientId: '', quantity: '', unit: 'GRAM', query: '', options: [], allowedUnits: null }])}>+ Agregar ingrediente</button>
        {formError && <p className="auth-error">{formError}</p>}

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

function PlannerPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [period, setPeriod] = useState('WEEK');
  const [startDate, setStartDate] = useState(isoDate(startOfWeekMonday()));
  const [recipes, setRecipes] = useState([]);
  const [plans, setPlans] = useState([]);
  const [slots, setSlots] = useState({});
  const [planId, setPlanId] = useState('');
  const [plannerNotice, setPlannerNotice] = useState('');
  const [selectedDay, setSelectedDay] = useState('');
  const [pickerSlot, setPickerSlot] = useState(null);
  const [recipeQuery, setRecipeQuery] = useState('');

  const dayCount = period === 'WEEK' ? 7 : 14;
  const days = useMemo(
    () => Array.from({ length: dayCount }, (_, i) => isoDate(addDays(new Date(`${startDate}T00:00:00`), i))),
    [startDate, period]
  );

  const recipeNameById = useMemo(() => Object.fromEntries(recipes.map((r) => [r.id, r.name])), [recipes]);
  const validRecipeIds = useMemo(() => new Set(recipes.map((r) => r.id)), [recipes]);
  const selectedDayRecipes = useMemo(() => {
    const q = recipeQuery.trim().toLowerCase();
    if (!q) return recipes;
    return recipes.filter((r) => r.name.toLowerCase().includes(q));
  }, [recipes, recipeQuery]);

  const load = async () => {
    try {
      setBusy(true);
      const [recipeData, planData] = await Promise.all([api.listRecipes(), api.listPlans()]);
      setRecipes(recipeData || []);
      setPlans(planData || []);
    } catch (err) {
      notifyError(err, 'planner');
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
      setPlannerNotice('');
      return;
    }
    setPlanId(current.id);
    const map = {};
    let dropped = 0;
    (current.slots || []).forEach((s) => {
      if (validRecipeIds.has(s.recipeId)) {
        map[`${s.date}|${s.mealType}`] = s.recipeId;
      } else {
        dropped += 1;
      }
    });
    setSlots(map);
    setPlannerNotice(
      dropped > 0
        ? 'Se limpiaron slots con recetas eliminadas. Revisa y guarda el plan.'
        : ''
    );
  }, [plans, startDate, period, validRecipeIds]);

  useEffect(() => {
    if (!days.length) return;
    if (!selectedDay || !days.includes(selectedDay)) {
      setSelectedDay(days[0]);
    }
  }, [days, selectedDay]);

  const setSlot = (date, mealType, recipeId) => {
    const key = `${date}|${mealType}`;
    setSlots((prev) => ({ ...prev, [key]: recipeId || '' }));
  };

  const clearSlot = (date, mealType) => {
    setSlot(date, mealType, '');
  };

  const savePlan = async () => {
    const payloadSlots = [];
    days.forEach((d) => {
      MEAL_TYPES.forEach((mealType) => {
        const recipeId = slots[`${d}|${mealType}`];
        if (recipeId && validRecipeIds.has(recipeId)) {
          payloadSlots.push({ date: d, mealType, recipeId });
        }
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
      notifyError(err, 'planner');
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
        {plannerNotice && <p className="notice-text">{plannerNotice}</p>}
      </header>

      <div className="stack">
        <div className="day-scroller">
          {days.map((date) => (
            <button
              key={date}
              className={`day-chip ${selectedDay === date ? 'active' : ''}`}
              onClick={() => setSelectedDay(date)}
            >
              {toPrettyDate(date)}
            </button>
          ))}
        </div>
        {selectedDay && (
          <article key={selectedDay} className="card">
            <h3 className="capitalize">{toPrettyDate(selectedDay)}</h3>
            {MEAL_TYPES.map((mealType) => {
              const key = `${selectedDay}|${mealType}`;
              const recipeId = slots[key] || '';
              const hasValidRecipe = recipeId && recipeNameById[recipeId];
              return (
                <div key={key} className="row between slot-row">
                  <div>
                    <div className="muted">{mealType}</div>
                    <div>{hasValidRecipe ? recipeNameById[recipeId] : 'Sin planificar'}</div>
                  </div>
                  <div className="row">
                    <button
                      className="btn slot-picker-btn"
                      onClick={() => {
                        setPickerSlot({ date: selectedDay, mealType });
                        setRecipeQuery('');
                      }}
                    >
                      {hasValidRecipe ? 'Cambiar' : '+'}
                    </button>
                    {hasValidRecipe && (
                      <button className="btn slot-clear-btn" onClick={() => clearSlot(selectedDay, mealType)}>×</button>
                    )}
                  </div>
                </div>
              );
            })}
          </article>
        )}
      </div>

      <button className="btn btn-primary sticky-cta" onClick={savePlan}>Guardar plan</button>
      {pickerSlot && (
        <div className="modal-backdrop">
          <div className="modal">
            <div className="row between">
              <h2>Seleccionar receta</h2>
              <button type="button" onClick={() => setPickerSlot(null)}>✕</button>
            </div>
            <input
              className="input"
              placeholder="Buscar receta..."
              value={recipeQuery}
              onChange={(e) => setRecipeQuery(e.target.value)}
            />
            <div className="stack no-pad">
              {selectedDayRecipes.map((r) => (
                <button
                  key={r.id}
                  className="btn recipe-picker-item"
                  onClick={() => {
                    setSlot(pickerSlot.date, pickerSlot.mealType, r.id);
                    setPickerSlot(null);
                  }}
                >
                  {r.name}
                </button>
              ))}
              {selectedDayRecipes.length === 0 && <p className="muted">No hay recetas para ese filtro.</p>}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function ShoppingPage({ isActive, setBusy, notifyError, notifySuccess }) {
  const [plans, setPlans] = useState([]);
  const [draft, setDraft] = useState(null);
  const [selectedPlanId, setSelectedPlanId] = useState('');
  const [validPlanIds, setValidPlanIds] = useState(new Set());
  const [shoppingNotice, setShoppingNotice] = useState('');
  const [recipesById, setRecipesById] = useState({});

  const load = async () => {
    try {
      setBusy(true);
      const [allPlans, allDrafts, allRecipes] = await Promise.all([
        api.listPlans(),
        api.listShoppingLists(),
        api.listRecipes()
      ]);
      const recipeIds = new Set((allRecipes || []).map((r) => r.id));
      setRecipesById(Object.fromEntries((allRecipes || []).map((r) => [r.id, r])));
      const nextValidPlanIds = new Set(
        (allPlans || [])
          .filter((p) => (p.slots || []).every((s) => recipeIds.has(s.recipeId)))
          .map((p) => p.id)
      );

      setPlans(allPlans || []);
      setValidPlanIds(nextValidPlanIds);

      const latestValidDraft = (allDrafts || []).find((d) => nextValidPlanIds.has(d.planId)) || null;
      const firstValidPlanId = (allPlans || []).find((p) => nextValidPlanIds.has(p.id))?.id || '';
      const nextSelectedPlanId = latestValidDraft?.planId || firstValidPlanId || '';

      setDraft(latestValidDraft);
      setSelectedPlanId(nextSelectedPlanId);
      setShoppingNotice(
        nextValidPlanIds.size < (allPlans || []).length
          ? 'Hay planes con recetas eliminadas. Solo se muestran planes válidos para generar lista.'
          : ''
      );
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const regenerate = async () => {
    if (!selectedPlanId) {
      notifyError(new Error('Selecciona un plan para generar lista'), 'shopping');
      return;
    }
    if (!validPlanIds.has(selectedPlanId)) {
      notifyError(new Error('El plan seleccionado tiene recetas eliminadas. Corrige el plan primero.'), 'shopping');
      return;
    }

    try {
      setBusy(true);
      const created = await api.generateShoppingList(selectedPlanId, `gen-${Date.now()}`);
      setDraft(created);
      notifySuccess('Lista generada');
    } catch (err) {
      notifyError(err, 'shopping');
    } finally {
      setBusy(false);
    }
  };

  const cleanPlanAndRegenerate = async () => {
    if (!selectedPlanId) {
      notifyError(new Error('Selecciona un plan primero.'), 'shopping');
      return;
    }
    const plan = plans.find((p) => p.id === selectedPlanId);
    if (!plan) {
      notifyError(new Error('Plan no encontrado en estado local.'), 'shopping');
      return;
    }

    const cleanedSlots = (plan.slots || []).filter((slot) => Boolean(recipesById[slot.recipeId]));
    const removed = (plan.slots || []).length - cleanedSlots.length;

    try {
      setBusy(true);
      if (removed > 0) {
        await api.updatePlan(plan.id, {
          startDate: plan.startDate,
          period: plan.period,
          slots: cleanedSlots
        });
      }
      const created = await api.generateShoppingList(plan.id, `clean-gen-${Date.now()}`);
      setDraft(created);
      notifySuccess(
        removed > 0
          ? `Plan limpiado (${removed} slot${removed > 1 ? 's' : ''}) y lista regenerada`
          : 'Lista regenerada desde plan limpio'
      );
      await load();
    } catch (err) {
      notifyError(err, 'shopping');
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

  const moveItem = (id, direction) => {
    setDraft((prev) => {
      if (!prev?.items?.length) return prev;
      const sorted = [...prev.items].sort((a, b) => a.sortOrder - b.sortOrder);
      const idx = sorted.findIndex((it) => it.id === id);
      if (idx < 0) return prev;
      const nextIdx = idx + direction;
      if (nextIdx < 0 || nextIdx >= sorted.length) return prev;
      const [moved] = sorted.splice(idx, 1);
      sorted.splice(nextIdx, 0, moved);
      return {
        ...prev,
        items: sorted.map((it, order) => ({ ...it, sortOrder: order }))
      };
    });
  };

  const bulkSetBought = (value) => {
    setDraft((prev) => {
      if (!prev) return prev;
      return {
        ...prev,
        items: prev.items.map((it) => ({ ...it, bought: value }))
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
      notifyError(new Error('Primero genera una lista desde un plan'), 'shopping');
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
      notifyError(err, 'shopping');
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
            {plans.filter((p) => validPlanIds.has(p.id)).map((p) => (
              <option key={p.id} value={p.id}>{p.startDate} ({p.period})</option>
            ))}
          </select>
        </div>
        {shoppingNotice && <p className="notice-text">{shoppingNotice}</p>}
        <div className="row">
          <button className="btn grow" onClick={regenerate}>↻ Regenerar desde plan</button>
          <button className="btn grow" onClick={cleanPlanAndRegenerate}>🧹 Limpiar + regenerar</button>
          <button className="btn btn-primary" onClick={addManual}>+</button>
        </div>
        <div className="row">
          <button className="btn grow" onClick={() => bulkSetBought(false)}>Marcar todo por comprar</button>
          <button className="btn grow" onClick={() => bulkSetBought(true)}>Marcar todo comprado</button>
        </div>
      </header>

      <div className="stack">
        <h4 className="section-title">POR COMPRAR ({unbought.length})</h4>
        {unbought.map((item) => (
          <ShoppingItem key={item.id} item={item} onChange={updateItem} onDelete={removeItem} onMove={moveItem} />
        ))}

        <h4 className="section-title">COMPRADO ({bought.length})</h4>
        {bought.map((item) => (
          <ShoppingItem key={item.id} item={item} onChange={updateItem} onDelete={removeItem} onMove={moveItem} />
        ))}

        {!draft && <p className="muted">Genera una lista desde un plan para empezar.</p>}
      </div>

      <button className="btn btn-primary sticky-cta" onClick={saveDraft}>Guardar cambios</button>
    </section>
  );
}

function ShoppingItem({ item, onChange, onDelete, onMove }) {
  return (
    <article className="card">
      <div className="row between">
        <label className="row gap-sm">
          <input type="checkbox" checked={!!item.bought} onChange={(e) => onChange(item.id, { bought: e.target.checked })} />
          <input className="inline-name" value={item.name} onChange={(e) => onChange(item.id, { name: e.target.value })} />
        </label>
        <div className="row">
          <button onClick={() => onMove(item.id, -1)}>↑</button>
          <button onClick={() => onMove(item.id, 1)}>↓</button>
          <button onClick={() => onDelete(item.id)}>🗑</button>
        </div>
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
