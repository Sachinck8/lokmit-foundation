import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import './Events.css'

export default function Events() {
  return (
    <div className="events-page">
      <PageHero
        title="Events"
        subtitle="Information about upcoming events, workshops, and gatherings will be shared here as it becomes available."
      />
      <section className="section events__section">
        <Container>
          <EmptyState
            icon={<Icon name="clock" />}
            title="No upcoming events published"
            text="Event information will be published here when available. Please check back later or contact us for general enquiries."
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />
          <div className="events__note">
            <p className="events__note-text">
              If you would like to propose an event, partnership, or workshop, please contact us through the contact form.
            </p>
          </div>
        </Container>
      </section>
    </div>
  )
}
