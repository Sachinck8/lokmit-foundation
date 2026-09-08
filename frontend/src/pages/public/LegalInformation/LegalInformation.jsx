import { company } from '../../../constants/siteIdentity.js'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import StatBlock from '../../../components/StatBlock/StatBlock.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './LegalInformation.css'

const legalDetails = [
  { label: 'Legal Status', value: company.legalStatus },
  { label: 'CIN', value: company.cin },
  { label: 'ROC', value: company.roc },
  { label: 'Date of Incorporation', value: company.incorporationDate },
  { label: 'PAN', value: company.pan },
  { label: 'TAN', value: company.tan },
  { label: 'Registered Office', value: company.registeredOffice.lines.join(' ') },
  { label: 'Director', value: company.director.name },
  { label: 'Official Email', value: company.officialEmail },
]

export default function LegalInformation() {
  return (
    <div className="legal-information">
      <PageHero
        title="Legal Information"
        subtitle="Official company and registration details for LOKMIT FOUNDATION."
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <section className="section legal-information__details">
        <Container>
          <StatBlock title="Registered & Corporate Details" items={legalDetails} compact />
        </Container>
      </section>

      <section className="section legal-information__note">
        <Container>
          <div className="legal-information__note-inner">
            <LegalMarker />
            <p className="legal-information__note-text">
              This page contains the official company and registration details provided for LOKMIT FOUNDATION. Legal information should not be treated as legal advice.
            </p>
            <div className="legal-information__links">
              <Link to="/company-profile">
                <Button variant="primary" size="medium">Company Profile</Button>
              </Link>
              <Link to="/privacy-policy">
                <Button variant="outline" size="medium">Privacy Policy</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
