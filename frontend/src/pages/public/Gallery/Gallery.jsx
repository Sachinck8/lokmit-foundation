import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './Gallery.css'

export default function Gallery() {
  return (
    <div className="gallery-page">
      <PageHero
        title="Gallery"
        subtitle="A visual journey through our projects, events, and the communities we serve will be added as approved images become available."
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <EmptyState
          title="No gallery content yet"
          text="Gallery content will be published here when approved organizational images are available."
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />
      </Container>
    </div>
  )
}
