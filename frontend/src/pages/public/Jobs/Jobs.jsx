import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import EmptyState from '../../../components/EmptyState/EmptyState.jsx'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import Badge from '../../../components/Badge/Badge.jsx'
import Button from '../../../components/Button/Button.jsx'
import { jobsContent } from '../../../constants/careersContent.js'
import { listJobs as listJobsFromApi } from '../../../services/jobService.js'
import './Jobs.css'

const EMPLOYMENT_TYPES = [
  { value: '', label: 'All employment types' },
  { value: 'FULL_TIME', label: 'Full-time' },
  { value: 'PART_TIME', label: 'Part-time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'TEMPORARY', label: 'Temporary' },
]

const WORK_MODES = [
  { value: '', label: 'All work modes' },
  { value: 'ONSITE', label: 'On-site' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
]

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

function PortalCard({ label, description, features }) {
  return (
    <article className="jobs__portal">
      <h2 className="jobs__portal-title">{label}</h2>
      <p className="jobs__portal-desc">{description}</p>
      <ul className="jobs__portal-features">
        {features.map(feature => (
          <li key={feature} className="jobs__portal-feature">
            <svg className="jobs__feature-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <polyline points="20 6 9 17 4 12" />
            </svg>
            {feature}
          </li>
        ))}
      </ul>
    </article>
  )
}

function JobCard({ job }) {
  const salary = formatSalary(job)
  const deadline = job.applicationDeadline
  return (
    <article className="jobs__card">
      <div className="jobs__card-head">
        <div>
          <h3 className="jobs__card-title">
            <Link to={`/jobs/${job.id}`} className="jobs__card-link">{job.title}</Link>
          </h3>
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

      <p className="jobs__card-desc">{job.description}</p>

      {(salary || deadline) && (
        <p className="jobs__card-extra">
          {salary && <span>{salary}</span>}
          {deadline && <span>Apply by {deadline}</span>}
        </p>
      )}

      <Link to={`/jobs/${job.id}`} className="jobs__card-cta">
        View details
        <Icon name="link" />
      </Link>
    </article>
  )
}

function JobListSkeleton() {
  return (
    <div className="jobs__list" aria-hidden="true">
      {[0, 1, 2].map(i => (
        <div key={i} className="jobs__card jobs__card--loading">
          <div className="jobs__skeleton jobs__skeleton--title" />
          <div className="jobs__skeleton jobs__skeleton--line" />
          <div className="jobs__skeleton jobs__skeleton--line jobs__skeleton--short" />
        </div>
      ))}
    </div>
  )
}

export default function Jobs() {
  const [jobs, setJobs] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalItems, setTotalItems] = useState(0)
  const [search, setSearch] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [workMode, setWorkMode] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    listJobsFromApi({ page, size: 9, search, employmentType, workMode })
      .then(data => {
        if (!active) return
        setJobs(data.items)
        setTotalPages(Math.max(data.totalPages, 1))
        setTotalItems(data.totalItems)
      })
      .catch(() => {
        if (active) setError('Job openings could not be loaded right now. Please try again.')
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [page, search, employmentType, workMode, reloadKey])

  const applyFilters = (event) => {
    event.preventDefault()
    setPage(0)
    setReloadKey(key => key + 1)
  }

  const clearFilters = () => {
    setSearch('')
    setEmploymentType('')
    setWorkMode('')
    setPage(0)
    setReloadKey(key => key + 1)
  }

  const hasFilters = search || employmentType || workMode

  const goToPage = (nextPage) => {
    setPage(nextPage)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="jobs-page">
      <PageHero
        title={jobsContent.hero.title}
        subtitle={jobsContent.hero.subtitle}
      />

      <section className="section jobs__section">
        <Container>
          <div className="jobs__grid">
            <PortalCard
              label={jobsContent.candidatePortal.label}
              description={jobsContent.candidatePortal.description}
              features={jobsContent.candidatePortal.features}
            />
            <PortalCard
              label={jobsContent.employerPortal.label}
              description={jobsContent.employerPortal.description}
              features={jobsContent.employerPortal.features}
            />
          </div>

          <h2 className="jobs__list-title">Current openings</h2>

          <form className="jobs__filters" onSubmit={applyFilters}>
            <input
              type="search"
              className="jobs__filter-input"
              placeholder="Search by title or location"
              aria-label="Search jobs"
              value={search}
              onChange={event => setSearch(event.target.value)}
            />
            <select
              className="jobs__filter-select"
              aria-label="Employment type"
              value={employmentType}
              onChange={event => setEmploymentType(event.target.value)}
            >
              {EMPLOYMENT_TYPES.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
            <select
              className="jobs__filter-select"
              aria-label="Work mode"
              value={workMode}
              onChange={event => setWorkMode(event.target.value)}
            >
              {WORK_MODES.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
            <Button type="submit" variant="primary" size="medium">Search</Button>
            {hasFilters && (
              <Button type="button" variant="outline" size="medium" onClick={clearFilters}>
                Clear
              </Button>
            )}
          </form>

          {loading && <JobListSkeleton />}

          {!loading && error && (
            <EmptyState
              icon={<Icon name="clock" />}
              title="Something went wrong"
              text={error}
              actionLabel="Try again"
              onAction={() => setReloadKey(key => key + 1)}
            />
          )}

          {!loading && !error && jobs.length === 0 && (
            <EmptyState
              icon={<Icon name="briefcase" />}
              title={hasFilters ? 'No matching openings' : 'No openings right now'}
              text={hasFilters
                ? 'No published job postings match your filters. Try different keywords or clear the filters.'
                : 'There are no published job postings at the moment. New opportunities are announced here as soon as they open — check back soon.'}
              actionLabel={hasFilters ? 'Clear filters' : undefined}
              onAction={hasFilters ? clearFilters : undefined}
              secondaryLabel="Contact Us"
              secondaryTo="/contact"
            />
          )}

          {!loading && !error && jobs.length > 0 && (
            <>
              <p className="jobs__count">
                {totalItems} open position{totalItems === 1 ? '' : 's'}
              </p>
              <div className="jobs__list">
                {jobs.map(job => <JobCard key={job.id} job={job} />)}
              </div>
              {totalPages > 1 && (
                <nav className="jobs__pagination" aria-label="Job listings pages">
                  <Button
                    variant="outline"
                    size="small"
                    onClick={() => goToPage(page - 1)}
                    disabled={page === 0}
                    ariaLabel="Previous page"
                  >
                    Previous
                  </Button>
                  <span className="jobs__page-indicator">
                    Page {page + 1} of {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="small"
                    onClick={() => goToPage(page + 1)}
                    disabled={page >= totalPages - 1}
                    ariaLabel="Next page"
                  >
                    Next
                  </Button>
                </nav>
              )}
            </>
          )}
        </Container>
      </section>
    </div>
  )
}
