import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { downloadsContent } from '../../../constants/downloadsContent.js'
import './Downloads.css'

export default function Downloads() {
  return (
    <div className="downloads-page">
      <PageHero
        title={downloadsContent.hero.title}
        subtitle={downloadsContent.hero.subtitle}
      />

      <section className="section downloads__section">
        <Container>
          <section className="downloads__categories">
            <h2 className="downloads__categories-title">Available Categories</h2>
            <div className="downloads__categories-list">
              {downloadsContent.categories.map(cat => (
                <span key={cat.label} className="downloads__category-tag">{cat.label}</span>
              ))}
            </div>
          </section>

          <EmptyState
            icon={<Icon name="document" />}
            title={downloadsContent.empty.title}
            text={downloadsContent.empty.text}
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />

          <p className="downloads__note">{downloadsContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
