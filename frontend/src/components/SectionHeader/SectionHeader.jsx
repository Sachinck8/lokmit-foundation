import './SectionHeader.css'

export default function SectionHeader({ title, subtitle, align = 'left', badge, action }) {
  return (
    <div className={`section-header${align !== 'left' ? ` section-header--${align}` : ''}`}>
      {badge && (
        <span className="section-header__badge">
          {badge}
        </span>
      )}
      <h2 className="section-header__title">{title}</h2>
      {subtitle && (
        <p className="section-header__subtitle">{subtitle}</p>
      )}
      {action && (
        <div className="section-header__action">
          {action}
        </div>
      )}
    </div>
  )
}
