import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import {
  getApplication,
  getApplicationHistory,
  getApplicationInterviews,
  getCandidateResumes,
  getCandidateSkills,
  getCandidateEducations,
  getCandidateExperiences,
  startReview,
  shortlist,
  decide,
  withdraw,
  downloadResume,
} from '../../services/adminService.js'

const STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  SHORTLISTED: 'Shortlisted',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
}

const INTERVIEW_MODE_LABELS = {
  ONSITE: 'Onsite',
  REMOTE: 'Remote',
  PHONE: 'Phone',
}

/**
 * Backend-authoritative transition availability (mirrors
 * ApplicationService exactly — the UI never invents lifecycle rules):
 *  - start-review:  SUBMITTED → UNDER_REVIEW
 *  - shortlist:     UNDER_REVIEW → SHORTLISTED
 *  - decide:        SUBMITTED/UNDER_REVIEW/SHORTLISTED → HIRED|REJECTED
 *                   (decided states are terminal → 409; WITHDRAWN → 400)
 *  - withdraw:      any pre-decision state → WITHDRAWN (decided → 409)
 */
function availableActions(status) {
  if (status === 'SUBMITTED') {
    return [
      { key: 'start-review', label: 'Start review', kind: 'primary' },
      { key: 'decide-hire', label: 'Mark hired', kind: 'primary', needsConfirm: true },
      { key: 'decide-reject', label: 'Mark rejected', kind: 'outline', needsConfirm: true },
      { key: 'withdraw', label: 'Withdraw', kind: 'ghost', needsConfirm: true },
    ]
  }
  if (status === 'UNDER_REVIEW') {
    return [
      { key: 'shortlist', label: 'Shortlist', kind: 'primary' },
      { key: 'decide-hire', label: 'Mark hired', kind: 'primary', needsConfirm: true },
      { key: 'decide-reject', label: 'Mark rejected', kind: 'outline', needsConfirm: true },
      { key: 'withdraw', label: 'Withdraw', kind: 'ghost', needsConfirm: true },
    ]
  }
  if (status === 'SHORTLISTED') {
    return [
      { key: 'decide-hire', label: 'Mark hired', kind: 'primary', needsConfirm: true },
      { key: 'decide-reject', label: 'Mark rejected', kind: 'outline', needsConfirm: true },
      { key: 'withdraw', label: 'Withdraw', kind: 'ghost', needsConfirm: true },
    ]
  }
  return [] // HIRED / REJECTED / WITHDRAWN are terminal
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

function formatSalary(min, max) {
  if (min == null && max == null) return '—'
  if (min != null && max != null) return `${min} – ${max}`
  return String(min != null ? min : max)
}

function triggerBlobDownload(response, fallbackName) {
  const disposition = response.headers && response.headers['content-disposition']
  let filename = fallbackName
  if (disposition) {
    const starMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
    if (starMatch && starMatch[1]) {
      try {
        filename = decodeURIComponent(starMatch[1])
      } catch {
        filename = fallbackName
      }
    } else {
      const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
      if (plainMatch && plainMatch[1]) filename = plainMatch[1]
    }
  }
  const url = window.URL.createObjectURL(new Blob([response.data]))
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename || 'resume'
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.URL.revokeObjectURL(url)
}

/**
 * A18 Stage 1 — Admin application detail/review page. Assembles the
 * existing backend surfaces: application (with A18 candidate identity
 * enrichment), candidate skills/education/experience (read-only admin
 * endpoints), resumes + secure download, status history timeline,
 * interviews, and the backend-authoritative status transitions.
 */
export default function AdminApplicationDetail() {
  const { applicationId } = useParams()
  const [application, setApplication] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  // Review sections (loaded only once the application is available).
  const [history, setHistory] = useState(null)
  const [interviews, setInterviews] = useState(null)
  const [resumes, setResumes] = useState(null)
  const [skills, setSkills] = useState(null)
  const [educations, setEducations] = useState(null)
  const [experiences, setExperiences] = useState(null)
  const [sectionsError, setSectionsError] = useState(false)

  // Transition state.
  const [pendingAction, setPendingAction] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [actionSuccess, setActionSuccess] = useState(null)

  // Resume download state (per resume id).
  const [downloadingId, setDownloadingId] = useState(null)
  const [downloadError, setDownloadError] = useState(null)

  const loadSections = useCallback(() => {
    if (!application || !applicationId) return
    let active = true
    setSectionsError(false)
    const candidateId = application.candidate && application.candidate.id
    Promise.all([
      getApplicationHistory(applicationId, { page: 0, size: 50 }).catch(() => 'error'),
      getApplicationInterviews(applicationId, { page: 0, size: 50 }).catch(() => 'error'),
      candidateId ? getCandidateResumes(candidateId).catch(() => 'error') : Promise.resolve('error'),
      candidateId ? getCandidateSkills(candidateId).catch(() => 'error') : Promise.resolve('error'),
      candidateId ? getCandidateEducations(candidateId).catch(() => 'error') : Promise.resolve('error'),
      candidateId ? getCandidateExperiences(candidateId).catch(() => 'error') : Promise.resolve('error'),
    ]).then(([h, i, r, s, e, x]) => {
      if (!active) return
      const failed = [h, i, r, s, e, x].some(value => value === 'error')
      setSectionsError(failed)
      if (h !== 'error') setHistory(h)
      if (i !== 'error') setInterviews(i)
      if (r !== 'error') setResumes(r)
      if (s !== 'error') setSkills(s)
      if (e !== 'error') setEducations(e)
      if (x !== 'error') setExperiences(x)
    })
    return () => {
      active = false
    }
  }, [application, applicationId])

  useEffect(() => {
    let active = true
    setLoading(true)
    setNotFound(false)
    setError(false)
    setApplication(null)
    setHistory(null)
    setInterviews(null)
    setResumes(null)
    setSkills(null)
    setEducations(null)
    setExperiences(null)
    getApplication(applicationId)
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

  useEffect(() => {
    return loadSections()
  }, [loadSections])

  async function handleTransition(action) {
    if (pendingAction || !application) return
    setPendingAction(action.key)
    setActionError(null)
    setActionSuccess(null)
    try {
      let updated
      if (action.key === 'start-review') updated = await startReview(application.id)
      else if (action.key === 'shortlist') updated = await shortlist(application.id)
      else if (action.key === 'decide-hire') updated = await decide(application.id, 'HIRED')
      else if (action.key === 'decide-reject') updated = await decide(application.id, 'REJECTED')
      else if (action.key === 'withdraw') updated = await withdraw(application.id)
      setApplication(previous => ({ ...previous, ...updated }))
      setActionSuccess(`Application is now ${STATUS_LABELS[updated.status] || updated.status}.`)
    } catch (err) {
      const status = err && err.response && err.response.status
      const apiErrors = err && err.response && err.response.data
        && err.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null
      if (status === 409) {
        setActionError(firstMessage || 'This application has already been decided or withdrawn.')
      } else if (status === 400) {
        setActionError(firstMessage || 'This transition is not allowed for the current status.')
      } else if (status === 401) {
        setActionError('Your session has expired. Please sign in again.')
      } else if (status === 403) {
        setActionError('You do not have permission to change this application.')
      } else {
        setActionError(firstMessage || 'The status change could not be recorded. Please try again.')
      }
    } finally {
      setPendingAction(null)
    }
  }

  async function handleDownload(resume) {
    if (downloadingId != null) return
    setDownloadingId(resume.id)
    setDownloadError(null)
    try {
      const response = await downloadResume(resume.id)
      triggerBlobDownload(response, resume.fileName || 'resume')
    } catch {
      setDownloadError('The resume could not be downloaded. Your access may have changed — please retry.')
    } finally {
      setDownloadingId(null)
    }
  }

  const actions = application ? availableActions(application.status) : []
  const activeResume = resumes && Array.isArray(resumes.items)
    ? resumes.items.find(item => item.active)
    : null
  const resumeCount = resumes && Array.isArray(resumes.items) ? resumes.items.length : 0

  return (
    <Container>
      <p className="admin-portal__back" style={{ paddingTop: 'var(--spacing-4)' }}>
        <Link to="/admin-panel/applications" className="admin-portal__back-link">
          Back to applications
        </Link>
      </p>

      {loading && (
        <div className="admin-portal__panel" aria-hidden="true">
          <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
        </div>
      )}

      {!loading && notFound && (
        <div className="admin-portal__panel">
          <h1 className="admin-portal__title">Application not found</h1>
          <p className="admin-portal__muted">
            This application does not exist or is no longer available.
          </p>
          <Link to="/admin-panel/applications">
            <Button variant="primary" size="medium">Back to applications</Button>
          </Link>
        </div>
      )}

      {!loading && error && (
        <div className="admin-portal__panel">
          <h1 className="admin-portal__title">Something went wrong</h1>
          <p className="admin-portal__muted">
            This application could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && application && (
        <div className="admin-portal__panel">
          <div className="admin-portal__row">
            <h1 className="admin-portal__title">
              Application #{application.id}
              {application.job ? ` — ${application.job.title}` : ''}
            </h1>
            <span className={`admin-portal__status admin-portal__status--${application.status}`}>
              {STATUS_LABELS[application.status] || application.status}
            </span>
          </div>

          {/* Application ------------------------------------------------ */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Application</h2>
            <dl className="admin-portal__facts">
              <div className="admin-portal__fact">
                <dt>Applied on</dt>
                <dd>{formatDateTime(application.appliedAt)}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Employer</dt>
                <dd>{application.employer ? application.employer.companyName : '—'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Decision recorded</dt>
                <dd>{application.decidedAt ? formatDateTime(application.decidedAt) : 'Pending'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Last updated</dt>
                <dd>{formatDateTime(application.updatedAt)}</dd>
              </div>
            </dl>
            {application.coverNote && (
              <p className="admin-portal__muted" style={{ whiteSpace: 'pre-wrap', marginTop: 'var(--spacing-3, 0.75rem)' }}>
                <strong>Cover note:</strong> {application.coverNote}
              </p>
            )}
            {application.employerNote && (
              <p className="admin-portal__muted" style={{ whiteSpace: 'pre-wrap', marginTop: 'var(--spacing-2, 0.5rem)' }}>
                <strong>Review notes:</strong> {application.employerNote}
              </p>
            )}
          </section>

          {/* Candidate --------------------------------------------------- */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Candidate</h2>
            <dl className="admin-portal__facts">
              <div className="admin-portal__fact">
                <dt>Name</dt>
                <dd>{(application.candidate && application.candidate.candidateName) || '—'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Email</dt>
                <dd>{(application.candidate && application.candidate.candidateEmail) || '—'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Phone</dt>
                <dd>{(application.candidate && application.candidate.phone) || '—'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Location</dt>
                <dd>{(application.candidate && application.candidate.currentLocation) || '—'}</dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Expected salary</dt>
                <dd>
                  {application.candidate
                    ? formatSalary(application.candidate.expectedSalaryMin, application.candidate.expectedSalaryMax)
                    : '—'}
                </dd>
              </div>
              <div className="admin-portal__fact">
                <dt>Availability</dt>
                <dd>{(application.candidate && application.candidate.availability) || '—'}</dd>
              </div>
            </dl>
            {application.candidate && application.candidate.summary && (
              <p className="admin-portal__muted" style={{ whiteSpace: 'pre-wrap', marginTop: 'var(--spacing-3, 0.75rem)' }}>
                {application.candidate.summary}
              </p>
            )}
          </section>

          {/* Skills / education / experience ----------------------------- */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Skills, education &amp; experience</h2>

            {sectionsError && !skills && (
              <p className="admin-portal__muted">
                The candidate profile sections could not be loaded right now.
              </p>
            )}

            <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>Skills</h3>
            {skills && skills.length === 0 && (
              <p className="admin-portal__muted">No skills listed by the candidate.</p>
            )}
            {skills && skills.length > 0 && (
              <p className="admin-portal__muted">
                {skills.map(skill => skill.skillName).join(', ')}
              </p>
            )}

            <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>Education</h3>
            {educations && educations.length === 0 && (
              <p className="admin-portal__muted">No education records added.</p>
            )}
            {educations && educations.length > 0 && (
              <ul className="admin-portal__sublist">
                {educations.map(item => (
                  <li key={item.id}>
                    <span className="admin-portal__subitem-title">
                      {item.degree}{item.fieldOfStudy ? `, ${item.fieldOfStudy}` : ''}
                    </span>{' '}
                    — {item.institution}
                    <span className="admin-portal__subitem-meta">
                      {' '}({item.startYear}
                      {item.endYear ? `–${item.endYear}` : ''}
                      {item.grade ? `, grade ${item.grade}` : ''})
                    </span>
                  </li>
                ))}
              </ul>
            )}

            <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>Experience</h3>
            {experiences && experiences.length === 0 && (
              <p className="admin-portal__muted">No experience records added.</p>
            )}
            {experiences && experiences.length > 0 && (
              <ul className="admin-portal__sublist">
                {experiences.map(item => (
                  <li key={item.id}>
                    <span className="admin-portal__subitem-title">{item.jobTitle}</span>{' '}
                    — {item.companyName}
                    <span className="admin-portal__subitem-meta">
                      {' '}({formatDate(item.startDate)} – {item.endDate ? formatDate(item.endDate) : 'present'})
                    </span>
                    {item.description && (
                      <div className="admin-portal__subitem-meta">{item.description}</div>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </section>

          {/* Resume ------------------------------------------------------ */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Resume</h2>
            {resumeCount === 0 && (
              <p className="admin-portal__muted">The candidate has not uploaded a resume.</p>
            )}
            {resumeCount > 0 && (
              <ul className="admin-portal__sublist">
                {resumes.items.map(item => (
                  <li key={item.id}>
                    <span className="admin-portal__subitem-title">{item.fileName}</span>
                    <span className="admin-portal__subitem-meta">
                      {' '}· {item.active ? 'Active' : 'Previous'} · uploaded {formatDateTime(item.createdAt)}
                    </span>{' '}
                    <Button
                      variant="outline"
                      size="small"
                      onClick={() => handleDownload(item)}
                      disabled={downloadingId != null}
                    >
                      {downloadingId === item.id ? 'Downloading…' : 'Download'}
                    </Button>
                  </li>
                ))}
              </ul>
            )}
            {downloadError && (
              <p className="admin-portal__error" role="alert">{downloadError}</p>
            )}
            {application.resumeId != null && activeResume && application.resumeId !== activeResume.id && (
              <p className="admin-portal__muted" style={{ marginTop: 'var(--spacing-2, 0.5rem)' }}>
                This application references resume #{application.resumeId} (not the currently active one).
              </p>
            )}
          </section>

          {/* Status transitions ------------------------------------------ */}
          {actions.length > 0 && (
            <section className="admin-portal__section">
              <h2 className="admin-portal__panel-title">Review actions</h2>
              <p className="admin-portal__muted">
                Transitions follow the platform's fixed lifecycle. Decisions and
                withdrawal are permanent and notify the candidate automatically.
              </p>
              <div className="admin-portal__actions-row">
                {actions.map(action => (
                  <Button
                    key={action.key}
                    variant={action.kind}
                    size="medium"
                    disabled={pendingAction != null}
                    onClick={() => {
                      if (action.needsConfirm) {
                        const confirmed = window.confirm(
                          `Are you sure you want to "${action.label}" this application? This is permanent.`
                        )
                        if (!confirmed) return
                      }
                      handleTransition(action)
                    }}
                  >
                    {pendingAction === action.key ? 'Working…' : action.label}
                  </Button>
                ))}
              </div>
              {actionError && (
                <p className="admin-portal__error" role="alert">{actionError}</p>
              )}
              {actionSuccess && (
                <p className="admin-portal__muted" role="status">{actionSuccess}</p>
              )}
            </section>
          )}

          {/* Status history ---------------------------------------------- */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Status history</h2>
            {history && Array.isArray(history.items) && history.items.length === 0 && (
              <p className="admin-portal__muted">No status changes recorded yet.</p>
            )}
            {history && Array.isArray(history.items) && history.items.length > 0 && (
              <ol className="admin-portal__timeline">
                {history.items.map((entry, index) => (
                  <li key={entry.id != null ? entry.id : index} className="admin-portal__timeline-item">
                    <div className="admin-portal__timeline-marker" aria-hidden="true" />
                    <div>
                      <p className="admin-portal__timeline-transition">
                        <span className={`admin-portal__status admin-portal__status--${entry.newStatus}`}>
                          {STATUS_LABELS[entry.newStatus] || entry.newStatus}
                        </span>
                        {entry.previousStatus && (
                          <span className="admin-portal__timeline-from">
                            from {STATUS_LABELS[entry.previousStatus] || entry.previousStatus}
                          </span>
                        )}
                      </p>
                      <p className="admin-portal__timeline-date">
                        {formatDateTime(entry.changedAt)}
                        {entry.note ? ` · ${entry.note}` : ''}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </section>

          {/* Interviews --------------------------------------------------- */}
          <section className="admin-portal__section">
            <h2 className="admin-portal__panel-title">Interviews</h2>
            {interviews && Array.isArray(interviews.items) && interviews.items.length === 0 && (
              <p className="admin-portal__muted">No interviews scheduled for this application.</p>
            )}
            {interviews && Array.isArray(interviews.items) && interviews.items.length > 0 && (
              <ul className="admin-portal__sublist">
                {interviews.items.map(item => (
                  <li key={item.id}>
                    <span className="admin-portal__subitem-title">
                      {formatDateTime(item.scheduledAt)}
                    </span>{' '}
                    <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                      {item.status}
                    </span>
                    <span className="admin-portal__subitem-meta">
                      {' '}· {INTERVIEW_MODE_LABELS[item.mode] || item.mode}
                      {item.location ? ` · ${item.location}` : ''}
                      {item.notes ? ` · ${item.notes}` : ''}
                    </span>
                  </li>
                ))}
              </ul>
            )}
            <p className="admin-portal__muted" style={{ marginTop: 'var(--spacing-2, 0.5rem)' }}>
              Interview scheduling and management are performed through the
              existing interview APIs.
            </p>
          </section>
        </div>
      )}
    </Container>
  )
}
