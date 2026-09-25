import axios from 'axios'
import { getAccessToken, setTokens, getRefreshToken, clearTokens } from '../auth/tokenStorage.js'

// Thin, single axios instance for all backend calls.
// baseURL defaults to /api/v1 and can be overridden with VITE_API_BASE_URL.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// --- request: attach the bearer token when present ---------------------
apiClient.interceptors.request.use(config => {
  const token = getAccessToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// --- response: refresh once on 401, then replay the request -----------
let refreshPromise = null

function beginRefresh() {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      refreshPromise = Promise.reject(new Error('no refresh token'))
    } else {
      // A bare axios instance: no interceptors, no retry loop.
      refreshPromise = axios
        .post(
          `${apiClient.defaults.baseURL}/auth/refresh`,
          { refreshToken },
          { headers: { 'Content-Type': 'application/json' } },
        )
        .then(response => {
          const payload = response.data && response.data.data
          if (!payload || !payload.accessToken) {
            throw new Error('Malformed refresh response')
          }
          setTokens(payload.accessToken, payload.refreshToken)
          return payload.accessToken
        })
        .catch(error => {
          // Rotation reuse detection / expiry / lockout → hard logout.
          // clearTokens notifies the auth context so the UI does not keep
          // showing an authenticated shell over a dead session (A24).
          clearTokens()
          throw error
        })
        .finally(() => {
          refreshPromise = null
        })
    }
  }
  return refreshPromise
}

apiClient.interceptors.response.use(
  response => response,
  async error => {
    const status = error && error.response && error.response.status
    const config = error && error.config
    const url = config && typeof config.url === 'string' ? config.url : ''
    const isAuthCall = url.includes('/auth/login')
        || url.includes('/auth/refresh')
        || url.includes('/auth/logout')

    if (status === 401 && config && !config._retried && !isAuthCall) {
      config._retried = true
      try {
        await beginRefresh()
        const token = getAccessToken()
        if (token) {
          config.headers = config.headers || {}
          config.headers.Authorization = `Bearer ${token}`
        }
        return apiClient.request(config)
      } catch {
        // fall through to the original 401
      }
    }
    return Promise.reject(error)
  },
)

export default apiClient
