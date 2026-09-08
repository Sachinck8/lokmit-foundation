import PartnerGrid from '../../../components/PartnerGrid/PartnerGrid.jsx'
import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import { clientsContent } from '../../../constants/clientsContent.js'
import './Clients.css'

export default function Clients() {
  return (
    <div className="clients-page">
      <PageHero
        title={clientsContent.hero.title}
        subtitle={clientsContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <div className="clients-page__partners">
          <div className="clients-page__partners-label">
            <h2 className="clients-page__section-title">Represented Partners</h2>
            <p className="clients-page__section-subtitle">
              Placeholder partners shown until approved logos and names are provided.
            </p>
          </div>
          <PartnerGrid count={6} label="Partner" />
        </div>

        <EmptyState
          title={clientsContent.empty.title}
          text={clientsContent.empty.text}
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />

        <section className="clients-page__note">
          <p className="clients-page__note-text">{clientsContent.note}</p>
        </section>
      </Container>
    </div>
  )
}
