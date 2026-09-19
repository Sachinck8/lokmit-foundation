import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'
import { setTokens, clearTokens, getRefreshToken } from './tokenStorage.js'

const AuthContext = createContext(null)

/**
 * A10 authentication context.
 *
 * - On mount: if a refresh token exists, calls POST /auth/refresh to
 *   exchange it for a fresh access token + user info (server-verified
 *   identity, never a cached snapshot). On failure the stale refresh
 *   token is discarded and the session starts signed-out.
 * - login(): POST /auth/login, stores the token pair, fetches /auth/me
 *   for the verified user info.
 * - logout(): POST /auth/logout (revokes the refresh token server-side),
 *   clears local tokens, navigates to /candidate-login.
 * - isCandidate: UX-only role gate; the backend remains the security
 *   boundary for every candidate API.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [initializing, setInitializing] = useState(true)
  const navigate = useNavigate()

  useEffect(() => {
    let active = true
    async function bootstrap() {
      if (!getRefreshToken()) {
        setInitializing(false)
        return
      }
      try {
        const response = await apiClient.post(API_ENDPOINTS.AUTH_REFRESH, {
          refreshToken: getRefreshToken(),
        })
        const payload = response.data && response.data.data
        if (!active || !payload || !payload.accessToken) return
        setTokens(payload.accessToken, payload.refreshToken)
        const me = await apiClient.get(API_ENDPOINTS.AUTH_ME)
        if (active) setUser(me.data && me.data.data)
      } catch {
        if (active) {
          clearTokens()
          setUser(null)
        }
      } finally {
        if (active) setInitializing(false)
      }
    }
    bootstrap()
    return () => {
      active = false
    }
  }, [])

  const value = useMemo(() => ({
    user,
    initializing,
    isCandidate:
      !!user && Array.isArray(user.roles) && user.roles.includes('ROLE_CANDIDATE'),

    async login(email, password) {
      const response = await apiClient.post(API_ENDPOINTS.AUTH_LOGIN, {
        email,
        password,
      })
      const payload = response.data && response.data.data
      if (!payload || !payload.accessToken) {
        throw new Error('Unexpected login response from the server.')
      }
      setTokens(payload.accessToken, payload.refreshToken)
      const me = await apiClient.get(API_ENDPOINTS.AUTH_ME)
      const meUser = me.data && me.data.data
      setUser(meUser)
      return meUser
    },

    async logout() {
      const refreshToken = getRefreshToken()
      try {
        if (refreshToken) {
          await apiClient.post(API_ENDPOINTS.AUTH_LOGOUT, { refreshToken })
        }
      } catch {
        // Server-side revocation is best-effort; local cleanup always runs.
      } finally {
        clearTokens()
        setUser(null)
        navigate('/candidate-login')
      }
    },
  }), [user, initializing, navigate])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
