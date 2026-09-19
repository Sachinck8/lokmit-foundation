import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import { getMyApplication } from '../../services/candidateService.js'
import { candidateContent } from '../../constants/candidateContent.js'

function friendlyStatus(code) {
  return candidateContent.applicationStatus[code] || code
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

/**
 * A10 application detail page over GET /candidates/me/applications/{id}
 * (A9). A foreign or unknown id returns the same masked 404; the page
 * shows one neutral "not available" state for both — no existence leak.
 */
export default function CandidateApplicationDetail() {
  const { applicationId } = useParams()
  const [application, setApplication] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setNotFound(false)
    setError(false)
    getMyApplication(applicationId)
      .then(data => {
        if (active) setApplication(data)
      })
      .catch(err => {
        if (!active) return
        const status = err && err.response && err.response.status
        if (status === 404) setNotFound(true)
        else setError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [applicationId, reloadKey])

  return (
    <Container>
      <p className="jobs-detail__back" style={{ paddingTop: 'var(--spacing-4)' }}>
        <Link to="/candidate/applications" className="jobs-detail__back-link">
          Back to my applications
        </Link>
      </p>

      {loading && (
        <div className="candidate-portal__panel" aria-hidden="true">
          <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
        </div>
      )}

      {!loading && notFound && (
        <div className="candidate-portal__panel">
          <h1 className="candidate-portal__title">Application not found</h1>
          <p className="candidate-portal__muted">
            This application does not exist or is not part of your candidate account.
          </p>
          <Link to="/candidate/applications">
            <Button variant="primary" size="medium">Back to my applications</Button>
          </Link>
        </div>
      )}

      {!loading && error && (
        <div className="candidate-portal__panel">
          <h1 className="candidate-portal__title">Something went wrong</h1>
          <p className="candidate-portal__muted">
            This application could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && application && (
        <div className="candidate-portal__panel">
          <div className="candidate-portal__row">
            <h1 className="candidate-portal__title">
              {application.job ? application.job.title : `Application #${application.id}`}
            </h1>
            <span className={`candidate-portal__status candidate-portal__status--${application.status}`}>
              {friendlyStatus(application.status)}
            </span>
          </div>

          <dl className="candidate-portal__facts">
            <div className="candidate-portal__fact">
              <dt>Applied on</dt>
              <dd>{formatDateTime(application.appliedAt)}</dd>
            </div>
            <div className="candidate-portal__fact">
              <dt>Resume</dt>
              <dd>{application.resumeId != null ? 'Attached' : 'Not attached'}</dd>
            </div>
            <div className="candidate-portal__fact">
              <dt>Decision recorded</dt>
              <dd>{application.decidedAt ? formatDateTime(application.decidedAt) : 'Pending'}</dd>
            </div>
            <div className="candidate-portal__fact">
              <dt>Last updated</dt>
              <dd>{formatDateTime(application.updatedAt)}</dd>
            </div>
          </dl>

          {application.job && (
            <p>
              <Link to={`/jobs/${application.job.id}`}>
                View the public job posting
              </Link>
            </p>
          )}

          {application.coverNote && (
            <section>
              <h2 className="candidate-portal__panel-title">Your cover note</h2>
              <p className="candidate-portal__muted" style={{ whiteSpace: 'pre-wrap' }}>
                {application.coverNote}
              </p>
            </section>
          )}
        </div>
      )}
    </Container>
  )
}
