import axios from 'axios'

// Thin, single axios instance for all backend calls.
// baseURL defaults to /api/v1 and can be overridden with VITE_API_BASE_URL.
// Interceptors for auth tokens are added in the authentication phase.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export default apiClient