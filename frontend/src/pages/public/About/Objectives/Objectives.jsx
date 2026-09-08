import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import BulletList from '../../../../components/BulletList/BulletList.jsx'
import { objectivesContent } from '../../../../constants/aboutContent.js'
import './Objectives.css'

export default function Objectives() {
  return (
    <div className="objectives-page">
      <PageHero
        title={objectivesContent.hero.title}
        subtitle={objectivesContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <section className="objectives-page__list">
          <BulletList
            title={objectivesContent.hero.title}
            items={objectivesContent.items.map(item => item.title)}
          />
        </section>
        <p className="objectives-page__note">{objectivesContent.note}</p>
      </Container>
    </div>
  )
}
