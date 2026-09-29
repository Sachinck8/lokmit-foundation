import './SectionHeader.css'

/**
 * Consistent section heading with optional eyebrow badge,
 * subtitle and trailing action.
 */
export default function SectionHeader({ title, subtitle, align = 'left', badge, action, as = 'h2' }) {
  const Heading = as
  return (
    <div className={`section-header${align !== 'left' ? ` section-header--${align}` : ''}`}>
      {badge && (
        <span className="section-header__badge">{badge}</span>
      )}
      <Heading className="section-header__title">{title}</Heading>
      {subtitle && (
        <p className="section-header__subtitle">{subtitle}</p>
      )}
      {action && (
        <div className="section-header__action">{action}</div>
      )}
    </div>
  )
}
