import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Container from '../components/Container/Container.jsx'

/**
 * A18 Stage 1 route guard for the admin employment console.
 *
 * - While the session is bootstrapping, shows a neutral loading panel.
 * - Unauthenticated visitors are redirected to /candidate-login with the
 *   intended path preserved (?returnTo=...) — the login page routes
 *   non-candidates straight back to the requested admin path.
 * - Signed-in users WITHOUT the employment:manage permission (e.g.
 *   candidates) get an explicit permission notice — they are NOT silently
 *   dumped anywhere.
 *
 * This is UX protection only; every admin API remains secured server-side
 * by its @PreAuthorize permission check.
 */
const REQUIRED_PERMISSION = 'employment:manage'

export default function RequireAdmin({ children }) {
  const { user, initializing, isCandidate } = useAuth()
  const location = useLocation()

  if (initializing) {
    return (
      <div className="admin-portal">
        <Container>
          <div className="admin-portal__panel" role="status" aria-live="polite">
            <p className="admin-portal__muted">Checking your session…</p>
          </div>
        </Container>
      </div>
    )
  }

  if (!user) {
    const returnTo = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/candidate-login?returnTo=${returnTo}`} replace />
  }

  const permissions = Array.isArray(user.permissions) ? user.permissions : []
  if (!permissions.includes(REQUIRED_PERMISSION)) {
    return (
      <div className="admin-portal">
        <Container>
          <div className="admin-portal__panel">
            <h1 className="admin-portal__title">Admin area</h1>
            <p className="admin-portal__muted">
              You don't have permission to access this area. The applications
              review console requires the <strong>employment:manage</strong>{' '}
              permission — you are signed in as{' '}
              <strong>{user.email || 'an account without staff access'}</strong>
              {isCandidate ? ' (candidate account)' : ''}.
            </p>
            <p className="admin-portal__muted">
              If you believe you should have staff access, please contact the
              foundation office.
            </p>
          </div>
        </Container>
      </div>
    )
  }

  return children
}
