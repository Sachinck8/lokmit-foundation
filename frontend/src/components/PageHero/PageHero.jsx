export default function PageHero({ title, subtitle, badge, background, align = 'center' }) {
  return (
    <section
      className="page-hero"
      style={{ background }}
    >
      {badge && (
        <span className="page-hero__badge" aria-label="Page category">
          {badge}
        </span>
      )}
      <h1 className="page-hero__title">{title}</h1>
      {subtitle && (
        <p className="page-hero__subtitle">{subtitle}</p>
      )}
    </section>
  )
}
