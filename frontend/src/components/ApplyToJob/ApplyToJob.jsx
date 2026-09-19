import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Button from '../Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import { applyToJob, listMyResumes } from '../../services/candidateService.js'

/**
 * A10 apply-to-job panel for the public job detail page.
 *
 * - Anonymous visitors: signpost to /candidate-login (with returnTo).
 * - Authenticated candidates: optional cover note + active-resume
 *   selection, wired to POST /candidates/me/applications (A9).
 * - Authenticated non-candidates: a neutral notice (the backend would
 *   reject with 404 anyway — this is UX-only).
 * - Duplicate application (backend 409) shows a clear user-facing message.
 */
export default function ApplyToJob({ job }) {
  const { user, isCandidate } = useAuth()
  const navigate = useNavigate()
  const [resumes, setResumes] = useState(null)
  const [resumeId, setResumeId] = useState('')
  const [coverNote, setCoverNote] = useState('')
  const [expanded, setExpanded] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState(null)
  const [isError, setIsError] = useState(false)

  const isClosedJob = job && (job.applicationDeadline)
    ? new Date(job.applicationDeadline) < new Date(new Date().toDateString())
    : false

  useEffect(() => {
    if (!isCandidate || !expanded || resumes) return
    let active = true
    listMyResumes()
      .then(data => {
        if (!active) return
        setResumes(data)
        const activeResume = Array.isArray(data) ? data.find(r => r.active) : null
        if (activeResume) setResumeId(String(activeResume.id))
      })
      .catch(() => {
        if (active) setResumes([])
      })
    return () => {
      active = false
    }
  }, [isCandidate, expanded, resumes])

  if (!user) {
    return (
      <section className="jobs-detail__block">
        <h3 className="jobs-detail__block-title">How to apply</h3>
        <p className="jobs-detail__text">
          Sign in to your candidate account to apply for this role. Don't have an
          account yet? Candidate accounts are provisioned by the foundation office —{' '}
          <Link to="/contact">contact us</Link>.
        </p>
        <Button
          variant="primary"
          size="medium"
          onClick={() => navigate(`/candidate-login?returnTo=${encodeURIComponent(`/jobs/${job.id}`)}`)}
        >
          Sign in to apply
        </Button>
      </section>
    )
  }

  if (!isCandidate) {
    return (
      <section className="jobs-detail__block">
        <h3 className="jobs-detail__block-title">How to apply</h3>
        <p className="jobs-detail__text">
          Applications are submitted from candidate accounts. You are currently
          signed in as {user.userType || 'a staff account'}.
        </p>
      </section>
    )
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitting(true)
    setMessage(null)
    setIsError(false)
    try {
      await applyToJob({
        jobId: job.id,
        resumeId: resumeId ? Number(resumeId) : null,
        coverNote,
      })
      setMessage('Application submitted. Track it under My Applications.')
      setIsError(false)
      setExpanded(false)
      setCoverNote('')
    } catch (error) {
      const status = error && error.response && error.response.status
      const apiErrors = error && error.response && error.response.data
        && error.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null
      setIsError(true)
      if (status === 409) {
        setMessage('You have already applied for this job.')
      } else if (status === 401) {
        setMessage('Your session has expired. Please log in again.')
      } else if (status === 404) {
        setMessage('This job is no longer open for applications.')
      } else {
        setMessage(firstMessage || 'Something went wrong. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <section className="jobs-detail__block">
      <h3 className="jobs-detail__block-title">How to apply</h3>

      {!expanded && !message && (
        <Button variant="primary" size="medium" onClick={() => setExpanded(true)}>
          Apply for this role
        </Button>
      )}

      {isClosedJob && !message && (
        <p className="jobs-detail__text">
          The application deadline for this posting has passed. You can no longer apply.
        </p>
      )}

      {expanded && (
        <form onSubmit={handleSubmit} noValidate>
          <label className="portal-login__label" htmlFor="apply-resume">
            Resume
          </label>
          <select
            id="apply-resume"
            className="candidate-portal__select"
            value={resumeId}
            onChange={event => setResumeId(event.target.value)}
            disabled={submitting}
          >
            <option value="">No resume attached</option>
            {(resumes || []).filter(resume => resume.active).map(resume => (
              <option key={resume.id} value={resume.id}>
                {resume.fileName} (active)
              </option>
            ))}
          </select>
          <p className="portal-login__status-text" style={{ margin: '0.3rem 0 1rem' }}>
            Only your current active resume can be attached. Manage it under{' '}
            <Link to="/candidate/resumes">My Resume</Link>.
          </p>

          <label className="portal-login__label" htmlFor="apply-cover-note">
            Cover note (optional)
          </label>
          <textarea
            id="apply-cover-note"
            className="candidate-portal__textarea"
            value={coverNote}
            onChange={event => setCoverNote(event.target.value)}
            disabled={submitting}
            maxLength={20000}
            rows={5}
          />

          <div className="candidate-portal__actions-row" style={{ marginTop: '1rem' }}>
            <Button variant="primary" size="medium" type="submit" disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit application'}
            </Button>
            <Button variant="ghost" size="medium" disabled={submitting} onClick={() => setExpanded(false)}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {message && (
        <p
          className={isError ? 'candidate-portal__error' : 'candidate-portal__success'}
          role={isError ? 'alert' : 'status'}
        >
          {message}{' '}
          {isError && message.includes('session has expired') && (
            <Link to={`/candidate-login?returnTo=${encodeURIComponent(`/jobs/${job.id}`)}`}>
              Log in
            </Link>
          )}
        </p>
      )}
      {!expanded && !message && !isClosedJob && (
        <p className="portal-login__status-text" style={{ marginTop: '0.5rem' }}>
          Your application goes directly to the foundation's hiring team.
        </p>
      )}
    </section>
  )
}
