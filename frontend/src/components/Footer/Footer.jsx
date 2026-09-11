import { Link } from 'react-router-dom'
import { site, company } from '../../constants/siteIdentity.js'
import BrandLogo from '../BrandLogo/BrandLogo.jsx'
import './Footer.css'

export default function Footer() {
  return (
    <footer className="footer">
      <div className="footer__wrapper">
        <div className="footer__grid">
          <div className="footer__brand">
            {/* Primary footer brand: official reverse logo (dark background),
                rendered prominently — it contains the full wordmark, so no
                duplicate text block is rendered. The small symbol below is a
                subtle supporting mark, never a replacement for the main logo
                and never oversized. */}
            <Link to="/" className="footer__logo" aria-label="LOKMIT FOUNDATION home">
              <BrandLogo variant="footer" to={null} className="footer__logo-img" loading="lazy" />
            </Link>
            <span className="footer__brand-symbol" aria-hidden="true">
              <BrandLogo variant="symbol" to={null} className="footer__symbol-img" loading="lazy" />
            </span>
            <p className="footer__descriptor">
              Professional Technical Consultancy &amp; Skill Development
            </p>
            <p className="footer__slogan">Empowering Lives. Building Brighter Futures.</p>
            <address className="footer__address">
              <p>{company.registeredOffice.lines.join(' ')}</p>
              <p>
                Email: <a href={`mailto:${company.officialEmail}`}>{company.officialEmail}</a>
              </p>
            </address>
          </div>

          <nav className="footer__nav-group" aria-label="Footer — Company">
            <h2 className="footer__nav-heading">Company</h2>
            <ul className="footer__nav-list">
              {site.footerNav.pages.slice(0, 8).map(link => (
                <li key={link.to}>
                  <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                </li>
              ))}
            </ul>
          </nav>

          <nav className="footer__nav-group" aria-label="Footer — Explore">
            <h2 className="footer__nav-heading">Services</h2>
            <ul className="footer__nav-list">
              <li><Link to="/services" className="footer__nav-link">All Services</Link></li>
              <li><Link to="/expertise" className="footer__nav-link">Expertise</Link></li>
              <li><Link to="/projects" className="footer__nav-link">Projects</Link></li>
              <li><Link to="/case-studies" className="footer__nav-link">Case Studies</Link></li>
              <li><Link to="/impact" className="footer__nav-link">Impact</Link></li>
              <li><Link to="/clients" className="footer__nav-link">Clients &amp; Partners</Link></li>
              <li><Link to="/careers" className="footer__nav-link">Careers</Link></li>
              <li><Link to="/jobs" className="footer__nav-link">Job Portal</Link></li>
            </ul>
          </nav>

          <nav className="footer__nav-group" aria-label="Footer — Portals and resources">
            <h2 className="footer__nav-heading">Portals</h2>
            <ul className="footer__nav-list">
              {site.loginNav.map(link => (
                <li key={link.to}>
                  <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                </li>
              ))}
              <li><Link to="/downloads" className="footer__nav-link">Downloads</Link></li>
              <li><Link to="/news" className="footer__nav-link">News</Link></li>
              <li><Link to="/gallery" className="footer__nav-link">Gallery</Link></li>
              <li><Link to="/events" className="footer__nav-link">Events</Link></li>
              <li><Link to="/faq" className="footer__nav-link">FAQ</Link></li>
            </ul>
          </nav>

          <nav className="footer__nav-group" aria-label="Footer — Legal">
            <h2 className="footer__nav-heading">Legal</h2>
            <ul className="footer__nav-list">
              {site.footerNav.legal.map(link => (
                <li key={link.to}>
                  <Link to={link.to} className="footer__nav-link">{link.label}</Link>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <div className="footer__bottom">
          <p className="footer__legal-status">
            {company.legalStatus}. CIN: {company.cin}. ROC: {company.roc}. Date of Incorporation: {company.incorporationDate}. PAN: {company.pan}. TAN: {company.tan}.
          </p>
          <p className="footer__copyright">
            &copy; {new Date().getFullYear()} {company.name}. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  )
}
