import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useOutletContext } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import {
  listMyNotifications,
  markMyNotificationRead,
} from '../../services/candidateService.js'

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function friendlyType(type) {
  switch (type) {
    case 'APPLICATION_STATUS_CHANGED': return 'Application update'
    case 'INTERVIEW_SCHEDULED': return 'Interview scheduled'
    case 'INTERVIEW_UPDATED': return 'Interview updated'
    case 'INTERVIEW_CANCELLED': return 'Interview cancelled'
    default: return type || 'Notification'
  }
}

/**
 * A16: maps a notification's entityType/entityId to a portal route.
 * Returns null for unknown/missing/unsupported entities — such
 * notifications simply stay non-actionable (no crash, no bad route).
 *
 * Values come from the existing outbox generation (OutboxPayloads):
 * "JOB_APPLICATION" -> the candidate's application id,
 * "INTERVIEW"       -> the interview id (read-only interviews page is
 * application-selection based, so it opens the page itself).
 */
function notificationRoute(notification) {
  const id = notification && notification.entityId
  if (typeof id !== 'number' || !Number.isInteger(id) || id <= 0) return null
  switch (notification.entityType) {
    case 'JOB_APPLICATION': return `/candidate/applications/${id}`
    case 'INTERVIEW': return '/candidate/interviews'
    default: return null
  }
}

/**
 * A14 "Notifications" page over GET /candidates/me/notifications (A7.5
 * service, user-scoped by JWT). Read action uses the existing
 * markRead service method; foreign/unknown ids are masked 404s handled as
 * a neutral refresh error. Only real backend data is displayed.
 *
 * A16 makes notifications whose entityType/entityId resolve to a portal
 * route clickable — navigation marks the item read first (existing
 * mark-read behavior), so the candidate lands straight on the entity.
 */
export default function CandidateNotifications() {
  const navigate = useNavigate()
  const outletContext = useOutletContext()
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [markingReadId, setMarkingReadId] = useState(null)
  const actionRequestsRef = useRef(new Set())

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listMyNotifications({ page, size: 10 })
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

  const contextMarkRead = outletContext && typeof outletContext.handleNotificationRead === 'function'
    ? outletContext.handleNotificationRead
    : null

  const items = data ? data.items : []

  function markReadRequest(notification) {
    // Prefer the layout callback so the nav badge stays in sync; fall back
    // to the direct service call when the layout provides none.
    return contextMarkRead
      ? contextMarkRead(notification.id)
      : markMyNotificationRead(notification.id)
  }

  function handleMarkRead(notification) {
    if (markingReadId !== null) return
    setMarkingReadId(notification.id)
    markReadRequest(notification)
      .then(updated => {
        setData(current => current && {
          ...current,
          items: current.items.map(item =>
            item.id === notification.id ? { ...item, ...(updated || {}) } : item),
        })
      })
      .catch(() => setError(true))
      .finally(() => setMarkingReadId(null))
  }

  /**
   * A16: opens the notification's entity route. Unread notifications are
   * marked read first (existing mark-read behavior) — a failed mark-read
   * still navigates; the item simply remains unread when the candidate
   * returns. Unknown/missing/unsupported entities never navigate.
   */
  function handleOpen(notification) {
    const route = notificationRoute(notification)
    if (!route) return
    if (actionRequestsRef.current.has(notification.id)) return
    actionRequestsRef.current.add(notification.id)
    const finish = () => {
      actionRequestsRef.current.delete(notification.id)
      navigate(route)
    }
    if (notification.readAt) {
      finish()
      return
    }
    markReadRequest(notification)
      .then(updated => {
        setData(current => current && {
          ...current,
          items: current.items.map(item =>
            item.id === notification.id ? { ...item, ...(updated || {}) } : item),
        })
      })
      .catch(() => {})
      .finally(finish)
  }

  return (
    <Container>
      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">Notifications</h1>
        <p className="candidate-portal__muted">
          Updates about your applications and interviews. Notifications are
          generated by the foundation's hiring team activity — you cannot
          reply from here.
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
              We could not load your notifications right now. Please try again.
            </p>
            <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
              Try again
            </Button>
          </>
        )}

        {!loading && !error && data && items.length === 0 && (
          <p className="candidate-portal__muted">
            <strong>No notifications yet.</strong>{' '}
            Updates about your applications and interviews will appear here.
          </p>
        )}

        {!loading && !error && data && items.length > 0 && (
          <>
            <ul className="candidate-portal__list">
              {items.map(item => {
                const route = notificationRoute(item)
                return (
                <li
                  key={item.id}
                  className={`candidate-portal__list-item${item.readAt ? '' : ' candidate-portal__list-item--unread'}${route ? ' candidate-portal__list-item--actionable' : ''}`}
                  {...(route ? {
                    role: 'button',
                    tabIndex: 0,
                    'aria-label': `${item.title || friendlyType(item.type)} — open details`,
                    onClick: () => handleOpen(item),
                    onKeyDown: event => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault()
                        handleOpen(item)
                      }
                    },
                  } : {})}
                >
                  <div className="candidate-portal__row">
                    <div>
                      <strong>{item.title || friendlyType(item.type)}</strong>
                      <div className="candidate-portal__muted">{item.body}</div>
                      <div className="candidate-portal__muted candidate-portal__muted--small">
                        {formatDateTime(item.createdAt)}
                      </div>
                      {route && (
                        <div className="candidate-portal__muted candidate-portal__muted--small candidate-portal__action-hint">
                          Click to view details
                        </div>
                      )}
                    </div>
                    {!item.readAt && (
                      <Button
                        variant="outline"
                        size="small"
                        disabled={markingReadId !== null}
                        onClick={event => {
                          event.stopPropagation()
                          handleMarkRead(item)
                        }}
                      >
                        {markingReadId === item.id ? 'Marking…' : 'Mark read'}
                      </Button>
                    )}
                  </div>
                </li>
                )
              })}
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
