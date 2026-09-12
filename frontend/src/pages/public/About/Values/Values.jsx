import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import { valuesContent } from '../../../../constants/aboutContent.js'
import './Values.css'

export default function Values() {
  return (
    <div className="values-page">
      <PageHero
        title={valuesContent.hero.title}
        subtitle={valuesContent.hero.subtitle}
      />

      <section className="section values__section">
        <Container>
          <ul className="values__grid">
            {valuesContent.items.map(item => (
              <li key={item.name} className="values__card">
                <span className="values__mark" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </span>
                <h2 className="values__name">{item.name}</h2>
                <p className="values__desc">{item.description}</p>
              </li>
            ))}
          </ul>
        </Container>
      </section>
    </div>
  )
}
