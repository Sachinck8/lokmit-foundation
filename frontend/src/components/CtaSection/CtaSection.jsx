import { Link } from 'react-router-dom'
import Button from '../Button/Button.jsx'

export default function CtaSection({ background, title, subtitle, primaryCta, secondaryCta, align = 'center' }) {
  return (
    <section
      className={`cta-section${background ? ' cta-section--dark' : ''}`}
      style={background ? { background } : undefined}
    >
      <div className="cta-section__content" style={{ textAlign: align }}>
        <h2 className="cta-section__title">{title}</h2>
        {subtitle && (
          <p className="cta-section__subtitle">{subtitle}</p>
        )}
        <div className="cta-section__actions" style={{ justifyContent: align === 'center' ? 'center' : 'flex-start' }}>
          {primaryCta && (
            <Link to={primaryCta.to}>
              <Button variant="primary" size="large">
                {primaryCta.label}
              </Button>
            </Link>
          )}
          {secondaryCta && (
            <Link to={secondaryCta.to}>
              <Button variant="outline" size="large" className="cta-section__secondary">
                {secondaryCta.label}
              </Button>
            </Link>
          )}
        </div>
      </div>
    </section>
  )
}
