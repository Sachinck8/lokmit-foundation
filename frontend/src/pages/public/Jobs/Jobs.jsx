import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { jobsContent } from '../../../constants/careersContent.js'
import './Jobs.css'

function PortalCard({ label, description, features }) {
  return (
    <article className="jobs__portal">
      <h2 className="jobs__portal-title">{label}</h2>
      <p className="jobs__portal-desc">{description}</p>
      <ul className="jobs__portal-features">
        {features.map(feature => (
          <li key={feature} className="jobs__portal-feature">
            <svg className="jobs__feature-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            {feature}
          </li>
        ))}
      </ul>
    </article>
  )
}

export default function Jobs() {
  return (
    <div className="jobs-page">
      <PageHero
        title={jobsContent.hero.title}
        subtitle={jobsContent.hero.subtitle}
      />

      <section className="section jobs__section">
        <Container>
          <div className="jobs__grid">
            <PortalCard
              label={jobsContent.candidatePortal.label}
              description={jobsContent.candidatePortal.description}
              features={jobsContent.candidatePortal.features}
            />
            <PortalCard
              label={jobsContent.employerPortal.label}
              description={jobsContent.employerPortal.description}
              features={jobsContent.employerPortal.features}
            />
          </div>

          <EmptyState
            icon={<Icon name="clock" />}
            title={jobsContent.note}
            text="The job portal is not yet available. This page is a placeholder for the future portal experience."
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />
        </Container>
      </section>
    </div>
  )
}
