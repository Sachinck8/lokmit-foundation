import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import {
  listMyApplications,
  listMyInterviews,
} from '../../services/candidateService.js'
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

function friendlyMode(mode) {
  switch (mode) {
    case 'ONSITE': return 'Onsite'
    case 'REMOTE': return 'Remote'
    case 'PHONE': return 'Phone'
    default: return mode || '—'
  }
}

function friendlyInterviewStatus(status) {
  switch (status) {
    case 'SCHEDULED': return 'Scheduled'
    case 'COMPLETED': return 'Completed'
    case 'CANCELLED': return 'Cancelled'
    case 'NO_SHOW': return 'No show'
    default: return status || '—'
  }
}

/**
 * A14 "My Interviews" page — read-only visibility of the interviews
 * scheduled on the candidate's OWN applications. The page first loads the
 * candidate's applications (A9, server-side ownership), then the existing
 * interview repository (A7.4) through the candidate route; ownership flows
 * interview → application → candidate entirely server-side. No creation,
 * editing, cancellation or rescheduling is offered.
 */
export default function CandidateInterviews() {
  const [applications, setApplications] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [interviews, setInterviews] = useState(null)
  const [loading, setLoading] = useState(true)
  const [interviewsLoading, setInterviewsLoading] = useState(false)
  const [error, setError] = useState(false)
  const [interviewsError, setInterviewsError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listMyApplications({ page: 0, size: 50 })
      .then(result => {
        if (!active) return
        setApplications(result.items)
        if (result.items.length > 0) setSelectedId(result.items[0].id)
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
  }, [reloadKey])

  useEffect(() => {
    if (selectedId === null) return undefined
    let active = true
    setInterviewsLoading(true)
    setInterviewsError(false)
    setInterviews(null)
    listMyInterviews(selectedId, { page: 0, size: 50 })
      .then(result => {
        if (active) setInterviews(result.items)
      })
      .catch(() => {
        if (active) setInterviewsError(true)
      })
      .finally(() => {
        if (active) setInterviewsLoading(false)
      })
    return () => {
      active = false
    }
  }, [selectedId, reloadKey])

  const applicationList = applications || []

  return (
    <Container>
      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">My interviews</h1>
        <p className="candidate-portal__muted">
          Interviews scheduled for your applications. Scheduling is managed by
          the foundation's hiring team — contact details for rescheduling
          requests are shared in the notification for each interview.
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
              We could not load your interviews right now. Please try again.
            </p>
            <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
              Try again
            </Button>
          </>
        )}

        {!loading && !error && applications && applicationList.length === 0 && (
          <p className="candidate-portal__muted">
            <strong>No applications yet.</strong>{' '}
            Interviews appear here once the hiring team schedules one for one
            of your applications. <Link to="/jobs">Browse jobs</Link> to get
            started.
          </p>
        )}

        {!loading && !error && applications && applicationList.length > 0 && (
          <>
            <div className="candidate-portal__field">
              <label className="candidate-portal__label" htmlFor="interview-application-select">
                Application
              </label>
              <select
                id="interview-application-select"
                className="candidate-portal__input"
                value={selectedId ?? ''}
                onChange={event => setSelectedId(Number(event.target.value))}
              >
                {applicationList.map(app => (
                  <option key={app.id} value={app.id}>
                    {app.job ? app.job.title : `Application #${app.id}`}
                  </option>
                ))}
              </select>
            </div>

            {interviewsLoading && (
              <div aria-hidden="true">
                <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
                <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
              </div>
            )}

            {!interviewsLoading && interviewsError && (
              <p className="candidate-portal__error" role="alert">
                We could not load the interviews for this application. Please try again.
              </p>
            )}

            {!interviewsLoading && !interviewsError && interviews && interviews.length === 0 && (
              <p className="candidate-portal__muted">
                No interviews have been scheduled for this application yet.
              </p>
            )}

            {!interviewsLoading && !interviewsError && interviews && interviews.length > 0 && (
              <ul className="candidate-portal__list">
                {interviews.map(interview => (
                  <li key={interview.id} className="candidate-portal__list-item">
                    <div className="candidate-portal__row">
                      <div>
                        <strong>{formatDateTime(interview.scheduledAt)}</strong>
                        <div className="candidate-portal__muted">
                          {friendlyMode(interview.mode)}
                          {interview.location ? <> · {interview.location}</> : null}
                        </div>
                        {interview.notes && (
                          <div className="candidate-portal__muted">{interview.notes}</div>
                        )}
                      </div>
                      <span className={`candidate-portal__status candidate-portal__status--${interview.status}`}>
                        {friendlyInterviewStatus(interview.status)}
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}
      </div>
    </Container>
  )
}
