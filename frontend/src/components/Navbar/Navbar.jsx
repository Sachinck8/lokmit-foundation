import { useState, useEffect, useRef } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { primaryPageLinks, loginLinks } from './PageLinks.jsx'
import { company } from '../../constants/siteIdentity.js'
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
      setIsSticky(window.scrollY > 40)
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

  const handleNavClick = (to) => {
    navigate(to)
    setIsOpen(false)
    setShowLoginMenu(false)
  }

  return (
    <header ref={headerRef} className={`navbar${isSticky ? ' navbar--sticky' : ''}`}>
      <div className="navbar__container">
        <Link to="/" className="navbar__brand" onClick={() => setIsOpen(false)}>
          <span className="navbar__brand-icon" aria-hidden="true">
            <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M20 4L36 12V28L20 36L4 28V12L20 4Z" fill="currentColor" opacity="0.2" />
              <path d="M20 12L28 16V24L20 28L12 24V16L20 12Z" fill="currentColor" />
              <circle cx="20" cy="20" r="3" fill="currentColor" opacity="0.6" />
            </svg>
          </span>
          <span className="navbar__brand-text">LOKMIT FOUNDATION</span>
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
              <span className="navbar__login-label">Log In</span>
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

          <a href={`mailto:${company.officialEmail}`} className="navbar__contact-btn">
            Contact
          </a>

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
                    autoFocus
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
            <div className="navbar__mobile-divider" aria-hidden="true" />
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
