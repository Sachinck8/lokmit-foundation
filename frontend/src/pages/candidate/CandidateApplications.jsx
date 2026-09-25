import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import { listMyApplications } from '../../services/candidateService.js'
import { candidateContent } from '../../constants/candidateContent.js'

function friendlyStatus(code) {
  return candidateContent.applicationStatus[code] || code
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' })
}

/**
 * A10 "My Applications" page over GET /candidates/me/applications (A9).
 * Ownership is enforced server-side; the page renders only the safe
 * candidate DTO (job public identity, status, timestamps).
 */
export default function CandidateApplications() {
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listMyApplications({ page, size: 10 })
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
  }, [page, reloadKey])

  const items = data ? data.items : []

  return (
    <Container>
      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">My applications</h1>
        <p className="candidate-portal__muted">
          Every application you have submitted, with its current status. Status
          changes are made by the foundation's hiring team.
        </p>

        {loading && (
          <div aria-hidden="true">
            <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
            <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
            <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
          </div>
        )}

        {!loading && error && (
          <>
            <p className="candidate-portal__error" role="alert">
              We could not load your applications right now. Please try again.
            </p>
            <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
              Try again
            </Button>
          </>
        )}

        {!loading && !error && data && items.length === 0 && (
          <p className="candidate-portal__muted">
            <strong>{candidateContent.empty.applications.title}.</strong>{' '}
            {candidateContent.empty.applications.text}{' '}
            <Link to="/jobs">Browse jobs</Link> to get started.
          </p>
        )}

        {!loading && !error && data && items.length > 0 && (
          <>
            <ul className="candidate-portal__list">
              {items.map(app => (
                <li key={app.id} className="candidate-portal__list-item">
                  <div className="candidate-portal__row">
                    <div>
                      <Link to={`/candidate/applications/${app.id}`} className="candidate-portal__nav-link">
                        <strong>{app.job ? app.job.title : `Application #${app.id}`}</strong>
                      </Link>
                      <div className="candidate-portal__muted">
                        Applied {formatDate(app.appliedAt)}
                        {app.resumeId != null && <> · Resume attached</>}
                      </div>
                    </div>
                    <span className={`candidate-portal__status candidate-portal__status--${app.status}`}>
                      {friendlyStatus(app.status)}
                    </span>
                  </div>
                </li>
              ))}
            </ul>

            <div className="candidate-portal__row" style={{ marginTop: 'var(--spacing-4)' }}>
              <Button
                variant="outline"
                size="small"
                disabled={page === 0 || loading}
                onClick={() => setPage(current => Math.max(0, current - 1))}
              >
                Previous
              </Button>
              <span className="candidate-portal__muted">
                Page {data.page + 1} of {Math.max(1, data.totalPages)}
              </span>
              <Button
                variant="outline"
                size="small"
                disabled={page + 1 >= data.totalPages || loading}
                onClick={() => setPage(current => current + 1)}
              >
                Next
              </Button>
            </div>
          </>
        )}
      </div>
    </Container>
  )
}
