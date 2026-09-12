import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import { teamContent } from '../../../../constants/aboutContent.js'
import './Team.css'

export default function Team() {
  return (
    <div className="team-page">
      <PageHero
        title={teamContent.hero.title}
        subtitle={teamContent.hero.subtitle}
      />

      <section className="section team__section">
        <Container>
          <div className="team__grid">
            {teamContent.members.map(member => (
              <article key={member.name} className="team__card">
                <div className="team__avatar" aria-hidden="true">
                  <span>{member.name.split(' ').map(part => part[0]).slice(0, 2).join('')}</span>
                </div>
                <h2 className="team__name">{member.name}</h2>
                <p className="team__role">{member.role}</p>
                <p className="team__bio">{member.bio}</p>
              </article>
            ))}
          </div>
          <p className="team__note">{teamContent.placeholderNote}</p>
        </Container>
      </section>
    </div>
  )
}
