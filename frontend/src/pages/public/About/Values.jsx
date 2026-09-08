import Container from '../../../components/Container/Container.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import { aboutContent } from '../../../constants/aboutContent.js'
import './Values.css'

export default function Values() {
  const hero = aboutContent.hero
  const values = aboutContent.values

  return (
    <div className="values-page">
      <PageHero
        title={hero.title}
        subtitle="The principles that guide our work."
        background={hero.background}
        align="left"
      >
        <a href="/about" className="values__back">
          Back to About
        </a>
      </PageHero>

      <section className="section values__section">
        <Container>
          <div className="values__header">
            <h2 className="section-title">{values.title}</h2>
            {values.subtitle && <p className="values__subtitle">{values.subtitle}</p>}
          </div>
          <div className="values__grid">
            {values.items.map((value, index) => (
              <div key={index} className="values__card">
                <div className="values__badge">{index + 1}</div>
                <div className="values__content">
                  <h3 className="values__title">{value.title}</h3>
                  <p className="values__desc">{value.short}</p>
                </div>
              </div>
            ))}
          </div>
        </Container>
      </section>
    </div>
  )
}
