/**
 * Auth token storage (A10).
 *
 * Design (matching the existing backend contract):
 * - Access tokens live in sessionStorage (cleared when the browser tab
 *   closes, limited exposure window; the access token itself expires in
 *   15 minutes server-side).
 * - Refresh tokens live in localStorage (needed to survive tab closure;
 *   rotated server-side on every use with reuse detection + family
 *   revocation, so a leaked copy is quickly invalidated).
 *
 * No password is ever stored. Tokens are sent ONLY to the existing
 * same-origin /api backend via the shared axios client.
 */

const ACCESS_TOKEN_KEY = 'lokmit.accessToken'
const REFRESH_TOKEN_KEY = 'lokmit.refreshToken'

/** In-memory mirror so sessionStorage access stays synchronous-free. */
let memoryAccessToken = null
let hydrated = false

function hydrate() {
  if (hydrated) return
  try {
    memoryAccessToken = sessionStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    memoryAccessToken = null
  }
  hydrated = true
}

export function getAccessToken() {
  hydrate()
  return memoryAccessToken
}

export function setTokens(accessToken, refreshToken) {
  hydrate()
  memoryAccessToken = accessToken || null
  try {
    if (accessToken) {
      sessionStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    } else {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY)
    }
  } catch {
    /* storage unavailable — in-memory still works for this tab */
  }
  try {
    if (refreshToken !== undefined && refreshToken !== null) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    }
  } catch {
    /* storage unavailable — refresh simply cannot persist */
  }
}

export function getRefreshToken() {
  try {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  } catch {
    return null
  }
}

export function clearTokens() {
  hydrate()
  memoryAccessToken = null
  try {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  } catch {
    /* ignore */
  }
  try {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    /* ignore */
  }
}
