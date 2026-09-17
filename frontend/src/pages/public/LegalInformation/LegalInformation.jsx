import { company } from '../../../constants/siteIdentity.js'
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
]

export default function LegalInformation() {
  return (
    <div className="legal-information">
      <PageHero
        title="Legal Information"
        subtitle="Official company and registration details for LOKMIT FOUNDATION."
      />

      <section className="section li__details">
        <Container>
          <div className="li__layout">
            <div className="li__panel">
              <h2 className="li__panel-title">Registered &amp; Corporate Details</h2>
              <dl className="li__list">
                {legalDetails.map(detail => (
                  <div key={detail.label} className="li__row">
                    <dt className="li__label">{detail.label}</dt>
                    <dd className="li__value">{detail.value}</dd>
                  </div>
                ))}
              </dl>
            </div>

            <div className="li__side">
              <div className="li__office">
                <h3 className="li__office-title">Registered Office</h3>
                <address className="li__office-address">
                  {company.registeredOffice.lines.map((line, index) => (
                    <span key={index} className="li__office-line">{line}</span>
                  ))}
                </address>
              </div>

              <div className="li__office">
                <h3 className="li__office-title">Contact</h3>
                <p className="li__office-line"><strong>Official Email:</strong></p>
                <a href={`mailto:${company.officialEmail}`} className="li__email">{company.officialEmail}</a>
                <p className="li__office-line li__director"><strong>Director:</strong> {company.director.name}</p>
              </div>

              <div className="li__links">
                <Link to="/company-profile">
                  <Button variant="primary" size="medium">Company Profile</Button>
                </Link>
                <Link to="/privacy-policy">
                  <Button variant="outline" size="medium">Privacy Policy</Button>
                </Link>
              </div>
            </div>
          </div>

          <p className="li__note">
            This page contains the official company and registration details provided for LOKMIT FOUNDATION. Legal information on this page should not be treated as legal advice.
          </p>
        </Container>
      </section>
    </div>
  )
}
