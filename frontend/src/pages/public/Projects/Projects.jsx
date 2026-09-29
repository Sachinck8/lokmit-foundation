import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { projectsContent } from '../../../constants/projectsContent.js'
import { listPublishedProjects, listProjectCategories } from '../../../services/publicContentService.js'
import './Projects.css'

/**
 * A26: live published projects from the public CMS API (GET /api/v1/projects,
 * PUBLISHED only, newest first) with an optional ACTIVE category filter.
 *
 * Fallback contract: while loading and whenever the API fails or returns
 * nothing, the page renders exactly the static content it had before A26 —
 * the public site must never show an error state for content. No invented
 * fields: only what PublicProjectResponse exposes.
 */
export default function Projects() {
  const [projects, setProjects] = useState(null) // null = loading/failed
  const [categories, setCategories] = useState(null)
  const [activeCategory, setActiveCategory] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    setProjects(null)
    Promise.all([
      listPublishedProjects({ page: 0, size: 60, category: activeCategory || undefined }),
      activeCategory ? Promise.resolve(null) : listProjectCategories().catch(() => null),
    ]).then(([projectPage, categoryList]) => {
      if (cancelled) return
      setProjects(projectPage && projectPage.items.length > 0 ? projectPage.items : null)
      if (categoryList) setCategories(categoryList)
    }).catch(() => {
      if (!cancelled) setProjects(null)
    })
    return () => {
      cancelled = true
    }
  }, [activeCategory, reloadKey])

  const hasFilters = Array.isArray(categories) && categories.length > 0

  return (
    <div className="projects-page">
      <PageHero
        title={projectsContent.hero.title}
        subtitle={projectsContent.hero.subtitle}
      />
      <section className="section projects__section">
        <Container>
          {hasFilters && (
            <div className="projects__filters" role="group" aria-label="Filter projects by category">
              <button
                type="button"
                className={`projects__filter${activeCategory === '' ? ' is-active' : ''}`}
                onClick={() => setActiveCategory('')}
              >
                All
              </button>
              {categories.map(category => (
                <button
                  key={category.slug}
                  type="button"
                  className={`projects__filter${activeCategory === category.slug ? ' is-active' : ''}`}
                  onClick={() => setActiveCategory(category.slug)}
                >
                  {category.name}
                </button>
              ))}
            </div>
          )}

          {projects && projects.length > 0 ? (
            <div className="projects__grid">
              {projects.map(project => (
                <article key={project.id} className="projects__card">
                  <div className="projects__card-head">
                    <h3 className="projects__card-title">
                      <Link to={`/projects/${project.slug}`} className="projects__card-link">
                        {project.title}
                      </Link>
                    </h3>
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
                    {project.category && (
                      <span className="projects__chip">{project.category.name}</span>
                    )}
                    {project.location && (
                      <span className="projects__meta-item">{project.location}</span>
                    )}
                    {project.startDate && (
                      <span className="projects__meta-item">
                        {project.startDate}{project.endDate ? ` – ${project.endDate}` : ''}
                      </span>
                    )}
                  </div>
                  {project.impactSummary && (
                    <p className="projects__impact">
                      <strong>Impact:</strong> {project.impactSummary}
                    </p>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <EmptyState
              icon={<Icon name="briefcase" />}
              title={projectsContent.empty.title}
              text={projectsContent.empty.text}
              secondaryLabel="Contact Us"
              secondaryTo="/contact"
            />
          )}
          <p className="projects__note">{projectsContent.note}</p>
        </Container>
      </section>
    </div>
  )
}
