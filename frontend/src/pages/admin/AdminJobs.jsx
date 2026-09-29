import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import { listJobs } from '../../services/adminService.js'

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'CLOSED', label: 'Closed' },
  { value: 'ARCHIVED', label: 'Archived' },
]

const TYPE_OPTIONS = [
  { value: '', label: 'All types' },
  { value: 'FULL_TIME', label: 'Full time' },
  { value: 'PART_TIME', label: 'Part time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'TEMPORARY', label: 'Temporary' },
]

const MODE_OPTIONS = [
  { value: '', label: 'All modes' },
  { value: 'ONSITE', label: 'Onsite' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
]

function statusLabel(code) {
  const match = STATUS_OPTIONS.find(option => option.value === code)
  return match ? match.label : code
}

function typeLabel(code) {
  const match = TYPE_OPTIONS.find(option => option.value === code)
  return match ? match.label : code || '—'
}

function modeLabel(code) {
  const match = MODE_OPTIONS.find(option => option.value === code)
  return match ? match.label : code || '—'
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

/**
 * A19 — Admin jobs list over the existing GET /admin/jobs API
 * (backend-side status/employmentType/workMode filters + title/slug/location
 * search + pagination; newest first). Staff manage the full job lifecycle
 * from here: create drafts, publish, close, archive. Loading/error/empty
 * states are explicit; a failed load never renders fabricated rows.
 */
export default function AdminJobs() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [employmentType, setEmploymentType] = useState('')
  const [workMode, setWorkMode] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listJobs({ page, size: 20, status, employmentType, workMode, search })
      .then(result => {
        if (active) setData(result)
      })
      .catch(() => {
        if (active) setError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [page, status, employmentType, workMode, search, reloadKey])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setEmploymentType('')
    setWorkMode('')
    setPage(0)
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || employmentType || workMode || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Jobs</h1>
          <p className="admin-portal__muted">
            Create and manage job postings through their lifecycle: draft,
            publish, close and archive. Published jobs appear on the public
            site.
          </p>
        </div>
        <Button
          variant="primary"
          size="medium"
          onClick={() => navigate('/admin-panel/jobs/new')}
        >
          New job
        </Button>
      </div>

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search title/slug/location</span>
          <input
            id="admin-jobs-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search jobs…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-jobs-status"
            className="admin-portal__filter-select"
            value={status}
            onChange={event => {
              setPage(0)
              setStatus(event.target.value)
            }}
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
        <label className="admin-portal__filter-field">
          <span>Employment type</span>
          <select
            id="admin-jobs-type"
            className="admin-portal__filter-select"
            value={employmentType}
            onChange={event => {
              setPage(0)
              setEmploymentType(event.target.value)
            }}
          >
            {TYPE_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
        <label className="admin-portal__filter-field">
          <span>Work mode</span>
          <select
            id="admin-jobs-mode"
            className="admin-portal__filter-select"
            value={workMode}
            onChange={event => {
              setPage(0)
              setWorkMode(event.target.value)
            }}
          >
            {MODE_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
        <Button type="submit" variant="primary" size="medium">Apply</Button>
        {hasFilters && (
          <Button type="button" variant="ghost" size="medium" onClick={clearFilters}>
            Clear filters
          </Button>
        )}
      </form>

      {loading && (
        <div className="admin-portal__panel" aria-hidden="true">
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
        </div>
      )}

      {!loading && error && (
        <div className="admin-portal__panel">
          <h2 className="admin-portal__panel-title">Something went wrong</h2>
          <p className="admin-portal__muted">
            Jobs could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No jobs match' : 'No jobs yet'}
          description={hasFilters
            ? 'No jobs match your filters. Try different keywords or clear the filters.'
            : 'Create your first job posting to start collecting applications.'}
          actionLabel={hasFilters ? 'Clear filters' : 'New job'}
          onAction={hasFilters ? clearFilters : () => navigate('/admin-panel/jobs/new')}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Job</th>
                  <th scope="col">Employer</th>
                  <th scope="col">Category</th>
                  <th scope="col">Type</th>
                  <th scope="col">Mode</th>
                  <th scope="col">Status</th>
                  <th scope="col">Openings</th>
                  <th scope="col">Deadline</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr
                    key={item.id}
                    tabIndex={0}
                    onClick={() => navigate(`/admin-panel/jobs/${item.id}`)}
                    onKeyDown={event => {
                      if (event.key === 'Enter') {
                        navigate(`/admin-panel/jobs/${item.id}`)
                      }
                    }}
                  >
                    <td>
                      <div className="admin-portal__cell-strong">{item.title || '—'}</div>
                      <div className="admin-portal__cell-muted">{item.workLocation || ''}</div>
                    </td>
                    <td>{item.employer ? item.employer.companyName : '—'}</td>
                    <td>{item.category ? item.category.name : '—'}</td>
                    <td>{typeLabel(item.employmentType)}</td>
                    <td>{modeLabel(item.workMode)}</td>
                    <td>
                      <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td>{item.openings != null ? item.openings : '—'}</td>
                    <td className="admin-portal__cell-muted">{formatDate(item.applicationDeadline)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="admin-portal__pagination">
            <span className="admin-portal__page-note">
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalItems} job{data.totalItems === 1 ? '' : 's'}
            </span>
            <div className="admin-portal__page-actions">
              <Button
                variant="outline"
                size="small"
                disabled={data.page === 0 || loading}
                onClick={() => setPage(current => Math.max(0, current - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="small"
                disabled={data.page + 1 >= data.totalPages || loading}
                onClick={() => setPage(current => current + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </Container>
  )
}
