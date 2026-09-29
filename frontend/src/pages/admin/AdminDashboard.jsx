import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  getDashboardSummary,
  getRecentEnquiries,
  getRecentUsers,
  getRecentApplications,
} from '../../services/adminService.js'

const APPLICATION_STATUS_LABELS = {
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under review',
  SHORTLISTED: 'Shortlisted',
  HIRED: 'Hired',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
}

const ENQUIRY_STATUS_LABELS = {
  NEW: 'New',
  READ: 'Read',
  REPLIED: 'Replied',
  ARCHIVED: 'Archived',
}

const USER_TYPE_LABELS = {
  CANDIDATE: 'Candidate',
  EMPLOYER: 'Employer',
  CLIENT: 'Client',
  STAFF: 'Staff',
}

const USER_STATUS_LABELS = {
  ACTIVE: 'Active',
  LOCKED: 'Locked',
  SUSPENDED: 'Suspended',
  DELETED: 'Deleted',
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function StatusChip({ value, labels }) {
  if (!value) return <span className="admin-portal__cell-muted">—</span>
  return (
    <span className={`admin-portal__status admin-portal__status--${value}`}>
      {labels[value] || value}
    </span>
  )
}

/**
 * A21 — Admin dashboard over the existing read-only A2 APIs
 * (dashboard:view). Every number and row comes from the API; nothing is
 * fabricated or hardcoded. The four activity sections load independently:
 * a failure in one shows a retryable error for that section only and never
 * blocks the others.
 */
export default function AdminDashboard() {
  const [summary, setSummary] = useState(null)
  const [enquiries, setEnquiries] = useState(null)
  const [users, setUsers] = useState(null)
  const [applications, setApplications] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    // Summary is the one required dataset; the three recent-activity lists
    // are independent so one failing list degrades gracefully instead of
    // blanking the whole dashboard.
    Promise.all([
      getDashboardSummary(),
      getRecentEnquiries(5).catch(() => 'error'),
      getRecentUsers(5).catch(() => 'error'),
      getRecentApplications(5).catch(() => 'error'),
    ])
      .then(([s, e, u, a]) => {
        if (!active) return
        setSummary(s)
        setEnquiries(e === 'error' ? null : e)
        setUsers(u === 'error' ? null : u)
        setApplications(a === 'error' ? null : a)
        setError(s == null)
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

  const jobs = summary
    ? [
        { label: 'Published jobs', value: summary.publishedJobs },
        { label: 'Draft jobs', value: summary.draftJobs },
        { label: 'Closed jobs', value: summary.closedJobs },
        { label: 'Archived jobs', value: summary.archivedJobs },
      ]
    : []
  const enquiriesBreakdown = summary
    ? [
        { label: 'New', value: summary.newContactEnquiries },
        { label: 'Read', value: summary.readContactEnquiries },
        { label: 'Replied', value: summary.repliedContactEnquiries },
        { label: 'Archived', value: summary.archivedContactEnquiries },
      ]
    : []

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Dashboard</h1>
          <p className="admin-portal__muted">
            Operational overview of the foundation's platform. All figures are
            live counts from the backend.
          </p>
        </div>
      </div>

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
            The dashboard summary could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && summary && (
        <>
          <section className="admin-portal__section" aria-label="Platform totals">
            <div className="admin-portal__facts">
              <div className="admin-portal__filter-field">
                <span>Total users</span>
                <strong className="admin-dashboard__stat">{summary.totalUsers}</strong>
                <span className="admin-portal__cell-muted">
                  {summary.activeUsers} active · {summary.inactiveUsers} not active
                </span>
              </div>
              <div className="admin-portal__filter-field">
                <span>Candidate profiles</span>
                <strong className="admin-dashboard__stat">{summary.totalCandidates}</strong>
              </div>
              <div className="admin-portal__filter-field">
                <span>Employer profiles</span>
                <strong className="admin-dashboard__stat">{summary.totalEmployers}</strong>
              </div>
              <div className="admin-portal__filter-field">
                <span>Job applications</span>
                <strong className="admin-dashboard__stat">{summary.totalApplications}</strong>
              </div>
              <div className="admin-portal__filter-field">
                <span>Jobs (all statuses)</span>
                <strong className="admin-dashboard__stat">{summary.totalJobs}</strong>
              </div>
              <div className="admin-portal__filter-field">
                <span>New enquiries</span>
                <strong className="admin-dashboard__stat">{summary.newContactEnquiries}</strong>
              </div>
            </div>
          </section>

          <div className="admin-dashboard__columns">
            <section className="admin-portal__section" aria-label="Jobs by lifecycle status">
              <h2 className="admin-portal__panel-title">Jobs by status</h2>
              <ul className="admin-portal__sublist">
                {jobs.map(item => (
                  <li key={item.label}>
                    <span>{item.label}</span>
                    <span className="admin-portal__cell-strong">{item.value}</span>
                  </li>
                ))}
              </ul>
            </section>

            <section className="admin-portal__section" aria-label="Contact enquiries by status">
              <h2 className="admin-portal__panel-title">Contact enquiries</h2>
              <ul className="admin-portal__sublist">
                {enquiriesBreakdown.map(item => (
                  <li key={item.label}>
                    <span>{item.label}</span>
                    <span className="admin-portal__cell-strong">{item.value}</span>
                  </li>
                ))}
              </ul>
            </section>
          </div>

          {applications === null ? (
            <section className="admin-portal__section">
              <h2 className="admin-portal__panel-title">Recent applications</h2>
              <p className="admin-portal__muted" role="status">
                Recent applications could not be loaded right now.{' '}
                <button
                  type="button"
                  className="admin-dashboard__link-button"
                  onClick={() => setReloadKey(key => key + 1)}
                >
                  Retry
                </button>
              </p>
            </section>
          ) : (
            <section className="admin-portal__section">
              <div className="admin-portal__row">
                <h2 className="admin-portal__panel-title">Recent applications</h2>
                <Link to="/admin-panel/applications" className="admin-dashboard__inline-link">
                  Open applications review →
                </Link>
              </div>
              {applications.length === 0 ? (
                <EmptyState
                  title="No applications yet"
                  description="When candidates apply to published jobs, the most recent applications will appear here."
                />
              ) : (
                <div className="admin-portal__table-wrap">
                  <table className="admin-portal__table">
                    <thead>
                      <tr>
                        <th scope="col">Candidate</th>
                        <th scope="col">Job</th>
                        <th scope="col">Status</th>
                        <th scope="col">Applied</th>
                      </tr>
                    </thead>
                    <tbody>
                      {applications.map(item => (
                        <tr key={item.id}>
                          <td>
                            <div className="admin-portal__cell-strong">
                              {item.candidateName || '—'}
                            </div>
                          </td>
                          <td>{item.jobTitle || '—'}</td>
                          <td>
                            <StatusChip value={item.status} labels={APPLICATION_STATUS_LABELS} />
                          </td>
                          <td className="admin-portal__cell-muted">{formatDateTime(item.appliedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {enquiries === null ? (
            <section className="admin-portal__section">
              <h2 className="admin-portal__panel-title">Recent enquiries</h2>
              <p className="admin-portal__muted" role="status">
                Recent enquiries could not be loaded right now.{' '}
                <button
                  type="button"
                  className="admin-dashboard__link-button"
                  onClick={() => setReloadKey(key => key + 1)}
                >
                  Retry
                </button>
              </p>
            </section>
          ) : (
            <section className="admin-portal__section">
              <h2 className="admin-portal__panel-title">Recent enquiries</h2>
              {enquiries.length === 0 ? (
                <EmptyState
                  title="No enquiries yet"
                  description="Messages submitted through the public contact page will appear here."
                />
              ) : (
                <div className="admin-portal__table-wrap">
                  <table className="admin-portal__table">
                    <thead>
                      <tr>
                        <th scope="col">From</th>
                        <th scope="col">Subject</th>
                        <th scope="col">Status</th>
                        <th scope="col">Received</th>
                      </tr>
                    </thead>
                    <tbody>
                      {enquiries.map(item => (
                        <tr key={item.id}>
                          <td>
                            <div className="admin-portal__cell-strong">{item.name || '—'}</div>
                            <div className="admin-portal__cell-muted">{item.email || ''}</div>
                          </td>
                          <td>{item.subject || '—'}</td>
                          <td>
                            <StatusChip value={item.status} labels={ENQUIRY_STATUS_LABELS} />
                          </td>
                          <td className="admin-portal__cell-muted">{formatDateTime(item.createdAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}

          {users === null ? (
            <section className="admin-portal__section">
              <h2 className="admin-portal__panel-title">Recent accounts</h2>
              <p className="admin-portal__muted" role="status">
                Recent accounts could not be loaded right now.{' '}
                <button
                  type="button"
                  className="admin-dashboard__link-button"
                  onClick={() => setReloadKey(key => key + 1)}
                >
                  Retry
                </button>
              </p>
            </section>
          ) : (
            <section className="admin-portal__section">
              <div className="admin-portal__row">
                <h2 className="admin-portal__panel-title">Recent accounts</h2>
                <Link to="/admin-panel/users" className="admin-dashboard__inline-link">
                  Manage users →
                </Link>
              </div>
              {users.length === 0 ? (
                <EmptyState
                  title="No accounts yet"
                  description="Newly created platform accounts will appear here."
                />
              ) : (
                <div className="admin-portal__table-wrap">
                  <table className="admin-portal__table">
                    <thead>
                      <tr>
                        <th scope="col">Account</th>
                        <th scope="col">Type</th>
                        <th scope="col">Status</th>
                        <th scope="col">Created</th>
                        <th scope="col">Last login</th>
                      </tr>
                    </thead>
                    <tbody>
                      {users.map(item => (
                        <tr key={item.id}>
                          <td>
                            <div className="admin-portal__cell-strong">{item.fullName || '—'}</div>
                            <div className="admin-portal__cell-muted">{item.email || ''}</div>
                          </td>
                          <td>{USER_TYPE_LABELS[item.userType] || item.userType || '—'}</td>
                          <td>
                            <StatusChip value={item.status} labels={USER_STATUS_LABELS} />
                          </td>
                          <td className="admin-portal__cell-muted">{formatDate(item.createdAt)}</td>
                          <td className="admin-portal__cell-muted">{formatDateTime(item.lastLoginAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          )}
        </>
      )}
    </Container>
  )
}
