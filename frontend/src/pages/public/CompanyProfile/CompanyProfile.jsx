import { company } from '../../../constants/siteIdentity.js'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import StatBlock from '../../../components/StatBlock/StatBlock.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
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
]

export default function CompanyProfile() {
  return (
    <div className="company-profile">
      <PageHero
        title="Company Profile"
        subtitle="A profile of LOKMIT FOUNDATION as provided in the official company information."
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <section className="section company-profile__quick-facts">
        <Container>
          <StatBlock title="Company Identification Details" items={quickFacts} />
        </Container>
      </section>

      <section className="section company-profile__overview">
        <Container>
          <div className="company-profile__overview-inner">
            <LegalMarker />
            <h2 className="company-profile__overview-title">About the Company</h2>
            <p className="company-profile__overview-text">{company.shortDescription}</p>
            <p className="company-profile__tagline">{company.tagline}</p>
          </div>
        </Container>
      </section>

      <section className="section company-profile__objectives">
        <Container>
          <BulletList
            title="Objectives"
            description="The core objectives that define the scope of our work."
            items={[
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
            ]}
          />
        </Container>
      </section>

      <section className="section company-profile__cta">
        <Container>
          <div className="company-profile__cta-inner">
            <h2 className="company-profile__cta-title">Work With Us</h2>
            <p className="company-profile__cta-subtitle">
              Whether you need technical consultancy, project advisory, documentation support, compliance assistance, or skill development solutions, we would like to hear from you.
            </p>
            <div className="company-profile__cta-actions">
              <Link to="/contact">
                <Button variant="primary" size="large">Contact Us</Button>
              </Link>
              <Link to="/legal-information">
                <Button variant="outline" size="large">Legal Information</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
