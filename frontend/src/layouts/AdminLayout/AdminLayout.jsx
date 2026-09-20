import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandLogo from '../../components/BrandLogo/BrandLogo.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import './AdminLayout.css'

const NAV_ITEMS = [
  { to: '/admin-panel/applications', label: 'Applications' },
  { to: '/admin-panel/jobs', label: 'Jobs' },
]

/**
 * A18 Stage 1 admin employment console shell — the smallest reusable admin
 * layout: brand header, Stage-1 navigation, signed-in staff identity and
 * sign-out. It reuses the public design system (same tokens, buttons and
 * typography) and deliberately ships no CMS/users/employers surfaces; those
 * remain API-only until their own phases.
 *
 * The layout renders inside <RequireAdmin>; the guard owns authorization
 * UX while backend @PreAuthorize checks remain the security boundary.
 */
export default function AdminLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleSignOut() {
    await logout()
    navigate('/')
  }

  return (
    <div className="admin-portal">
      <header className="admin-portal__header">
        <div className="admin-portal__header-inner">
          <LinkRow />
          <nav className="admin-portal__nav" aria-label="Admin">
            {NAV_ITEMS.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  `admin-portal__nav-link${isActive ? ' admin-portal__nav-link--active' : ''}`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="admin-portal__user">
            <span className="admin-portal__user-email">
              {user ? user.email : ''}
            </span>
            <Button variant="ghost" size="small" onClick={handleSignOut}>
              Sign out
            </Button>
          </div>
        </div>
      </header>
      <main className="admin-portal__main">
        <Outlet />
      </main>
    </div>
  )
}

function LinkRow() {
  return (
    <div className="admin-portal__brand">
      <BrandLogo />
      <span className="admin-portal__brand-label">Admin Console</span>
    </div>
  )
}
