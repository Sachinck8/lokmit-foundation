import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import LegalMarker from '../../../../components/LegalMarker/LegalMarker.jsx'
import Button from '../../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { directorsMessage } from '../../../../constants/aboutContent.js'
import './DirectorsMessage.css'

export default function DirectorsMessage() {
  return (
    <div className="directors-message-page">
      <PageHero
        title={directorsMessage.hero.title}
        subtitle={directorsMessage.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <section className="directors-message-page__message">
          <blockquote className="directors-message-page__quote">
            {directorsMessage.message}
          </blockquote>
          <footer className="directors-message-page__signature">
            <cite className="directors-message-page__name">{directorsMessage.signature.name}</cite>
            <span className="directors-message-page__title">{directorsMessage.signature.title}</span>
          </footer>
        </section>
        <section className="directors-message-page__legal">
          <LegalMarker />
        </section>
        <section className="directors-message-page__actions">
          <Link to="/contact">
            <Button variant="primary" size="large">Contact Us</Button>
          </Link>
          <Link to="/company-profile">
            <Button variant="outline" size="large">Company Profile</Button>
          </Link>
        </section>
      </Container>
    </div>
  )
}
