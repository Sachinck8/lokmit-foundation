import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { careersContent } from '../../../constants/careersContent.js'
import './Careers.css'

export default function Careers() {
  return (
    <div className="careers-page">
      <PageHero
        title={careersContent.hero.title}
        subtitle={careersContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <section className="careers-page__intro">
          <h2 className="careers-page__intro-title">Building a Mission-Aligned Team</h2>
          <p className="careers-page__intro-text">{careersContent.intro}</p>
        </section>

        <section className="careers-page__status">
          <div className="careers-page__status-label">
            <span className="careers-page__status-pill">{careersContent.status.label}</span>
          </div>
          <p className="careers-page__status-text">{careersContent.status.description}</p>
        </section>

        <section className="careers-page__related">
          <h3 className="careers-page__related-title">{careersContent.related.label}</h3>
          <ul className="careers-page__related-list">
            {careersContent.related.items.map(item => (
              <li key={item.to}>
                <Link to={item.to} className="careers-page__related-link">{item.label}</Link>
              </li>
            ))}
          </ul>
        </section>
      </Container>
    </div>
  )
}
