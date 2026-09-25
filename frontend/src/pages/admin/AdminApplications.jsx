import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import { listApplications } from '../../services/adminService.js'

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'SUBMITTED', label: 'Submitted' },
  { value: 'UNDER_REVIEW', label: 'Under review' },
  { value: 'SHORTLISTED', label: 'Shortlisted' },
  { value: 'HIRED', label: 'Hired' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
]

function statusLabel(code) {
  const match = STATUS_OPTIONS.find(option => option.value === code)
  return match ? match.label : code
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
 * A18 Stage 1 — Admin applications list over the existing
 * GET /admin/applications API (backend-side filtering + pagination).
 * Reviewers see candidate identity (A18 enrichment), job, employer,
 * status, applied date and resume availability at a glance, then open the
 * detail/review page. Loading/error/empty states are explicit; a failed
 * load never renders fabricated rows.
 */
export default function AdminApplications() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
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
    listApplications({ page, size: 20, status, search })
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
  }, [page, status, search, reloadKey])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setPage(0)
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Applications</h1>
          <p className="admin-portal__muted">
            Review submitted job applications, open a candidate's profile and
            record status decisions.
          </p>
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search cover/employer notes</span>
          <input
            id="admin-apps-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search applications…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-apps-status"
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
            Applications could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No applications match' : 'No applications yet'}
          description={hasFilters
            ? 'No submitted applications match your filters. Try different keywords or clear the filters.'
            : 'When candidates apply to published jobs, their applications will appear here for review.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Candidate</th>
                  <th scope="col">Job</th>
                  <th scope="col">Employer</th>
                  <th scope="col">Status</th>
                  <th scope="col">Applied</th>
                  <th scope="col">Resume</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr
                    key={item.id}
                    tabIndex={0}
                    onClick={() => navigate(`/admin-panel/applications/${item.id}`)}
                    onKeyDown={event => {
                      if (event.key === 'Enter') {
                        navigate(`/admin-panel/applications/${item.id}`)
                      }
                    }}
                  >
                    <td>
                      <div className="admin-portal__cell-strong">
                        {(item.candidate && item.candidate.candidateName) || '—'}
                      </div>
                      <div className="admin-portal__cell-muted">
                        {(item.candidate && item.candidate.candidateEmail) || ''}
                      </div>
                    </td>
                    <td>{item.job ? item.job.title : '—'}</td>
                    <td>{item.employer ? item.employer.companyName : '—'}</td>
                    <td>
                      <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                        {statusLabel(item.status)}
                      </span>
                    </td>
                    <td className="admin-portal__cell-muted">{formatDate(item.appliedAt)}</td>
                    <td className="admin-portal__cell-muted">
                      {item.resumeId != null ? 'Attached' : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="admin-portal__pagination">
            <span className="admin-portal__page-note">
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalItems} application{data.totalItems === 1 ? '' : 's'}
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
