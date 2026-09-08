import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { projectsContent } from '../../../constants/projectsContent.js'
import './Projects.css'

export default function Projects() {
  return (
    <div className="projects-page">
      <PageHero
        title={projectsContent.hero.title}
        subtitle={projectsContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <EmptyState
          title={projectsContent.empty.title}
          text={projectsContent.empty.text}
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />
        <p className="projects-page__note">{projectsContent.note}</p>
      </Container>
    </div>
  )
}
