import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Container from '../components/Container/Container.jsx'

/**
 * A10 route guard for the candidate portal.
 *
 * - While the session is bootstrapping, shows a neutral loading panel
 *   (no route decisions on incomplete state).
 * - Unauthenticated visitors are redirected to /candidate-login with the
 *   intended path preserved (?returnTo=...).
 * - Authenticated non-candidates (admin/employer/client staff) get an
 *   explicit permission notice — they are NOT silently dumped to login.
 *
 * Frontend checks are UX only; every candidate API remains protected by
 * backend ownership resolution.
 */
export default function RequireCandidate({ children }) {
  const { user, initializing, isCandidate } = useAuth()
  const location = useLocation()

  if (initializing) {
    return (
      <div className="candidate-portal">
        <Container>
          <div className="candidate-portal__panel" role="status" aria-live="polite">
            <p className="candidate-portal__muted">Checking your session…</p>
          </div>
        </Container>
      </div>
    )
  }

  if (!user) {
    const returnTo = encodeURIComponent(location.pathname + location.search)
    return <Navigate to={`/candidate-login?returnTo=${returnTo}`} replace />
  }

  if (!isCandidate) {
    return (
      <div className="candidate-portal">
        <Container>
          <div className="candidate-portal__panel">
            <h1 className="candidate-portal__title">Candidate area</h1>
            <p className="candidate-portal__muted">
              You don't have permission to access this area. This portal is for
              candidate accounts — you are signed in as{' '}
              <strong>{user.userType || 'a different account type'}</strong>.
            </p>
            <p className="candidate-portal__muted">
              If you believe you should have candidate access, please contact
              the foundation office.
            </p>
          </div>
        </Container>
      </div>
    )
  }

  return children
}
