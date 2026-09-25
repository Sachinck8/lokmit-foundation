import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import './Gallery.css'

export default function Gallery() {
  return (
    <div className="gallery-page">
      <PageHero
        title="Gallery"
        subtitle="A visual journey through our projects, events, and the communities we serve will be added as approved images become available."
      />
      <section className="section gallery__section">
        <Container>
          <EmptyState
            icon={<Icon name="image" />}
            title="No gallery content yet"
            text="Gallery content will be published here when approved organizational images are available."
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />
        </Container>
      </section>
    </div>
  )
}
