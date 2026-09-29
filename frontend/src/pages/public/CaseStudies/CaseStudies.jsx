import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { caseStudiesContent } from '../../../constants/caseStudiesContent.js'
import './CaseStudies.css'

export default function CaseStudies() {
  return (
    <div className="case-studies-page">
      <PageHero
        title={caseStudiesContent.hero.title}
        subtitle={caseStudiesContent.hero.subtitle}
      />

      <section className="section cs__section">
        <Container>
          <EmptyState
            icon={<Icon name="document" />}
            title={caseStudiesContent.empty.title}
            text={caseStudiesContent.empty.text}
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />

          <section className="cs__structure">
            <h2 className="cs__structure-title">Case Study Framework</h2>
            <p className="cs__structure-intro">
              Each published case study will follow this structure where content is available:
            </p>
            <ul className="cs__structure-list">
              {caseStudiesContent.structure.fields.map((field, index) => (
                <li key={field} className="cs__structure-item">
                  <span className="cs__structure-number">{String(index + 1).padStart(2, '0')}</span>
                  <span className="cs__structure-field">{field}</span>
                </li>
              ))}
            </ul>
          </section>
        </Container>
      </section>
    </div>
  )
}
