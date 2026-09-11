import { brand, company } from '../../constants/siteIdentity.js'
import './BrandLogo.css'

/**
 * Single reusable branding component for every official LOKMIT FOUNDATION
 * logo placement. All official assets live in `frontend/public/brand/` and
 * are never redrawn, recolored, or modified — only displayed as supplied.
 *
 * Variants (context → official asset):
 *  - "desktop-navbar" → logo-horizontal.png (transparent lockup, light bar)
 *  - "mobile-navbar"  → logo-compact.png (opaque compact square logo)
 *  - "footer"         → logo-white.png (reverse logo for the dark footer)
 *  - "symbol"         → logo-symbol.png (small supporting mark; never huge)
 *  - "primary"        → logo-primary.png (general light-background usage)
 *
 * Aspect ratio is always preserved; no CSS filters, crops, or recolors.
 * Every variant is optionally rendered inside a link (default "/").
 */
const VARIANTS = {
  'desktop-navbar': { src: brand.horizontal, className: 'brand-logo--desktop-navbar' },
  'mobile-navbar': { src: brand.compact, className: 'brand-logo--mobile-navbar' },
  footer: { src: brand.white, className: 'brand-logo--footer' },
  symbol: { src: brand.symbol, className: 'brand-logo--symbol' },
  primary: { src: brand.primary, className: 'brand-logo--primary' },
}

export default function BrandLogo({
  variant = 'primary',
  alt = `${company.name} logo`,
  to = '/',
  className = '',
  loading,
}) {
  const config = VARIANTS[variant] ?? VARIANTS.primary
  const img = (
    <img
      src={config.src}
      alt={alt}
      className={`brand-logo ${config.className}${className ? ` ${className}` : ''}`}
      loading={loading}
    />
  )

  if (!to) return img
  return (
    <a href={to} className="brand-logo-link" aria-label={`${company.name} — Home`}>
      {img}
    </a>
  )
}
