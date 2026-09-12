import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { servicesContent } from '../../../constants/servicesContent.js'
import './Services.css'

const valueOutcomes = {
  'Project Approval Consultancy': 'Stronger, approval-ready applications with complete, quality documentation.',
  'Technical Mentorship': 'Confident day-to-day execution with expert guidance at every project stage.',
  'Professional Consultancy': 'Standardized systems and policies that keep your organization audit-ready.',
}

export default function Services() {
  const { categories, expertise } = servicesContent

  return (
    <div className="services-page">
      <PageHero
        title={servicesContent.hero.title}
        subtitle={servicesContent.hero.subtitle}
      />

      <section className="section sv__section">
        <Container>
          <SectionHeader
            badge="Service Architecture"
            title="Three Practice Areas. One Standard of Quality."
            subtitle="Each engagement combines clear deliverables, practical guidance, and compliance-ready documentation."
            align="centered"
          />

          <div className="sv__grid">
            {categories.map((category, index) => (
              <article key={category.title} className="sv__card">
                <div className="sv__card-head">
                  <span className="sv__card-num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                  <h3 className="sv__card-title">{category.title}</h3>
                </div>
                <p className="sv__card-desc">{category.description}</p>
                <ul className="sv__card-items">
                  {category.items.map(item => (
                    <li key={item} className="sv__card-item">
                      <svg className="sv__check" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                      {item}
                    </li>
                  ))}
                </ul>
                <div className="sv__card-outcome">
                  <span className="sv__outcome-label">Outcome</span>
                  <p className="sv__outcome-text">{valueOutcomes[category.title]}</p>
                </div>
              </article>
            ))}
          </div>
        </Container>
      </section>

      <section className="section section--tinted sv__expertise">
        <Container>
          <SectionHeader
            title={expertise.title}
            subtitle={expertise.subtitle}
            align="centered"
          />
          <ul className="sv__expertise-tags">
            {expertise.items.map(item => (
              <li key={item.name} className="sv__expertise-tag">{item.name}</li>
            ))}
          </ul>
          <p className="sv__disclaimer">{expertise.note}</p>
        </Container>
      </section>

      <section className="section section--dark sv__cta">
        <Container>
          <div className="sv__cta-inner">
            <h2 className="sv__cta-title">Ready to Discuss Your Project?</h2>
            <p className="sv__cta-subtitle">
              Whether you need technical consultancy, documentation support, compliance guidance, or skill development assistance, we are ready to help.
            </p>
            <div className="sv__cta-actions">
              <Link to="/contact">
                <Button variant="secondary" size="large">Get in Touch</Button>
              </Link>
              <Link to="/expertise">
                <Button variant="dark-outline" size="large">View Expertise</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
