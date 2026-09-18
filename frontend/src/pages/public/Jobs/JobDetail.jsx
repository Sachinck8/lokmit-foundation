import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import Badge from '../../../components/Badge/Badge.jsx'
import Button from '../../../components/Button/Button.jsx'
import { jobsContent } from '../../../constants/careersContent.js'
import { getJob as getJobFromApi } from '../../../services/jobService.js'
import './Jobs.css'

/** Formats one job's salary range from fields the backend actually models. */
function formatSalary(job) {
  if (!job.salaryMin && !job.salaryMax) return null
  const currency = job.salaryCurrency || ''
  const min = job.salaryMin ? Number(job.salaryMin).toLocaleString('en-IN') : null
  const max = job.salaryMax ? Number(job.salaryMax).toLocaleString('en-IN') : null
  if (min && max) return `${currency} ${min} – ${max} per month`
  return `${currency} ${min || max} per month`
}

/** Maps a backend employment type / work mode code to a human label. */
function humanize(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('-')
}

/** Formats the ISO published timestamp as a plain date, when present. */
function formatPublishedAt(value) {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' })
}

export default function JobDetail() {
  const { jobId } = useParams()
  const [job, setJob] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setNotFound(false)
    setError(false)
    getJobFromApi(jobId)
      .then(data => {
        if (active) setJob(data)
      })
      .catch(err => {
        if (!active) return
        const status = err && err.response && err.response.status
        if (status === 404) {
          // Unknown id AND non-published id are indistinguishable by design.
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
  }, [jobId, reloadKey])

  const salary = job ? formatSalary(job) : null
  const publishedLabel = job ? formatPublishedAt(job.publishedAt) : null

  return (
    <div className="jobs-page">
      <PageHero
        title={job ? job.title : jobsContent.hero.title}
        subtitle={job
          ? `Published opening${job.companyName ? ` at ${job.companyName}` : ''}`
          : jobsContent.hero.subtitle}
      />

      <section className="section jobs__section">
        <Container>
          <p className="jobs-detail__back">
            <Link to="/jobs" className="jobs-detail__back-link">
              <Icon name="link" /> Back to all openings
            </Link>
          </p>

          {loading && (
            <div className="jobs__card jobs__card--loading" aria-hidden="true">
              <div className="jobs__skeleton jobs__skeleton--title" />
              <div className="jobs__skeleton jobs__skeleton--line" />
              <div className="jobs__skeleton jobs__skeleton--line jobs__skeleton--short" />
            </div>
          )}

          {!loading && notFound && (
            <div className="jobs__detail-state">
              <h2 className="jobs-detail__back" style={{ margin: 0 }}>This opening is not available</h2>
              <p className="jobs-detail__state-text">
                The job posting you are looking for does not exist, is no longer open,
                or was never published.
              </p>
              <Link to="/jobs">
                <Button variant="primary" size="medium">Browse all openings</Button>
              </Link>
            </div>
          )}

          {!loading && error && (
            <div className="jobs__detail-state">
              <h2 className="jobs-detail__back" style={{ margin: 0 }}>Something went wrong</h2>
              <p className="jobs-detail__state-text">
                This job could not be loaded right now. Please try again.
              </p>
              <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
                Try again
              </Button>
            </div>
          )}

          {!loading && job && (
            <article className="jobs__card jobs-detail">
              <div className="jobs__card-head">
                <div>
                  <h2 className="jobs-detail__title">{job.title}</h2>
                  {job.companyName && (
                    <p className="jobs__card-company">{job.companyName}</p>
                  )}
                </div>
                {job.category && <Badge label={job.category.name} variant="default" />}
              </div>

              <p className="jobs__card-meta">
                {job.workLocation && (
                  <span className="jobs__meta-item">
                    <Icon name="mapPin" /> {job.workLocation}
                  </span>
                )}
                {job.employmentType && (
                  <span className="jobs__meta-item">
                    <Icon name="briefcase" /> {humanize(job.employmentType)}
                  </span>
                )}
                {job.workMode && (
                  <span className="jobs__meta-item">
                    <Icon name="compass" /> {humanize(job.workMode)}
                  </span>
                )}
              </p>

              <dl className="jobs-detail__facts">
                {salary && (
                  <div className="jobs-detail__fact">
                    <dt>Salary</dt>
                    <dd>{salary}</dd>
                  </div>
                )}
                {job.openings != null && (
                  <div className="jobs-detail__fact">
                    <dt>Openings</dt>
                    <dd>{job.openings}</dd>
                  </div>
                )}
                {job.applicationDeadline && (
                  <div className="jobs-detail__fact">
                    <dt>Apply by</dt>
                    <dd>{job.applicationDeadline}</dd>
                  </div>
                )}
                {publishedLabel && (
                  <div className="jobs-detail__fact">
                    <dt>Published</dt>
                    <dd>{publishedLabel}</dd>
                  </div>
                )}
              </dl>

              <section className="jobs-detail__block">
                <h3 className="jobs-detail__block-title">
                  <Icon name="document" /> About this role
                </h3>
                <p className="jobs-detail__text">{job.description}</p>
              </section>

              {job.requirements && (
                <section className="jobs-detail__block">
                  <h3 className="jobs-detail__block-title">
                    <Icon name="check" /> Requirements
                  </h3>
                  <p className="jobs-detail__text">{job.requirements}</p>
                </section>
              )}

              {job.skills && job.skills.length > 0 && (
                <section className="jobs-detail__block">
                  <h3 className="jobs-detail__block-title">
                    <Icon name="briefcase" /> Skills
                  </h3>
                  <ul className="jobs-detail__skills">
                    {job.skills.map(skill => (
                      <li key={skill.id} className="jobs-detail__skill">{skill.name}</li>
                    ))}
                  </ul>
                </section>
              )}

              <p className="jobs-detail__apply-note">
                Interested in this role? Send your application enquiry through our{' '}
                <Link to="/contact" className="jobs-detail__back-link">contact page</Link>.
              </p>
            </article>
          )}
        </Container>
      </section>
    </div>
  )
}
