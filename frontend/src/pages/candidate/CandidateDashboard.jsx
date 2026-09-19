import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import { getMyProfile, listMyApplications, listMyResumes } from '../../services/candidateService.js'
import { candidateContent } from '../../constants/candidateContent.js'
import '../public/Jobs/Jobs.css'

function friendlyApplicationStatus(code) {
  return candidateContent.applicationStatus[code] || code
}

/**
 * A10 candidate dashboard — everything shown comes from the real backend
 * (profile identity, application count, two most recent applications,
 * active resume status). No invented statistics; missing data shows its
 * empty state instead of placeholder numbers.
 */
export default function CandidateDashboard() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [applications, setApplications] = useState(null)
  const [resumes, setResumes] = useState(null)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setError(false)
    Promise.all([
      getMyProfile().catch(err => ({ __error: err })),
      listMyApplications({ page: 0, size: 5 }).catch(err => ({ __error: err })),
      listMyResumes().catch(err => ({ __error: err })),
    ]).then(([profileResult, applicationsResult, resumesResult]) => {
      if (!active) return
      if (profileResult.__error) {
        setError(true)
      } else {
        setProfile(profileResult)
      }
      if (!applicationsResult.__error) setApplications(applicationsResult)
      if (!resumesResult.__error) setResumes(resumesResult)
    })
    return () => {
      active = false
    }
  }, [reloadKey])

  const activeResume = Array.isArray(resumes)
    ? resumes.find(resume => resume.active)
    : null
  const totalApplications = applications ? applications.totalItems : null
  const recentApplications = applications ? applications.items.slice(0, 2) : []

  return (
    <Container>
      <p className="jobs-detail__back" style={{ paddingTop: 'var(--spacing-4)' }}>
        <Link to="/jobs" className="jobs-detail__back-link">Browse public openings</Link>
      </p>

      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">
          Welcome{user && user.fullName ? `, ${user.fullName}` : ''}
        </h1>
        <p className="candidate-portal__muted">
          Your candidate account with LOKMIT FOUNDATION. All information below comes
          from your live candidate record.
        </p>
      </div>

      {error && (
        <div className="candidate-portal__panel">
          <p className="candidate-portal__error" role="alert">
            We could not load your dashboard right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      <div className="candidate-portal__grid">
        <div>
          <div className="candidate-portal__panel">
            <h2 className="candidate-portal__panel-title">Recent applications</h2>
            {applications === null && !error && (
              <div aria-hidden="true">
                <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
                <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
                <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
              </div>
            )}
            {applications && applications.items.length === 0 && (
              <p className="candidate-portal__muted">
                {candidateContent.empty.applications.text}{' '}
                <Link to="/jobs">Browse jobs</Link> to apply.
              </p>
            )}
            {applications && applications.items.length > 0 && (
              <>
                <p className="candidate-portal__muted">
                  You have {totalApplications === 1 ? '1 application' : `${totalApplications} applications`} in total.
                </p>
                <ul className="candidate-portal__list">
                  {recentApplications.map(app => (
                    <li key={app.id} className="candidate-portal__list-item">
                      <div className="candidate-portal__row">
                        <div>
                          <strong>{app.job ? app.job.title : 'Application'}</strong>
                          <div className="candidate-portal__muted">
                            Applied {app.appliedAt ? new Date(app.appliedAt).toLocaleDateString('en-IN') : ''}
                          </div>
                        </div>
                        <span className={`candidate-portal__status candidate-portal__status--${app.status}`}>
                          {friendlyApplicationStatus(app.status)}
                        </span>
                      </div>
                    </li>
                  ))}
                </ul>
                <p style={{ marginTop: 'var(--spacing-3)' }}>
                  <Link to="/candidate/applications">View all applications</Link>
                </p>
              </>
            )}
          </div>
        </div>

        <div>
          <div className="candidate-portal__panel">
            <h2 className="candidate-portal__panel-title">Your resume</h2>
            {resumes === null && !error && (
              <div aria-hidden="true">
                <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
                <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
              </div>
            )}
            {resumes && activeResume && (
              <p className="candidate-portal__muted">
                Active resume: <strong>{activeResume.fileName}</strong>
                {activeResume.fileSizeBytes != null && (
                  <> ({Math.max(1, Math.round(activeResume.fileSizeBytes / 1024))} KB)</>
                )}
              </p>
            )}
            {resumes && !activeResume && (
              <p className="candidate-portal__muted">
                {candidateContent.empty.resumes.text}
              </p>
            )}
            <p>
              <Link to="/candidate/resumes">Manage resume</Link>
            </p>
          </div>

          <div className="candidate-portal__panel">
            <h2 className="candidate-portal__panel-title">Quick actions</h2>
            <ul className="candidate-portal__list">
              <li className="candidate-portal__list-item">
                <Link to="/jobs" className="candidate-portal__nav-link">Browse Jobs</Link>
              </li>
              <li className="candidate-portal__list-item">
                <Link to="/candidate/applications" className="candidate-portal__nav-link">My Applications</Link>
              </li>
              <li className="candidate-portal__list-item">
                <Link to="/candidate/resumes" className="candidate-portal__nav-link">My Resume</Link>
              </li>
              <li className="candidate-portal__list-item">
                <Link to="/candidate/profile" className="candidate-portal__nav-link">Profile</Link>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </Container>
  )
}
