import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import { newsContent } from '../../../constants/newsContent.js'
import './News.css'

export default function News() {
  return (
    <div className="news-page">
      <PageHero
        title={newsContent.hero.title}
        subtitle={newsContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <EmptyState
          title={newsContent.empty.title}
          text={newsContent.empty.text}
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />
        <p className="news-page__note">{newsContent.note}</p>
      </Container>
    </div>
  )
}
