const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const API_VERSION = '1';
const CACHE_TTL_MS = 15_000;
const cache = new Map();

function getAuthToken() {
  return localStorage.getItem('appcompras_id_token') || '';
}

function buildHeaders(extra = {}) {
  const headers = {
    'Content-Type': 'application/json',
    'X-API-Version': API_VERSION,
    ...extra
  };

  const token = getAuthToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: buildHeaders(options.headers)
  });

  if (response.status === 204) {
    return null;
  }

  const bodyText = await response.text();
  const body = bodyText ? JSON.parse(bodyText) : null;

  if (!response.ok) {
    const apiCode = body?.code || 'HTTP_ERROR';
    const message = body?.error || `HTTP ${response.status}`;
    const error = new Error(message);
    error.code = apiCode;
    error.status = response.status;
    error.payload = body;
    throw error;
  }

  return body;
}

function readCache(key) {
  const entry = cache.get(key);
  if (!entry) return null;
  if (Date.now() > entry.expiresAt) {
    cache.delete(key);
    return null;
  }
  return entry.value;
}

function writeCache(key, value) {
  cache.set(key, { value, expiresAt: Date.now() + CACHE_TTL_MS });
}

function invalidateCache(keys) {
  keys.forEach((k) => cache.delete(k));
}

export const api = {
  baseUrl: API_BASE_URL,
  hasAuthToken() {
    return Boolean(getAuthToken());
  },

  setAuthToken(token) {
    if (!token) {
      localStorage.removeItem('appcompras_id_token');
      return;
    }
    localStorage.setItem('appcompras_id_token', token.trim());
  },

  async listIngredients(q) {
    const query = q ? `?q=${encodeURIComponent(q)}` : '';
    return request(`/api/ingredients${query}`);
  },

  async createCustomIngredient(payload) {
    return request('/api/ingredients/custom', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  async createRecipe(payload) {
    const response = await request('/api/recipes', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    invalidateCache(['recipes', 'plans', 'shopping']);
    return response;
  },

  async updateRecipe(id, payload) {
    const response = await request(`/api/recipes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    invalidateCache(['recipes', 'plans', 'shopping']);
    return response;
  },

  async deleteRecipe(id) {
    const response = await request(`/api/recipes/${id}`, {
      method: 'DELETE'
    });
    invalidateCache(['recipes', 'plans', 'shopping']);
    return response;
  },

  async listRecipes() {
    const cached = readCache('recipes');
    if (cached) return cached;
    const response = await request('/api/recipes');
    writeCache('recipes', response || []);
    return response;
  },

  async listPlans() {
    const cached = readCache('plans');
    if (cached) return cached;
    const response = await request('/api/plans');
    writeCache('plans', response || []);
    return response;
  },

  async createPlan(payload) {
    const response = await request('/api/plans', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
    invalidateCache(['plans', 'shopping']);
    return response;
  },

  async updatePlan(id, payload) {
    const response = await request(`/api/plans/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
    invalidateCache(['plans', 'shopping']);
    return response;
  },

  async generateShoppingList(planId, idempotencyKey) {
    const response = await request(`/api/shopping-lists/generate?planId=${encodeURIComponent(planId)}`, {
      method: 'POST',
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
    });
    invalidateCache(['shopping']);
    return response;
  },

  async listShoppingLists() {
    const cached = readCache('shopping');
    if (cached) return cached;
    const response = await request('/api/shopping-lists');
    writeCache('shopping', response || []);
    return response;
  },

  async updateShoppingList(id, items) {
    const response = await request(`/api/shopping-lists/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ items })
    });
    invalidateCache(['shopping']);
    return response;
  },

  clearCache() {
    cache.clear();
  }
};
