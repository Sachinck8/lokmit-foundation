import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import Badge from '../../../components/Badge/Badge.jsx'
import Button from '../../../components/Button/Button.jsx'
import { projectsContent } from '../../../constants/projectsContent.js'
import { getPublishedProject } from '../../../services/publicContentService.js'
import './Projects.css'

/**
 * A27: public project detail page (GET /api/v1/projects/{slug}, anonymous,
 * published only). Follows the JobDetail pattern: skeleton while loading, a
 * masked not-available state on 404 (unknown slug and non-published project
 * are indistinguishable by design), a retry state on other errors, and a
 * back link to the listing. No fallback record is invented for unknown
 * slugs — the API's 404 semantics are preserved. Only fields that exist on
 * PublicProjectResponse are rendered; objectives is admin-authored JSONB
 * rendered safely as text (no HTML injection surface).
 */
export default function ProjectDetail() {
  const { slug } = useParams()
  const [project, setProject] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setNotFound(false)
    setError(false)
    getPublishedProject(slug)
      .then(data => {
        if (active) setProject(data)
      })
      .catch(err => {
        if (!active) return
        const status = err && err.response && err.response.status
        if (status === 404) {
          // Unknown slug AND non-published project are indistinguishable by design.
          setNotFound(true)
        } else {
          setError(true)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [slug, reloadKey])

  return (
    <div className="projects-page">
      <PageHero
        title={project ? project.title : projectsContent.hero.title}
        subtitle={project ? (project.summary || '') : projectsContent.hero.subtitle}
      />

      <section className="section projects__section">
        <Container>
          <p className="jobs-detail__back">
            <Link to="/projects" className="jobs-detail__back-link">
              <Icon name="link" /> Back to all projects
            </Link>
          </p>

          {loading && (
            <div className="projects__card" aria-hidden="true">
              <div className="jobs__skeleton jobs__skeleton--title" />
              <div className="jobs__skeleton jobs__skeleton--line" />
              <div className="jobs__skeleton jobs__skeleton--line jobs__skeleton--short" />
            </div>
          )}

          {!loading && notFound && (
            <div className="projects__card">
              <h2 className="projects__card-title">This project is not available</h2>
              <p className="projects__card-summary">
                The project you are looking for does not exist or is not currently published.
              </p>
              <p style={{ marginTop: 'var(--spacing-4)' }}>
                <Link to="/projects">
                  <Button variant="primary" size="medium">View all projects</Button>
                </Link>
              </p>
            </div>
          )}

          {!loading && error && (
            <div className="projects__card">
              <h2 className="projects__card-title">Something went wrong</h2>
              <p className="projects__card-summary">
                This project could not be loaded right now. Please try again.
              </p>
              <p style={{ marginTop: 'var(--spacing-4)' }}>
                <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
                  Try again
                </Button>
              </p>
            </div>
          )}

          {!loading && project && (
            <article className="projects__card projects__detail">
              <div className="projects__card-head">
                <h2 className="projects__card-title">{project.title}</h2>
                {project.projectStatus && (
                  <span className={`projects__status projects__status--${project.projectStatus}`}>
                    {project.projectStatus}
                  </span>
                )}
              </div>

              {project.summary && (
                <p className="projects__card-summary">{project.summary}</p>
              )}

              <div className="projects__card-meta">
                {project.category && <span className="projects__chip">{project.category.name}</span>}
                {project.location && (
                  <span className="projects__meta-item">{project.location}</span>
                )}
                {project.startDate && (
                  <span className="projects__meta-item">
                    {project.startDate}{project.endDate ? ` – ${project.endDate}` : ''}
                  </span>
                )}
              </div>

              {project.description && (
                <p className="projects__detail__text">{project.description}</p>
              )}

              {project.objectives && (
                <section className="projects__detail__block">
                  <h3 className="projects__detail__block-title">Objectives</h3>
                  <pre className="projects__detail__objectives">{project.objectives}</pre>
                </section>
              )}

              {project.impactSummary && (
                <p className="projects__impact">
                  <strong>Impact:</strong> {project.impactSummary}
                </p>
              )}
            </article>
          )}
        </Container>
      </section>
    </div>
  )
}
