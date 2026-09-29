import { company } from '../../../constants/siteIdentity.js'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './CompanyProfile.css'

const quickFacts = [
  { label: 'Legal Status', value: company.legalStatus },
  { label: 'CIN', value: company.cin },
  { label: 'ROC', value: company.roc },
  { label: 'Date of Incorporation', value: company.incorporationDate },
  { label: 'PAN', value: company.pan },
  { label: 'TAN', value: company.tan },
  { label: 'Registered Office', value: company.registeredOffice.lines.join(' ') },
  { label: 'Director', value: company.director.name },
]

const objectives = [
  'Technical Consultancy',
  'Project Advisory',
  'Capacity Building',
  'Documentation Support',
  'Compliance Guidance',
  'Skill Development Promotion',
  'Employment Generation',
  'Rural Livelihood Promotion',
  'Digital Transformation',
  'Organizational Development',
]

export default function CompanyProfile() {
  return (
    <div className="company-profile">
      <PageHero
        title="Company Profile"
        subtitle="A profile of LOKMIT FOUNDATION as provided in the official company information."
      />

      <section className="section cp__facts">
        <Container>
          <ul className="cp__facts-list">
            {quickFacts.map(fact => (
              <li key={fact.label} className="cp__fact">
                <span className="cp__fact-label">{fact.label}</span>
                <span className="cp__fact-value">{fact.value}</span>
              </li>
            ))}
          </ul>
        </Container>
      </section>

      <section className="section section--alt cp__overview">
        <Container>
          <div className="cp__overview-grid">
            <div>
              <SectionHeader
                badge="About the Company"
                title="Professional Consultancy, Built on Strong Systems"
              />
              <p className="cp__overview-text">{company.shortDescription}</p>
              <p className="cp__overview-tagline">Empowering Lives. Building Brighter Futures.</p>
              <div className="cp__overview-actions">
                <Link to="/contact">
                  <Button variant="primary" size="medium">Contact Us</Button>
                </Link>
                <Link to="/legal-information">
                  <Button variant="outline" size="medium">Legal Information</Button>
                </Link>
              </div>
            </div>
            <div className="cp__overview-aside">
              <LegalMarker />
            </div>
          </div>
        </Container>
      </section>

      <section className="section cp__objectives">
        <Container>
          <SectionHeader
            badge="Objectives"
            title="Scope of Our Work"
            align="centered"
          />
          <ul className="cp__objectives-grid">
            {objectives.map(item => (
              <li key={item} className="cp__objective">{item}</li>
            ))}
          </ul>
        </Container>
      </section>

      <section className="section section--dark cp__cta">
        <Container>
          <div className="cp__cta-inner">
            <h2 className="cp__cta-title">Work With Us</h2>
            <p className="cp__cta-subtitle">
              Whether you need technical consultancy, project advisory, documentation support, compliance assistance, or skill development solutions, we would like to hear from you.
            </p>
            <div className="cp__cta-actions">
              <Link to="/contact">
                <Button variant="secondary" size="large">Contact Us</Button>
              </Link>
              <Link to="/services">
                <Button variant="dark-outline" size="large">Our Services</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
