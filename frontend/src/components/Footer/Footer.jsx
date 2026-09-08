import { Link } from 'react-router-dom'
import { site, company } from '../../constants/siteIdentity.js'
import './Footer.css'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer__wrapper">
        <div className="footer__grid">
          <div className="footer__brand">
            <Link to="/" className="footer__logo" aria-label="LOKMIT FOUNDATION home">
              <span className="footer__logo-mark" aria-hidden="true">
                <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M20 4L36 12V28L20 36L4 28V12L20 4Z" fill="currentColor" opacity="0.2" />
                  <path d="M20 12L28 16V24L20 28L12 24V16L20 12Z" fill="currentColor" />
                  <circle cx="20" cy="20" r="3" fill="currentColor" opacity="0.6" />
                </svg>
              </span>
              <span className="footer__logo-text">LOKMIT FOUNDATION</span>
            </Link>
            <p className="footer__tagline">{site.tagline}</p>
            <address className="footer__address">
              <p>{company.registeredOffice.lines.join(' ')}</p>
              <p>Email: <a href={`mailto:${company.officialEmail}`}>{company.officialEmail}</a></p>
            </address>
          </div>

          <div className="footer__nav-group">
            <h2 className="footer__nav-heading">Pages</h2>
            <ul className="footer__nav-list">
              {site.footerNav.pages.map(link => (
                <li key={link.to}>
                  <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="footer__nav-group">
            <h2 className="footer__nav-heading">Legal</h2>
            <ul className="footer__nav-list">
              {site.footerNav.legal.map(link => (
                <li key={link.to}>
                  <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                </li>
              ))}
            </ul>
          </div>

          <div className="footer__nav-group">
            <h2 className="footer__nav-heading">Contact</h2>
            <ul className="footer__nav-list">
              {site.footerNav.contact.map(link =>
                link.href ? (
                  <li key={link.href}>
                    <a href={link.href} className="footer__nav-link">{link.label}</a>
                  </li>
                ) : (
                  <li key={link.to}>
                    <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                  </li>
                )
              )}
            </ul>
          </div>
        </div>

        <div className="footer__bottom">
          <div className="footer__bottom-inner">
            <p className="footer__legal-status">
              {company.legalStatus}. CIN: {company.cin}. ROC: {company.roc}. Date of Incorporation: {company.incorporationDate}.
            </p>
            <p className="footer__copyright">
              &copy; {new Date().getFullYear()} {company.name}. All rights reserved.
            </p>
          </div>
        </div>
      </div>
    </footer>
  )
}
