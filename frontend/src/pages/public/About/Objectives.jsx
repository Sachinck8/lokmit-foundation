import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import { aboutContent } from '../../../constants/aboutContent.js'
import './Objectives.css'

export default function Objectives() {
  const hero = aboutContent.hero
  const objectives = aboutContent.objectives

  return (
    <div className="objectives-page">
      <PageHero
        title={hero.title}
        subtitle="Key objectives that guide our work."
        background={hero.background}
        align="left"
      >
        <a href="/about" className="objectives__back">
          Back to About
        </a>
      </PageHero>

      <section className="section objectives__section">
        <Container>
          <SectionHeader
            title={objectives.title}
            align="left"
            className="section-title"
          />
          <ul className="objectives__list">
            {objectives.items.map((item, index) => (
              <li key={index} className="objectives__item">
                <span className="objectives__number">{index + 1}</span>
                <span className="objectives__text">{item}</span>
              </li>
            ))}
          </ul>
        </Container>
      </section>
    </div>
  )
}
