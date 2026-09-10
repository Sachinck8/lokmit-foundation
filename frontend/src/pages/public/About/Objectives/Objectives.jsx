import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import { objectivesContent } from '../../../../constants/aboutContent.js'
import './Objectives.css'

export default function Objectives() {
  return (
    <div className="objectives-page">
      <PageHero
        title={objectivesContent.hero.title}
        subtitle={objectivesContent.hero.subtitle}
      />

      <section className="section obj__list">
        <Container>
          <ul className="obj__grid">
            {objectivesContent.items.map((item, index) => (
              <li key={item.title} className="obj__item">
                <span className="obj__num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                <span className="obj__title">{item.title}</span>
              </li>
            ))}
          </ul>
          <p className="obj__note">{objectivesContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
