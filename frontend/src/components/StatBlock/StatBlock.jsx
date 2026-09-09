import './StatBlock.css'

export default function StatBlock({ items, title, subtitle, compact = false }) {
  return (
    <div className={`stat-block${compact ? ' stat-block--compact' : ''}`}>
      {title && (
        <h2 className="stat-block__title">{title}</h2>
      )}
      {subtitle && (
        <p className="stat-block__subtitle">{subtitle}</p>
      )}
      <ul className="stat-block__list">
        {items.map((item, index) => (
          <li key={index} className="stat-block__item">
            <span className="stat-block__value">{item.value}</span>
            <span className="stat-block__label">{item.label}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
