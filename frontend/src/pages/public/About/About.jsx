import { company } from '../../../constants/siteIdentity.js'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import StatBlock from '../../../components/StatBlock/StatBlock.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './About.css'

export default function About() {
  return (
    <div className="about-page">
      <PageHero
        title="About Us"
        subtitle="A professional organization providing technical consultancy, skill development, livelihood and employment support across India."
        badge={company.legalStatus}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <section className="section about__quick-facts">
        <Container>
          <StatBlock
            title="Company Identification Details"
            items={[
              { label: 'Legal Status', value: company.legalStatus },
              { label: 'CIN', value: company.cin },
              { label: 'ROC', value: company.roc },
              { label: 'Date of Incorporation', value: company.incorporationDate },
            ]}
          />
        </Container>
      </section>

      <section className="section about__overview">
        <Container>
          <div className="about__overview-inner">
            <LegalMarker />
            <h2 className="about__overview-title">Who We Are</h2>
            <p className="about__overview-text">{company.shortDescription}</p>
            <p className="about__overview-tagline">{company.tagline}</p>
          </div>
        </Container>
      </section>

      <section className="section about__objectives">
        <Container>
          <div className="about__objectives-inner">
            <BulletList
              title="Our Objectives"
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
          </div>
        </Container>
      </section>

      <section className="section about__hub">
        <Container>
          <h2 className="about__hub-title">Explore About Us</h2>
          <ul className="about__hub-list">
            <li>
              <Link to="/about/vision-mission" className="about__hub-link">
                <span className="about__hub-link-title">Vision & Mission</span>
                <span className="about__hub-link-desc">Our vision, mission statements, and mission objectives.</span>
              </Link>
            </li>
            <li>
              <Link to="/about/objectives" className="about__hub-link">
                <span className="about__hub-link-title">Objectives</span>
                <span className="about__hub-link-desc">The key objectives that guide our work.</span>
              </Link>
            </li>
            <li>
              <Link to="/about/values" className="about__hub-link">
                <span className="about__hub-link-title">Our Values</span>
                <span className="about__hub-link-desc">Integrity, transparency, accountability, professionalism, innovation, quality, excellence, and commitment.</span>
              </Link>
            </li>
            <li>
              <Link to="/about/directors-message" className="about__hub-link">
                <span className="about__hub-link-title">Director's Message</span>
                <span className="about__hub-link-desc">A message from Mr. Sanjay Kumar Yadav, Director.</span>
              </Link>
            </li>
            <li>
              <Link to="/about/team" className="about__hub-link">
                <span className="about__hub-link-title">Leadership</span>
                <span className="about__hub-link-desc">Directed by Mr. Sanjay Kumar Yadav.</span>
              </Link>
            </li>
          </ul>
        </Container>
      </section>

      <section className="section about__cta section--dark">
        <Container>
          <div className="about__cta-inner" style={{ textAlign: 'center' }}>
            <h2 className="about__cta-title" style={{ color: 'var(--color-surface)' }}>Work With Us</h2>
            <p className="about__cta-subtitle" style={{ color: 'rgba(255,255,255,0.88)' }}>
              Whether you need technical consultancy, project advisory, documentation support, compliance assistance, or skill development solutions, we would like to hear from you.
            </p>
            <div className="about__cta-actions" style={{ display: 'flex', gap: 'var(--spacing-4)', justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link to="/contact">
                <Button variant="primary" size="large">Contact Us</Button>
              </Link>
              <Link to="/company-profile">
                <Button variant="outline" size="large">Company Profile</Button>
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
