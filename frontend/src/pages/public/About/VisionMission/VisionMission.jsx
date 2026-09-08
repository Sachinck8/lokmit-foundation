import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import BulletList from '../../../../components/BulletList/BulletList.jsx'
import { visionMission } from '../../../../constants/aboutContent.js'
import './VisionMission.css'

export default function VisionMission() {
  return (
    <div className="vision-mission-page">
      <PageHero
        title={visionMission.hero.title}
        subtitle={visionMission.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <section className="vision-mission-page__vision">
          <h2 className="vision-mission-page__label">{visionMission.vision.label}</h2>
          <p className="vision-mission-page__text">{visionMission.vision.text}</p>
        </section>
        <section className="vision-mission-page__mission">
          <h2 className="vision-mission-page__label">{visionMission.mission.label}</h2>
          <p className="vision-mission-page__intro">{visionMission.mission.intro}</p>
          <ul className="vision-mission-page__items">
            {visionMission.mission.items.map((item, index) => (
              <li key={index} className="vision-mission-page__item">{item}</li>
            ))}
          </ul>
        </section>
      </Container>
    </div>
  )
}
