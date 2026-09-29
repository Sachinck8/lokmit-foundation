import { Link } from 'react-router-dom'
import Button from '../Button/Button.jsx'
import './CtaSection.css'

/**
 * Full-width premium CTA band.
 * Renders real route links for primaryCta/secondaryCta ({ label, to }).
 */
export default function CtaSection({ title, subtitle, primaryCta, secondaryCta, eyebrow, align = 'center' }) {
  const isCentered = align === 'center'

  return (
    <section className="cta-section">
      <div className={`cta-section__content${isCentered ? ' cta-section__content--centered' : ''}`}>
        {eyebrow && <p className="cta-section__eyebrow">{eyebrow}</p>}
        <h2 className="cta-section__title">{title}</h2>
        {subtitle && (
          <p className="cta-section__subtitle">{subtitle}</p>
        )}
        {(primaryCta || secondaryCta) && (
          <div className="cta-section__actions">
            {primaryCta && (
              <Link to={primaryCta.to}>
                <Button variant="secondary" size="large">
                  {primaryCta.label}
                </Button>
              </Link>
            )}
            {secondaryCta && (
              <Link to={secondaryCta.to}>
                <Button variant="dark-outline" size="large">
                  {secondaryCta.label}
                </Button>
              </Link>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
