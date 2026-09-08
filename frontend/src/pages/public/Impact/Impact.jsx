import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import { impactContent } from '../../../constants/impactContent.js'
import './Impact.css'

export default function Impact() {
  return (
    <div className="impact-page">
      <PageHero
        title={impactContent.hero.title}
        subtitle={impactContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <EmptyState
          title={impactContent.empty.title}
          text={impactContent.empty.text}
          actionLabel="Request Verified Figures"
          onAction={() => {}}
          secondaryLabel="Go to Contact"
          onSecondary={() => {}}
        />

        <section className="impact-page__placeholder-list">
          <div className="impact-page__placeholder-intro">
            <p className="impact-page__placeholder-intro-text">
              The following metrics are reserved for verified organizational statistics:
            </p>
          </div>
          <ul className="impact-page__placeholder-items">
            {impactContent.placeholders.map((item, index) => (
              <li key={index} className="impact-page__placeholder-item">
                <span className="impact-page__placeholder-label">{item.label}</span>
                <span className="impact-page__placeholder-status">Pending verification</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="impact-page__note">
          <p className="impact-page__note-text">{impactContent.note}</p>
        </section>
      </Container>
    </div>
  )
}
