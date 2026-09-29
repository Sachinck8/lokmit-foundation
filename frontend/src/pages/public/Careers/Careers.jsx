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
      />

      <Container>
        <section className="section careers__intro">
          <h2 className="careers__intro-title">Building a Mission-Aligned Team</h2>
          <p className="careers__intro-text">{careersContent.intro}</p>
        </section>

        <section className="section section--tinted careers__status">
          <div className="careers__status-panel">
            <span className="careers__status-pill">{careersContent.status.label}</span>
            <p className="careers__status-text">{careersContent.status.description}</p>
          </div>
        </section>

        <section className="section careers__related">
          <h3 className="careers__related-title">{careersContent.related.label}</h3>
          <div className="careers__related-actions">
            {careersContent.related.items.map(item => (
              <Link key={`${item.label}-${item.to}`} to={item.to}>
                <Button variant={item.to === '/contact' ? 'primary' : 'outline'} size="medium">
                  {item.label}
                </Button>
              </Link>
            ))}
          </div>
        </section>
      </Container>
    </div>
  )
}
