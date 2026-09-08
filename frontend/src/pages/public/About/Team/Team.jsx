import PageHero from '../../../../components/PageHero/PageHero.jsx'
import Container from '../../../../components/Container/Container.jsx'
import TeamCard from '../../../../components/TeamCard/TeamCard.jsx'
import TeamCardGrid from '../../../../components/TeamCardGrid/TeamCardGrid.jsx'
import { teamContent } from '../../../../constants/aboutContent.js'
import './Team.css'

export default function Team() {
  return (
    <div className="team-page">
      <PageHero
        title={teamContent.hero.title}
        subtitle={teamContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <TeamCardGrid>
          {teamContent.members.map(member => (
            <TeamCard
              key={member.name}
              name={member.name}
              role={member.role}
              bio={member.bio}
            />
          ))}
        </TeamCardGrid>
        {teamContent.placeholderNote && (
          <p className="team-page__note">{teamContent.placeholderNote}</p>
        )}
      </Container>
    </div>
  )
}
