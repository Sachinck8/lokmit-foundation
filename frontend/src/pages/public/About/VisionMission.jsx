import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import { aboutContent } from '../../../constants/aboutContent.js'
import './VisionMission.css'

export default function VisionMission() {
  const hero = aboutContent.hero
  const vision = aboutContent.vision
  const mission = aboutContent.mission

  return (
    <div className="vision-mission-page">
      <PageHero
        title={hero.title}
        subtitle={hero.subtitle}
        background={hero.background}
        align="left"
      >
        <a href="/about" className="vision-mission__back">
          Back to About
        </a>
      </PageHero>

      <section className="section vision-mission__section">
        <Container>
          <div className="vision-mission__inner">
            <div className="vision-mission__block">
              <SectionHeader
                title={vision.title}
                align="left"
                className="section-title"
              />
              <p className="vision-mission__text">{vision.text}</p>
            </div>

            <div className="vision-mission__block">
              <SectionHeader
                title={mission.title}
                subtitle={mission.intro}
                align="left"
                className="section-title"
              />
              <Accordion
                items={mission.items.map((item) => ({
                  question: item,
                  answer: '',
                }))}
                className="vision-mission__accordion"
              />
              <div className="vision-mission__mission-note">
                <strong>Our mission is to:</strong>
              </div>
              <ul className="vision-mission__mission-list">
                {mission.items.map((item, index) => (
                  <li key={index} className="vision-mission__mission-item">
                    <span className="vision-mission__mission-number">{index + 1}</span>
                    <span>{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
