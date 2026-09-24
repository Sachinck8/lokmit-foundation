import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext.jsx'
import Container from '../components/Container/Container.jsx'

/**
 * A18 route guard for the admin console, extended in A21 to be
 * permission-aware for the new console surfaces.
 *
 * - While the session is bootstrapping, shows a neutral loading panel.
 * - Unauthenticated visitors are redirected to /candidate-login with the
 *   intended path preserved (?returnTo=...) — the login page routes
 *   non-candidates straight back to the requested admin path.
 * - Employment routes keep requiring employment:manage; the A21 dashboard
 *   requires dashboard:view and the users page requires users:manage.
 * - Signed-in users without the required permission get an explicit
 *   permission notice — they are NOT silently dumped anywhere.
 *
 * This is UX protection only; every admin API remains secured server-side
 * by its @PreAuthorize permission check.
 */
const PERMISSION_BY_PATH = [
  { prefix: '/admin-panel/applications', permission: 'employment:manage' },
  { prefix: '/admin-panel/jobs', permission: 'employment:manage' },
  { prefix: '/admin-panel/employment', permission: 'employment:manage' },
  { prefix: '/admin-panel/users', permission: 'users:manage' },
  { prefix: '/admin-panel/audit-logs', permission: 'users:manage' },
  { prefix: '/admin-panel/content', permission: 'content:manage' },
  { prefix: '/admin-panel/services', permission: 'services:manage' },
  { prefix: '/admin-panel/expertise', permission: 'services:manage' },
  { prefix: '/admin-panel/projects', permission: 'projects:manage' },
  { prefix: '/admin-panel/messages', permission: 'messages:manage' },
  { prefix: '/admin-panel/notifications', permission: 'notifications:manage' },
  { prefix: '/admin-panel', permission: 'dashboard:view' },
]

function requiredPermission(pathname) {
  const match = PERMISSION_BY_PATH.find(entry => pathname.startsWith(entry.prefix))
  return match ? match.permission : 'employment:manage'
}

const PERMISSION_DESCRIPTIONS = {
  'employment:manage': 'applications review and jobs management',
  'users:manage': 'user management (SUPER_ADMIN)',
  'dashboard:view': 'the admin dashboard',
  'content:manage': 'website content management',
  'services:manage': 'services and expertise management',
  'projects:manage': 'projects management',
  'messages:manage': 'contact message management',
  'notifications:manage': 'your in-app notifications',
}

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
  const permission = requiredPermission(location.pathname)
  if (!permissions.includes(permission)) {
    return (
      <div className="admin-portal">
        <Container>
          <div className="admin-portal__panel">
            <h1 className="admin-portal__title">Admin area</h1>
            <p className="admin-portal__muted">
              You don't have permission to access this area. This page requires
              the <strong>{permission}</strong> permission
              ({PERMISSION_DESCRIPTIONS[permission] || 'the matching admin capability'})
              — you are signed in as{' '}
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
