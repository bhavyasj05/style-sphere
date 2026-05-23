async function request(path, options = {}) {
  const response = await fetch(path, {
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options
  })
  const body = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(body.message || 'Request failed.')
  }
  return body
}

export const api = {
  products: (params = '') => request(`/api/products${params}`),
  product: (id) => request(`/api/products/${id}`),
  cart: () => request('/api/cart'),
  addCart: (productId, quantity = 1) => request('/api/cart', { method: 'POST', body: JSON.stringify({ productId, quantity }) }),
  updateCart: (productId, quantity) => request('/api/cart', { method: 'PUT', body: JSON.stringify({ productId, quantity }) }),
  removeCart: (productId) => request(`/api/cart/${productId}`, { method: 'DELETE' }),
  me: () => request('/api/auth/me'),
  login: (payload) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) }),
  register: (payload) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) }),
  logout: () => request('/api/auth/logout', { method: 'POST' }),
  profile: () => request('/api/profile'),
  saveProfile: (payload) => request('/api/profile', { method: 'PUT', body: JSON.stringify(payload) }),
  orders: () => request('/api/orders'),
  checkout: (payload) => request('/api/orders', { method: 'POST', body: JSON.stringify(payload) }),
  verifyPayment: (payload) => request('/api/payments/verify', { method: 'POST', body: JSON.stringify(payload) }),
  failPayment: (payload) => request('/api/payments/failure', { method: 'POST', body: JSON.stringify(payload) }),
  adminSummary: () => request('/api/admin/summary'),
  adminProducts: () => request('/api/admin/products'),
  createProduct: (payload) => request('/api/admin/products', { method: 'POST', body: JSON.stringify(payload) }),
  updateProduct: (id, payload) => request(`/api/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteProduct: (id) => request(`/api/admin/products/${id}`, { method: 'DELETE' })
}
