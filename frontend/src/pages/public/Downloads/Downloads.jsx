import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import { downloadsContent } from '../../../constants/downloadsContent.js'
import './Downloads.css'

const categoryTags = downloadsContent.categories.map(cat => (
  <span key={cat.label} className="downloads__category-tag">{cat.label}</span>
))

export default function Downloads() {
  return (
    <div className="downloads-page">
      <PageHero
        title={downloadsContent.hero.title}
        subtitle={downloadsContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <section className="downloads-page__categories">
          <h2 className="downloads-page__categories-title">Available Categories</h2>
          <div className="downloads-page__categories-list">{categoryTags}</div>
        </section>

        <EmptyState
          title={downloadsContent.empty.title}
          text={downloadsContent.empty.text}
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />

        <section className="downloads-page__note">
          <p className="downloads-page__note-text">{downloadsContent.note}</p>
        </section>
      </Container>
    </div>
  )
}
