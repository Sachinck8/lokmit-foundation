import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { impactContent } from '../../../constants/impactContent.js'
import './Impact.css'

export default function Impact() {
  return (
    <div className="impact-page">
      <PageHero
        title={impactContent.hero.title}
        subtitle={impactContent.hero.subtitle}
      />

      <section className="section impact__section">
        <Container>
          <EmptyState
            icon={<Icon name="compass" />}
            title={impactContent.empty.title}
            text={impactContent.empty.text}
            secondaryLabel="Go to Contact"
            secondaryTo="/contact"
          />

          <section className="impact__metrics">
            <p className="impact__metrics-intro">
              The following metrics are reserved for verified organizational statistics:
            </p>
            <ul className="impact__metrics-list">
              {impactContent.placeholders.map((item, index) => (
                <li key={index} className="impact__metric">
                  <span className="impact__metric-label">{item.label}</span>
                  <span className="impact__metric-status">Pending verification</span>
                </li>
              ))}
            </ul>
          </section>

          <p className="impact__note">{impactContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
