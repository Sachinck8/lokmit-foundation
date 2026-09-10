import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { newsContent } from '../../../constants/newsContent.js'
import './News.css'

export default function News() {
  return (
    <div className="news-page">
      <PageHero
        title={newsContent.hero.title}
        subtitle={newsContent.hero.subtitle}
      />
      <section className="section news__section">
        <Container>
          <EmptyState
            icon={<Icon name="book" />}
            title={newsContent.empty.title}
            text={newsContent.empty.text}
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />
          <p className="news__note">{newsContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
