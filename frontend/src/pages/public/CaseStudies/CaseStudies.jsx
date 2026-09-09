import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import { useNavigate } from 'react-router-dom'
import { caseStudiesContent } from '../../../constants/caseStudiesContent.js'
import '../LegalPage/LegalPage.css'

export default function CaseStudies() {
  const navigate = useNavigate()
  return (
    <div className="case-studies-page">
      <PageHero
        title={caseStudiesContent.hero.title}
        subtitle={caseStudiesContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <EmptyState
          title={caseStudiesContent.empty.title}
          text={caseStudiesContent.empty.text}
          secondaryLabel="Contact Us"
          onSecondary={() => navigate('/contact')}
        />

        <section className="case-studies-page__structure">
          <h2 className="case-studies-page__structure-title">Reusable Case Study Structure</h2>
          <p className="case-studies-page__structure-intro">
            Each published case study will follow this structure where content is available:
          </p>
          <ul className="case-studies-page__structure-list">
            {caseStudiesContent.structure.fields.map((field, index) => (
              <li key={index} className="case-studies-page__structure-item">
                <span className="case-studies-page__structure-number">{String(index + 1).padStart(2, '0')}</span>
                <span className="case-studies-page__structure-field">{field}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="case-studies-page__template">
          <h2 className="case-studies-page__template-title">Case Study Template</h2>
          <dl className="case-studies-page__template-list">
            {Object.entries(caseStudiesContent.caseStudyTemplate).map(([key, value]) => (
              <div key={key} className="case-studies-page__template-item">
                <dt className="case-studies-page__template-label">{key.replace(/([A-Z])/g, ' $1').trim()}</dt>
                <dd className="case-studies-page__template-value">
                  {typeof value === 'string' && !value ? (
                    <em>To be added</em>
                  ) : Array.isArray(value) ? (
                    value.length ? value.map((item, i) => <li key={i}>{item}</li>) : <em>No items</em>
                  ) : (
                    value
                  )}
                </dd>
              </div>
            ))}
          </dl>
        </section>
      </Container>
    </div>
  )
}
