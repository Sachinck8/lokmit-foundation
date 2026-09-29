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
      />

      <section className="section dm__section">
        <Container>
          <figure className="dm__panel">
            <span className="dm__quote-mark" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M9.6 4C6 6 3.5 9.4 3.5 13.6c0 3 1.9 5.4 4.7 5.4 2.4 0 4.2-1.8 4.2-4.1 0-2.2-1.6-3.9-3.8-3.9-.4 0-.9.1-1 .1.3-2 2.2-4.3 4.3-5.5L9.6 4zm10 0c-3.6 2-6.1 5.4-6.1 9.6 0 3 1.9 5.4 4.7 5.4 2.4 0 4.2-1.8 4.2-4.1 0-2.2-1.6-3.9-3.8-3.9-.4 0-.9.1-1 .1.3-2 2.2-4.3 4.3-5.5L19.6 4z" />
              </svg>
            </span>
            <blockquote className="dm__quote">{directorsMessage.message}</blockquote>
            <figcaption className="dm__signature">
              <span className="dm__name">{directorsMessage.signature.name}</span>
              <span className="dm__title">{directorsMessage.signature.title}</span>
            </figcaption>
          </figure>

          <div className="dm__legal">
            <LegalMarker />
          </div>

          <div className="dm__actions">
            <Link to="/contact">
              <Button variant="primary" size="large">Contact Us</Button>
            </Link>
            <Link to="/company-profile">
              <Button variant="outline" size="large">Company Profile</Button>
            </Link>
          </div>
        </Container>
      </section>
    </div>
  )
}
