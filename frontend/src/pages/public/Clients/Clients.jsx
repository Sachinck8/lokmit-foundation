import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { clientsContent } from '../../../constants/clientsContent.js'
import './Clients.css'

export default function Clients() {
  return (
    <div className="clients-page">
      <PageHero
        title={clientsContent.hero.title}
        subtitle={clientsContent.hero.subtitle}
      />

      <section className="section clients__section">
        <Container>
          <EmptyState
            icon={<Icon name="briefcase" />}
            title={clientsContent.empty.title}
            text={clientsContent.empty.text}
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />

          <p className="clients__note">{clientsContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
