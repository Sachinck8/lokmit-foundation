import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import { expertiseContent } from '../../../constants/servicesContent.js'
import './Expertise.css'

export default function Expertise() {
  return (
    <div className="expertise-page">
      <PageHero
        title={expertiseContent.hero.title}
        subtitle={expertiseContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <section className="section expertise-page__sectors">
        <Container>
          <BulletList
            title="Sectors We Serve"
            items={expertiseContent.sectors.map(s => s.name)}
          />
        </Container>
      </section>

      <section className="section expertise-page__expertise section--alt">
        <Container>
          <BulletList
            title="Areas of Expertise"
            items={expertiseContent.expertise.map(e => e.name)}
          />
        </Container>
      </section>

      <section className="section expertise-page__disclaimer">
        <Container>
          <div className="expertise-page__disclaimer-inner">
            <LegalMarker compact />
            <p className="expertise-page__disclaimer-text">{expertiseContent.disclaimer.text}</p>
          </div>
        </Container>
      </section>
    </div>
  )
}
