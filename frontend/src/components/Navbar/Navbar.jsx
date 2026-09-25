import { useState, useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { primaryPageLinks, loginLinks } from './PageLinks.jsx'
import { company } from '../../constants/siteIdentity.js'
import BrandLogo from '../BrandLogo/BrandLogo.jsx'
import './Navbar.css'

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false)
  const [isSticky, setIsSticky] = useState(false)
  const [showLoginMenu, setShowLoginMenu] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()
  const menuRef = useRef(null)
  const headerRef = useRef(null)
  const loginMenuRef = useRef(null)

  const activeLink = primaryPageLinks.find(link => {
    if (link.to === '/') return location.pathname === '/'
    return location.pathname.startsWith(link.to)
  })?.to

  useEffect(() => {
    const handleScroll = () => {
      setIsSticky(window.scrollY > 24)
    }
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => { document.body.style.overflow = '' }
  }, [isOpen])

  useEffect(() => {
    if (showLoginMenu) {
      const handler = (event) => {
        if (
          loginMenuRef.current &&
          !loginMenuRef.current.contains(event.target) &&
          headerRef.current &&
          !headerRef.current.contains(event.target)
        ) {
          setShowLoginMenu(false)
        }
      }
      document.addEventListener('mousedown', handler)
      return () => document.removeEventListener('mousedown', handler)
    }
  }, [showLoginMenu])

  useEffect(() => {
    if (isOpen) {
      const handler = (event) => {
        if (
          menuRef.current &&
          !menuRef.current.contains(event.target) &&
          headerRef.current &&
          !headerRef.current.contains(event.target)
        ) {
          setIsOpen(false)
        }
      }
      document.addEventListener('mousedown', handler)
      return () => document.removeEventListener('mousedown', handler)
    }
  }, [isOpen])

  // Close menus whenever the route changes
  useEffect(() => {
    setIsOpen(false)
    setShowLoginMenu(false)
  }, [location.pathname])

  const handleNavClick = (to) => {
    navigate(to)
    setIsOpen(false)
    setShowLoginMenu(false)
  }

  return (
    <header ref={headerRef} className={`navbar${isSticky ? ' navbar--sticky' : ''}`}>
      <div className="navbar__container">
        {/* Official brand lockup: horizontal on desktop, compact on mobile.
            Clickable, links to "/". Layout adjusts around the logo — the
            logo asset itself is never modified. */}
        <Link to="/" className="navbar__brand" aria-label="LOKMIT FOUNDATION — Home">
          {/* to={null}: the surrounding <Link> is the single clickable element */}
          <BrandLogo variant="desktop-navbar" to={null} className="navbar__brand-logo--desktop" loading="eager" />
          <BrandLogo variant="mobile-navbar" to={null} className="navbar__brand-logo--mobile" loading="eager" />
          {/* Text fallback for mobile, where the compact logo is a mark only.
              Hidden ≥768px so the horizontal logo is the sole brand element. */}
          <span className="navbar__brand-text">
            <span className="navbar__brand-name">LOKMIT</span>
            <span className="navbar__brand-sub">FOUNDATION</span>
          </span>
        </Link>

        <nav className="navbar__nav" aria-label="Primary navigation">
          <ul className="navbar__list">
            {primaryPageLinks.map(link => (
              <li key={link.to}>
                <Link
                  to={link.to}
                  className={`navbar__link${activeLink === link.to ? ' navbar__link--active' : ''}`}
                  onClick={() => handleNavClick(link.to)}
                  aria-current={activeLink === link.to ? 'page' : undefined}
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="navbar__actions">
          <div className="navbar__login-group">
            <button
              type="button"
              className="navbar__login-btn"
              onClick={() => setShowLoginMenu(!showLoginMenu)}
              aria-expanded={showLoginMenu}
              aria-haspopup="true"
              aria-label="Log in menu"
            >
              <span className="navbar__login-label">Login</span>
              <svg className="navbar__login-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </button>

            {showLoginMenu && (
              <div className="navbar__login-menu" ref={loginMenuRef} role="menu" aria-label="Log in options">
                {loginLinks.map(link => (
                  <Link
                    key={link.to}
                    to={link.to}
                    className="navbar__login-item"
                    onClick={() => handleNavClick(link.to)}
                    role="menuitem"
                  >
                    {link.label}
                  </Link>
                ))}
              </div>
            )}
          </div>

          <Link to="/contact" className="navbar__contact-btn">
            Contact
          </Link>

          <button
            className={`navbar__menu-btn${isOpen ? ' navbar__menu-btn--open' : ''}`}
            onClick={() => setIsOpen(!isOpen)}
            aria-expanded={isOpen}
            aria-controls="mobile-menu"
            aria-label={isOpen ? 'Close menu' : 'Open menu'}
            type="button"
          >
            <span className="navbar__menu-icon" aria-hidden="true">
              {isOpen ? (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              ) : (
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
                  <line x1="3" y1="6" x2="21" y2="6" />
                  <line x1="3" y1="12" x2="21" y2="12" />
                  <line x1="3" y1="18" x2="21" y2="18" />
                </svg>
              )}
            </span>
          </button>
        </div>
      </div>

      {isOpen && (
        <div className="navbar__mobile-menu" id="mobile-menu" ref={menuRef}>
          <nav className="navbar__mobile-nav" aria-label="Mobile navigation">
            <ul className="navbar__mobile-list">
              {primaryPageLinks.map(link => (
                <li key={link.to}>
                  <Link
                    to={link.to}
                    className={`navbar__mobile-link${activeLink === link.to ? ' navbar__mobile-link--active' : ''}`}
                    onClick={() => handleNavClick(link.to)}
                    aria-current={activeLink === link.to ? 'page' : undefined}
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <div className="navbar__mobile-divider" aria-hidden="true" />
            <p className="navbar__mobile-group-label">Portals</p>
            <ul className="navbar__mobile-list">
              {loginLinks.map(link => (
                <li key={link.to}>
                  <Link
                    to={link.to}
                    className="navbar__mobile-link"
                    onClick={() => handleNavClick(link.to)}
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <div className="navbar__mobile-contact">
              <a href={`mailto:${company.officialEmail}`} className="navbar__mobile-contact-link">
                {company.officialEmail}
              </a>
            </div>
          </nav>
        </div>
      )}
    </header>
  )
}
