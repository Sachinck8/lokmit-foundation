import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import BrandLogo from '../../components/BrandLogo/BrandLogo.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import './CandidateLayout.css'

const NAV_ITEMS = [
  { to: '/candidate', label: 'Dashboard', end: true },
  { to: '/candidate/profile', label: 'Profile' },
  { to: '/candidate/resumes', label: 'My Resume' },
  { to: '/candidate/applications', label: 'My Applications' },
  { to: '/candidate/interviews', label: 'My Interviews' },
  { to: '/candidate/notifications', label: 'Notifications' },
]

/**
 * A10 candidate portal shell — same brand identity and design system as
 * the public site, with a portal-specific header, sub-navigation and
 * sign-out. Public navbar/footer are intentionally untouched.
 */
export default function CandidateLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  return (
    <div className="candidate-portal">
      <a href="#candidate-main" className="skip-link">Skip to main content</a>

      <header className="candidate-portal__header">
        <div className="candidate-portal__header-inner">
          <NavLink to="/candidate" className="candidate-portal__brand" aria-label="Candidate portal home">
            <BrandLogo variant="mobile-navbar" to={null} className="candidate-portal__brand-logo" loading="eager" />
            <span className="candidate-portal__brand-text">Candidate Portal</span>
          </NavLink>

          <nav className="candidate-portal__nav" aria-label="Candidate portal navigation">
            <ul className="candidate-portal__nav-list">
              {NAV_ITEMS.map(item => (
                <li key={item.to}>
                  <NavLink
                    to={item.to}
                    end={item.end}
                    className={({ isActive }) => `candidate-portal__nav-link${isActive ? ' candidate-portal__nav-link--active' : ''}`}
                  >
                    {item.label}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>

          <div className="candidate-portal__header-actions">
            <span className="candidate-portal__user" title={user && user.email ? user.email : undefined}>
              {user ? user.fullName : ''}
            </span>
            <Button variant="outline" size="small" onClick={() => logout().then(() => navigate('/candidate-login'))}>
              Sign out
            </Button>
          </div>
        </div>
      </header>

      <main className="candidate-portal__main" id="candidate-main">
        <Outlet />
      </main>
    </div>
  )
}
