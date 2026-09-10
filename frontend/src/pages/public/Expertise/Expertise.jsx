import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { expertiseContent } from '../../../constants/expertiseContent.js'
import './Expertise.css'

export default function Expertise() {
  return (
    <div className="expertise-page">
      <PageHero
        title={expertiseContent.hero.title}
        subtitle={expertiseContent.hero.subtitle}
      />

      <section className="section ex__sectors">
        <Container>
          <SectionHeader
            badge="Sectors"
            title="Where We Work"
            subtitle="Domains where we provide technical consultancy, documentation, mentorship, and capacity-building support."
            align="centered"
          />
          <div className="ex__grid">
            {expertiseContent.sectors.map(sector => (
              <article key={sector.title} className="ex__card">
                <h3 className="ex__card-title">{sector.title}</h3>
                <p className="ex__card-desc">{sector.description}</p>
              </article>
            ))}
          </div>
        </Container>
      </section>

      <section className="section section--tinted ex__programs">
        <Container>
          <SectionHeader
            badge="Program Areas"
            title="Supported Programs & Initiatives"
            align="centered"
          />
          <ul className="ex__program-tags">
            {expertiseContent.expertise.map(item => (
              <li key={item} className="ex__program-tag">{item}</li>
            ))}
          </ul>
          <p className="ex__disclaimer">
            <strong>{expertiseContent.disclaimer.label}:</strong> {expertiseContent.disclaimer.text}
          </p>
        </Container>
      </section>

      <section className="section section--dark ex__cta">
        <Container>
          <div className="ex__cta-inner">
            <h2 className="ex__cta-title">{expertiseContent.cta.title}</h2>
            <div className="ex__cta-actions">
              <Link to={expertiseContent.cta.primaryCta.to}>
                <Button variant="secondary" size="large">{expertiseContent.cta.primaryCta.label}</Button>
              </Link>
              <Link to={expertiseContent.cta.secondaryCta.to}>
                <Button variant="dark-outline" size="large">{expertiseContent.cta.secondaryCta.label}</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
