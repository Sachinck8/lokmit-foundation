import { useCallback, useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import BrandLogo from '../../components/BrandLogo/BrandLogo.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import { countMyUnreadNotifications, markMyNotificationRead } from '../../services/candidateService.js'
import './CandidateLayout.css'

const NAV_ITEMS = [
  { to: '/candidate', label: 'Dashboard', end: true },
  { to: '/candidate/profile', label: 'Profile' },
  { to: '/candidate/resumes', label: 'My Resume' },
  { to: '/candidate/applications', label: 'My Applications' },
  { to: '/candidate/interviews', label: 'My Interviews' },
  { to: '/candidate/notifications', label: 'Notifications' },
]

const NOTIFICATIONS_ROUTE = '/candidate/notifications'

function badgeLabel(count) {
  return count > 99 ? '99+' : String(count)
}

/**
 * A10 candidate portal shell — same brand identity and design system as
 * the public site, with a portal-specific header, sub-navigation and
 * sign-out. Public navbar/footer are intentionally untouched.
 *
 * A16 surfaces the unread-notification count as a badge on the
 * Notifications nav item (reusing the existing A14 unread-count endpoint)
 * and shares a mark-read callback through the outlet context so the
 * notifications page can refresh the badge without a page reload.
 */
export default function CandidateLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [unreadCount, setUnreadCount] = useState(null)

  useEffect(() => {
    let active = true
    countMyUnreadNotifications()
      .then(count => {
        if (active && typeof count === 'number') setUnreadCount(count)
      })
      .catch(() => {
        // Badge is a convenience signal - a failed request must never
        // break the portal, so the count simply stays hidden.
        if (active) setUnreadCount(null)
      })
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    // Re-check when the candidate returns to the notifications page so the
    // badge reflects notifications read since the layout last loaded.
    if (location.pathname === NOTIFICATIONS_ROUTE) {
      let active = true
      countMyUnreadNotifications()
        .then(count => {
          if (active && typeof count === 'number') setUnreadCount(count)
        })
        .catch(() => {})
      return () => {
        active = false
      }
    }
    return undefined
  }, [location.pathname])

  /**
   * Lets the notifications page mark an item read while keeping the nav
   * badge in sync; resolves with the mark-read response DTO. Callers handle
   * rejections (the page shows its existing error state).
   */
  const handleNotificationRead = useCallback(notificationId => {
    if (typeof notificationId !== 'number') return Promise.resolve()
    return markMyNotificationRead(notificationId).then(updated => {
      countMyUnreadNotifications()
        .then(count => {
          if (typeof count === 'number') setUnreadCount(count)
        })
        .catch(() => {})
      return updated
    })
  }, [])

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
                    {item.to === NOTIFICATIONS_ROUTE && unreadCount !== null && unreadCount > 0 && (
                      <span
                        className="candidate-portal__nav-badge"
                        aria-label={`${unreadCount} unread notification${unreadCount === 1 ? '' : 's'}`}
                      >
                        {badgeLabel(unreadCount)}
                      </span>
                    )}
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
        <Outlet context={{ handleNotificationRead }} />
      </main>
    </div>
  )
}
