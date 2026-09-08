import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import './Events.css'

export default function Events() {
  return (
    <div className="events-page">
      <PageHero
        title="Events"
        subtitle="Information about upcoming events, workshops, and gatherings will be shared here as it becomes available."
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <EmptyState
          title="No upcoming events published"
          text="Event information will be published here when available. Please check back later or contact us for general enquiries."
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />
        <div className="events-page__note">
          <p className="events-page__note-text">
            If you would like to propose an event, partnership, or workshop, please contact us through the contact form.
          </p>
        </div>
      </Container>
    </div>
  )
}
