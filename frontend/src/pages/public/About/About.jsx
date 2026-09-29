import { company } from '../../../constants/siteIdentity.js'
import LegalMarker from '../../../components/LegalMarker/LegalMarker.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './About.css'

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

const hubLinks = [
  {
    to: '/about/vision-mission',
    title: 'Vision & Mission',
    desc: 'Our vision, mission statements, and mission objectives.',
  },
  {
    to: '/about/objectives',
    title: 'Objectives',
    desc: 'The key objectives that guide our work.',
  },
  {
    to: '/about/values',
    title: 'Our Values',
    desc: 'Integrity, transparency, accountability, professionalism, and more.',
  },
  {
    to: '/about/directors-message',
    title: "Director's Message",
    desc: 'A message from Mr. Sanjay Kumar Yadav, Director.',
  },
  {
    to: '/about/team',
    title: 'Leadership',
    desc: 'Directed by Mr. Sanjay Kumar Yadav.',
  },
]

export default function About() {
  return (
    <div className="about-page">
      <PageHero
        title="About Us"
        subtitle="A professional organization providing technical consultancy, skill development, livelihood and employment support across India."
        badge={company.legalStatus}
      />

      <section className="section about__facts">
        <Container>
          <ul className="about__facts-list">
            <li className="about__fact">
              <span className="about__fact-label">Legal Status</span>
              <span className="about__fact-value">{company.legalStatus}</span>
            </li>
            <li className="about__fact">
              <span className="about__fact-label">CIN</span>
              <span className="about__fact-value">{company.cin}</span>
            </li>
            <li className="about__fact">
              <span className="about__fact-label">ROC</span>
              <span className="about__fact-value">{company.roc}</span>
            </li>
            <li className="about__fact">
              <span className="about__fact-label">Incorporated</span>
              <span className="about__fact-value">{company.incorporationDate}</span>
            </li>
          </ul>
        </Container>
      </section>

      <section className="section section--alt about__overview">
        <Container>
          <div className="about__overview-grid">
            <div>
              <SectionHeader
                badge="Who We Are"
                title="A Professional Technical Consultancy Organization"
              />
              <p className="about__overview-text">{company.shortDescription}</p>
              <p className="about__overview-tagline">Empowering Lives. Building Brighter Futures.</p>
              <div className="about__overview-actions">
                <Link to="/company-profile">
                  <Button variant="primary" size="medium">Company Profile</Button>
                </Link>
                <Link to="/legal-information">
                  <Button variant="outline" size="medium">Legal Information</Button>
                </Link>
              </div>
            </div>
            <div className="about__overview-aside">
              <LegalMarker />
              <ul className="about__scope-list">
                {objectives.slice(0, 5).map(item => (
                  <li key={item} className="about__scope-item">{item}</li>
                ))}
              </ul>
            </div>
          </div>
        </Container>
      </section>

      <section className="section about__objectives">
        <Container>
          <SectionHeader
            badge="Objectives"
            title="What Guides Our Work"
            subtitle="The core objectives that define the scope of our work."
            align="centered"
          />
          <ul className="about__objectives-grid">
            {objectives.map(item => (
              <li key={item} className="about__objective-chip">{item}</li>
            ))}
          </ul>
        </Container>
      </section>

      <section className="section section--tinted about__hub">
        <Container>
          <SectionHeader
            badge="Explore"
            title="More About LOKMIT FOUNDATION"
            align="centered"
          />
          <div className="about__hub-grid">
            {hubLinks.map(link => (
              <Link key={link.to} to={link.to} className="about__hub-card">
                <span className="about__hub-card-title">{link.title}</span>
                <span className="about__hub-card-desc">{link.desc}</span>
                <span className="about__hub-card-arrow" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M5 12h14M12 5l7 7-7 7" />
                  </svg>
                </span>
              </Link>
            ))}
          </div>
        </Container>
      </section>

      <section className="section section--dark about__cta">
        <Container>
          <div className="about__cta-inner">
            <h2 className="about__cta-title">Work With Us</h2>
            <p className="about__cta-subtitle">
              Whether you need technical consultancy, project advisory, documentation support, compliance assistance, or skill development solutions, we would like to hear from you.
            </p>
            <div className="about__cta-actions">
              <Link to="/contact">
                <Button variant="secondary" size="large">Contact Us</Button>
              </Link>
              <Link to="/company-profile">
                <Button variant="dark-outline" size="large">Company Profile</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
