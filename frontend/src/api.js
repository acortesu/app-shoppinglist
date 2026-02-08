const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';
const API_VERSION = '1';

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

  async createRecipe(payload) {
    return request('/api/recipes', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  async updateRecipe(id, payload) {
    return request(`/api/recipes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  },

  async deleteRecipe(id) {
    return request(`/api/recipes/${id}`, {
      method: 'DELETE'
    });
  },

  async listRecipes() {
    return request('/api/recipes');
  },

  async listPlans() {
    return request('/api/plans');
  },

  async createPlan(payload) {
    return request('/api/plans', {
      method: 'POST',
      body: JSON.stringify(payload)
    });
  },

  async updatePlan(id, payload) {
    return request(`/api/plans/${id}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    });
  },

  async generateShoppingList(planId, idempotencyKey) {
    return request(`/api/shopping-lists/generate?planId=${encodeURIComponent(planId)}`, {
      method: 'POST',
      headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}
    });
  },

  async listShoppingLists() {
    return request('/api/shopping-lists');
  },

  async updateShoppingList(id, items) {
    return request(`/api/shopping-lists/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ items })
    });
  }
};
