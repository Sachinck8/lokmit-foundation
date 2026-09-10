import './PageHero.css'

/**
 * Standard page hero used by every public page.
 * Dark by default for strong hierarchy; supports badge, subtitle,
 * left/center/right alignment and CTA actions.
 */
export default function PageHero({ title, subtitle, badge, background, align = 'center', actions = null, children = null }) {
  const alignment = align === 'left' || align === 'right' ? align : 'center'
  const isCentered = alignment === 'center'

  return (
    <section
      className={`page-hero page-hero--${alignment}`}
      style={background ? { background } : undefined}
    >
      <div className={`page-hero__inner${isCentered ? ' page-hero__inner--centered' : ''}`}>
        {badge && (
          <span className="page-hero__badge">{badge}</span>
        )}
        <h1 className="page-hero__title">{title}</h1>
        {subtitle && (
          <p className="page-hero__subtitle">{subtitle}</p>
        )}
        {actions && (
          <div className="page-hero__actions">{actions}</div>
        )}
        {children}
      </div>
    </section>
  )
}
