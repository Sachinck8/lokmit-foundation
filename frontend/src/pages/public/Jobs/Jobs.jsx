import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import { jobsContent } from '../../../constants/careersContent.js'
import './Jobs.css'

export default function Jobs() {
  return (
    <div className="jobs-page">
      <PageHero
        title={jobsContent.hero.title}
        subtitle={jobsContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <section className="jobs-page__candidate">
          <h2 className="jobs-page__portal-title">{jobsContent.candidatePortal.label}</h2>
          <p className="jobs-page__portal-desc">{jobsContent.candidatePortal.description}</p>
          <BulletList
            title="Planned Candidate Features"
            items={jobsContent.candidatePortal.features}
          />
        </section>

        <section className="jobs-page__employer">
          <h2 className="jobs-page__portal-title">{jobsContent.employerPortal.label}</h2>
          <p className="jobs-page__portal-desc">{jobsContent.employerPortal.description}</p>
          <BulletList
            title="Planned Employer Features"
            items={jobsContent.employerPortal.features}
          />
        </section>

        <EmptyState
          title={jobsContent.note}
          text="The job portal is not yet available. This page is a placeholder for the future portal experience."
          secondaryLabel="Contact Us"
          onSecondary={() => {}}
        />
      </Container>
    </div>
  )
}
