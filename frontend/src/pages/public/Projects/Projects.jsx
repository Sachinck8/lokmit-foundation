import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { projectsContent } from '../../../constants/projectsContent.js'
import './Projects.css'

export default function Projects() {
  return (
    <div className="projects-page">
      <PageHero
        title={projectsContent.hero.title}
        subtitle={projectsContent.hero.subtitle}
      />
      <section className="section projects__section">
        <Container>
          <EmptyState
            icon={<Icon name="briefcase" />}
            title={projectsContent.empty.title}
            text={projectsContent.empty.text}
            secondaryLabel="Contact Us"
            secondaryTo="/contact"
          />
          <p className="projects__note">{projectsContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
