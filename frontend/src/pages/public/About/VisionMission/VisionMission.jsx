import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import { visionMission } from '../../../../constants/aboutContent.js'
import './VisionMission.css'

export default function VisionMission() {
  return (
    <div className="vision-mission-page">
      <PageHero
        title={visionMission.hero.title}
        subtitle={visionMission.hero.subtitle}
      />

      <section className="section vm__vision">
        <Container>
          <div className="vm__vision-panel">
            <span className="vm__eyebrow">{visionMission.vision.label}</span>
            <blockquote className="vm__vision-text">{visionMission.vision.text}</blockquote>
          </div>
        </Container>
      </section>

      <section className="section section--tinted vm__mission">
        <Container>
          <div className="vm__mission-panel">
            <span className="vm__eyebrow vm__eyebrow--green">{visionMission.mission.label}</span>
            <p className="vm__mission-intro">{visionMission.mission.intro}</p>
            <ul className="vm__mission-list">
              {visionMission.mission.items.map((item, index) => (
                <li key={index} className="vm__mission-item">
                  <span className="vm__mission-num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                  <span className="vm__mission-text">{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </Container>
      </section>
    </div>
  )
}
