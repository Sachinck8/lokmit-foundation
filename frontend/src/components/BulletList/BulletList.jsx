import './BulletList.css'

export default function BulletList({ items, title, description }) {
  return (
    <div className="bullet-list">
      {title && <h3 className="bullet-list__title">{title}</h3>}
      {description && <p className="bullet-list__description">{description}</p>}
      <ul className="bullet-list__items">
        {items.map((item, index) => (
          <li key={index} className="bullet-list__item">
            <svg className="bullet-list__icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}
