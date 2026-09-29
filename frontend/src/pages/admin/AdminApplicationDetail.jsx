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
  scheduleInterview,
  updateInterview,
  setInterviewStatus,
  deleteInterview,
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

const INTERVIEW_MODE_OPTIONS = [
  { value: 'ONSITE', label: 'Onsite' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'PHONE', label: 'Phone' },
]

const INTERVIEW_STATUS_LABELS = {
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Completed',
  NO_SHOW: 'No-show',
  CANCELLED: 'Cancelled',
}

/**
 * Backend-authoritative terminal application states for interview purposes
 * (mirrors InterviewService.requireNonTerminalApplication — the backend
 * rejects interview creation on these with 400, and remains authoritative).
 */
const TERMINAL_APPLICATION_STATUSES = ['HIRED', 'REJECTED', 'WITHDRAWN']

/**
 * Backend-authoritative interview status transitions (mirrors
 * InterviewService.validateTransition): only a SCHEDULED interview can
 * move, to any of the three closed states; those are final (409 otherwise).
 */
function interviewStatusActions(status) {
  if (status !== 'SCHEDULED') return []
  return [
    { key: 'COMPLETED', label: 'Complete', kind: 'primary' },
    { key: 'NO_SHOW', label: 'No-show', kind: 'outline' },
    { key: 'CANCELLED', label: 'Cancel', kind: 'ghost' },
  ]
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

/**
 * Converts an ISO-8601 timestamp to the value shape the
 * `<input type="datetime-local">` element expects (local time,
 * no offset). Returns '' for missing/invalid input.
 */
function toDatetimeLocal(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = number => String(number).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
    `T${pad(date.getHours())}:${pad(date.getMinutes())}`
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

  // A20 interview management state.
  const [showScheduleForm, setShowScheduleForm] = useState(false)
  const [scheduleForm, setScheduleForm] = useState({
    scheduledAt: '',
    mode: '',
    location: '',
    notes: '',
  })
  const [scheduling, setScheduling] = useState(false)
  const [scheduleError, setScheduleError] = useState(null)
  const [editingInterviewId, setEditingInterviewId] = useState(null)
  const [editForm, setEditForm] = useState({ scheduledAt: '', mode: '', location: '', notes: '' })
  const [savingInterviewId, setSavingInterviewId] = useState(null)
  const [statusPendingId, setStatusPendingId] = useState(null)
  const [deletingId, setDeletingId] = useState(null)
  const [interviewActionError, setInterviewActionError] = useState(null)
  const [interviewActionSuccess, setInterviewActionSuccess] = useState(null)

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

  // --------------------------------------------------------------- A20
  // Interview management handlers. Each mutation reloads the interviews
  // section from the server afterwards so the UI only ever shows real
  // API state. Errors use the page's existing conventions; no raw
  // backend messages or stack traces are shown.

  function resetInterviewFeedback() {
    setInterviewActionError(null)
    setInterviewActionSuccess(null)
  }

  function describeInterviewError(err, fallback) {
    const status = err && err.response && err.response.status
    const apiErrors = err && err.response && err.response.data && err.response.data.errors
    const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
      ? apiErrors[0].message
      : null
    if (status === 400) return firstMessage || 'Invalid interview operation.'
    if (status === 409) {
      return firstMessage || 'The interview cannot be changed in its current state.'
    }
    if (status === 404) return 'Interview or application not found.'
    if (status === 403) return 'You do not have permission to manage interviews.'
    if (status === 401) return 'Your session has expired. Please sign in again.'
    return firstMessage || fallback
  }

  function reloadInterviews() {
    return getApplicationInterviews(applicationId, { page: 0, size: 50 })
      .then(data => setInterviews(data))
      .catch(() => {
        // Keep the last known list on screen; the action error already
        // tells the reviewer the latest state could not be confirmed.
      })
  }

  async function handleSchedule(event) {
    event.preventDefault()
    if (scheduling || !canMutateInterviews) return
    setScheduling(true)
    setScheduleError(null)
    resetInterviewFeedback()
    try {
      await scheduleInterview(applicationId, {
        // datetime-local gives an offset-less local string; convert it to a
        // real ISO-8601 instant so the backend OffsetDateTime parses it.
        scheduledAt: new Date(scheduleForm.scheduledAt).toISOString(),
        mode: scheduleForm.mode,
        location: scheduleForm.location.trim() === '' ? undefined : scheduleForm.location.trim(),
        notes: scheduleForm.notes.trim() === '' ? undefined : scheduleForm.notes.trim(),
      })
      await reloadInterviews()
      setScheduleForm({ scheduledAt: '', mode: '', location: '', notes: '' })
      setShowScheduleForm(false)
      setInterviewActionSuccess('Interview scheduled.')
    } catch (err) {
      setScheduleError(describeInterviewError(err, 'The interview could not be scheduled. Please try again.'))
    } finally {
      setScheduling(false)
    }
  }

  function startEditing(interview) {
    resetInterviewFeedback()
    setScheduleError(null)
    setShowScheduleForm(false)
    setEditingInterviewId(interview.id)
    setEditForm({
      scheduledAt: toDatetimeLocal(interview.scheduledAt),
      mode: interview.mode || '',
      location: interview.location || '',
      notes: interview.notes || '',
    })
  }

  function cancelEditing() {
    setEditingInterviewId(null)
    setEditForm({ scheduledAt: '', mode: '', location: '', notes: '' })
  }

  async function handleUpdate(event) {
    event.preventDefault()
    if (savingInterviewId != null || editingInterviewId == null) return
    setSavingInterviewId(editingInterviewId)
    setInterviewActionError(null)
    setInterviewActionSuccess(null)
    try {
      await updateInterview(applicationId, editingInterviewId, {
        scheduledAt: editForm.scheduledAt ? new Date(editForm.scheduledAt).toISOString() : undefined,
        mode: editForm.mode || undefined,
        location: editForm.location.trim() === '' ? null : editForm.location.trim(),
        notes: editForm.notes.trim() === '' ? null : editForm.notes.trim(),
      })
      await reloadInterviews()
      cancelEditing()
      setInterviewActionSuccess('Interview updated.')
    } catch (err) {
      setInterviewActionError(describeInterviewError(err, 'The interview could not be updated. Please try again.'))
    } finally {
      setSavingInterviewId(null)
    }
  }

  async function handleInterviewStatus(interview, nextStatus) {
    if (statusPendingId != null) return
    setStatusPendingId(interview.id)
    resetInterviewFeedback()
    try {
      await setInterviewStatus(applicationId, interview.id, nextStatus)
      await reloadInterviews()
      setInterviewActionSuccess(`Interview marked ${INTERVIEW_STATUS_LABELS[nextStatus] || nextStatus}.`)
    } catch (err) {
      setInterviewActionError(describeInterviewError(err, 'The interview status could not be changed. Please try again.'))
    } finally {
      setStatusPendingId(null)
    }
  }

  async function handleDeleteInterview(interview) {
    if (deletingId != null) return
    const confirmed = window.confirm(
      'Delete this cancelled interview permanently? This cannot be undone.'
    )
    if (!confirmed) return
    setDeletingId(interview.id)
    resetInterviewFeedback()
    try {
      await deleteInterview(applicationId, interview.id)
      await reloadInterviews()
      setInterviewActionSuccess('Cancelled interview deleted.')
    } catch (err) {
      setInterviewActionError(describeInterviewError(err, 'The interview could not be deleted. Please try again.'))
    } finally {
      setDeletingId(null)
    }
  }

  const actions = application ? availableActions(application.status) : []
  const activeResume = resumes && Array.isArray(resumes.items)
    ? resumes.items.find(item => item.active)
    : null
  const resumeCount = resumes && Array.isArray(resumes.items) ? resumes.items.length : 0

  /**
   * Interview mutations are offered only while the application is
   * non-terminal (the backend rejects interview creation on HIRED/REJECTED/
   * WITHDRAWN with 400 and remains authoritative — this is UX mirroring,
   * not a security control).
   */
  const canMutateInterviews = Boolean(
    application && !TERMINAL_APPLICATION_STATUSES.includes(application.status)
  )

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

            {canMutateInterviews ? (
              <div className="admin-portal__actions-row">
                <Button
                  variant="primary"
                  size="medium"
                  disabled={scheduling}
                  onClick={() => {
                    resetInterviewFeedback()
                    setScheduleError(null)
                    cancelEditing()
                    setShowScheduleForm(value => !value)
                  }}
                >
                  {showScheduleForm ? 'Close form' : 'Schedule interview'}
                </Button>
              </div>
            ) : (
              <p className="admin-portal__muted">
                This application is {STATUS_LABELS[application.status] || application.status},
                so no further interviews can be scheduled. Existing interviews
                remain for reference.
              </p>
            )}

            {showScheduleForm && canMutateInterviews && (
              <form onSubmit={handleSchedule} style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                <div className="admin-portal__facts">
                  <label className="admin-portal__filter-field">
                    <span>Date &amp; time *</span>
                    <input
                      className="admin-portal__filter-input"
                      type="datetime-local"
                      value={scheduleForm.scheduledAt}
                      onChange={event => setScheduleForm(previous => ({
                        ...previous,
                        scheduledAt: event.target.value,
                      }))}
                      required
                    />
                  </label>
                  <label className="admin-portal__filter-field">
                    <span>Mode *</span>
                    <select
                      className="admin-portal__filter-select"
                      value={scheduleForm.mode}
                      onChange={event => setScheduleForm(previous => ({
                        ...previous,
                        mode: event.target.value,
                      }))}
                      required
                    >
                      <option value="">Select a mode…</option>
                      {INTERVIEW_MODE_OPTIONS.map(option => (
                        <option key={option.value} value={option.value}>{option.label}</option>
                      ))}
                    </select>
                  </label>
                  <label className="admin-portal__filter-field">
                    <span>Location / meeting link</span>
                    <input
                      className="admin-portal__filter-input"
                      type="text"
                      maxLength={255}
                      value={scheduleForm.location}
                      onChange={event => setScheduleForm(previous => ({
                        ...previous,
                        location: event.target.value,
                      }))}
                    />
                  </label>
                  <label className="admin-portal__filter-field">
                    <span>Notes</span>
                    <textarea
                      className="admin-portal__filter-input"
                      rows={3}
                      value={scheduleForm.notes}
                      onChange={event => setScheduleForm(previous => ({
                        ...previous,
                        notes: event.target.value,
                      }))}
                    />
                  </label>
                </div>
                {scheduleError && (
                  <p className="admin-portal__error" role="alert">{scheduleError}</p>
                )}
                <div className="admin-portal__actions-row">
                  <Button type="submit" variant="primary" size="medium" disabled={scheduling}>
                    {scheduling ? 'Scheduling…' : 'Schedule interview'}
                  </Button>
                </div>
              </form>
            )}

            {interviews && Array.isArray(interviews.items) && interviews.items.length === 0 && (
              <p className="admin-portal__muted">No interviews scheduled for this application.</p>
            )}
            {interviews && Array.isArray(interviews.items) && interviews.items.length > 0 && (
              <ul className="admin-portal__sublist">
                {interviews.items.map(item => (
                  <li key={item.id}>
                    {editingInterviewId === item.id ? (
                      <form onSubmit={handleUpdate}>
                        <div className="admin-portal__facts">
                          <label className="admin-portal__filter-field">
                            <span>Date &amp; time</span>
                            <input
                              className="admin-portal__filter-input"
                              type="datetime-local"
                              value={editForm.scheduledAt}
                              onChange={event => setEditForm(previous => ({
                                ...previous,
                                scheduledAt: event.target.value,
                              }))}
                            />
                          </label>
                          <label className="admin-portal__filter-field">
                            <span>Mode</span>
                            <select
                              className="admin-portal__filter-select"
                              value={editForm.mode}
                              onChange={event => setEditForm(previous => ({
                                ...previous,
                                mode: event.target.value,
                              }))}
                            >
                              <option value="">Keep current</option>
                              {INTERVIEW_MODE_OPTIONS.map(option => (
                                <option key={option.value} value={option.value}>{option.label}</option>
                              ))}
                            </select>
                          </label>
                          <label className="admin-portal__filter-field">
                            <span>Location / meeting link</span>
                            <input
                              className="admin-portal__filter-input"
                              type="text"
                              maxLength={255}
                              value={editForm.location}
                              onChange={event => setEditForm(previous => ({
                                ...previous,
                                location: event.target.value,
                              }))}
                            />
                          </label>
                          <label className="admin-portal__filter-field">
                            <span>Notes</span>
                            <textarea
                              className="admin-portal__filter-input"
                              rows={3}
                              value={editForm.notes}
                              onChange={event => setEditForm(previous => ({
                                ...previous,
                                notes: event.target.value,
                              }))}
                            />
                          </label>
                        </div>
                        {interviewActionError && (
                          <p className="admin-portal__error" role="alert">{interviewActionError}</p>
                        )}
                        <div className="admin-portal__actions-row">
                          <Button
                            type="submit"
                            variant="primary"
                            size="medium"
                            disabled={savingInterviewId != null}
                          >
                            {savingInterviewId === item.id ? 'Saving…' : 'Save changes'}
                          </Button>
                          <Button
                            variant="ghost"
                            size="medium"
                            disabled={savingInterviewId != null}
                            onClick={cancelEditing}
                          >
                            Cancel editing
                          </Button>
                        </div>
                      </form>
                    ) : (
                      <>
                        <span className="admin-portal__subitem-title">
                          {formatDateTime(item.scheduledAt)}
                        </span>{' '}
                        <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                          {INTERVIEW_STATUS_LABELS[item.status] || item.status}
                        </span>
                        <span className="admin-portal__subitem-meta">
                          {' '}· {INTERVIEW_MODE_LABELS[item.mode] || item.mode}
                          {item.location ? ` · ${item.location}` : ''}
                          {item.notes ? ` · ${item.notes}` : ''}
                        </span>
                        {canMutateInterviews && (
                          <div className="admin-portal__actions-row" style={{ marginTop: 'var(--spacing-2, 0.5rem)' }}>
                            <Button
                              variant="outline"
                              size="small"
                              disabled={
                                statusPendingId != null
                                || deletingId != null
                                || savingInterviewId != null
                              }
                              onClick={() => startEditing(item)}
                            >
                              Edit
                            </Button>
                            {interviewStatusActions(item.status).map(action => (
                              <Button
                                key={action.key}
                                variant={action.kind}
                                size="small"
                                disabled={statusPendingId != null || deletingId != null}
                                onClick={() => handleInterviewStatus(item, action.key)}
                              >
                                {statusPendingId === item.id ? 'Working…' : action.label}
                              </Button>
                            ))}
                            {item.status === 'CANCELLED' && (
                              <Button
                                variant="ghost"
                                size="small"
                                disabled={statusPendingId != null || deletingId != null}
                                onClick={() => handleDeleteInterview(item)}
                              >
                                {deletingId === item.id ? 'Deleting…' : 'Delete'}
                              </Button>
                            )}
                          </div>
                        )}
                      </>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {interviewActionError && editingInterviewId == null && (
              <p className="admin-portal__error" role="alert">{interviewActionError}</p>
            )}
            {interviewActionSuccess && (
              <p className="admin-portal__muted" role="status">{interviewActionSuccess}</p>
            )}
          </section>
        </div>
      )}
    </Container>
  )
}
