import { useState } from 'react';
import { Shell } from './components/Shell';
import { AuthGate } from './components/AuthGate';
import { RecipesPage } from './features/recipes/RecipesPage';
import { PlannerPage } from './features/planner/PlannerPage';
import { ShoppingPage } from './features/shopping/ShoppingPage';
import { api } from './api';

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
        <div className="brand-stripe" />
        <main className="screen">
          <AuthGate onLogin={handleLogin} googleClientId={GOOGLE_CLIENT_ID} />
        </main>
        {error && <div className="banner banner-error">{error}</div>}
        {success && <div className="banner banner-success">{success}</div>}
      </div>
    );
  }

  return (
    <Shell
      tab={tab}
      onTabChange={setTab}
      busy={busy}
      error={error}
      success={success}
      onLogout={handleLogout}
      requireAuth={REQUIRE_AUTH}
    >
      <section className={tab === 'recipes' ? '' : 'hidden'}>
        <RecipesPage isActive={tab === 'recipes'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
      </section>
      <section className={tab === 'planner' ? '' : 'hidden'}>
        <PlannerPage isActive={tab === 'planner'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
      </section>
      <section className={tab === 'shopping' ? '' : 'hidden'}>
        <ShoppingPage isActive={tab === 'shopping'} setBusy={setBusy} notifyError={notifyError} notifySuccess={notifySuccess} />
      </section>
    </Shell>
  );
}

export default App;
