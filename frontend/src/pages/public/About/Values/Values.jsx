import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import BulletList from '../../../../components/BulletList/BulletList.jsx'
import { valuesContent } from '../../../../constants/aboutContent.js'
import './Values.css'

export default function Values() {
  return (
    <div className="values-page">
      <PageHero
        title={valuesContent.hero.title}
        subtitle={valuesContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <section className="values-page__list">
          <BulletList
            title={valuesContent.hero.title}
            description="The principles that shape how we work and partner with organizations."
            items={valuesContent.items.map(item => `${item.name}: ${item.description}`)}
          />
        </section>
      </Container>
    </div>
  )
}
