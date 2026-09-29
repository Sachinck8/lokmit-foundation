import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listAdminNotifications,
  getAdminNotificationUnreadCount,
  markAdminNotificationRead,
  markAdminNotificationUnread,
} from '../../services/adminService.js'
import {
  describeAdminError,
  ListPagination,
  SectionFeedback,
  formatDay,
} from './adminShared.jsx'

const TYPE_FILTER_OPTIONS = [
  { value: '', label: 'All types' },
  { value: 'APPLICATION', label: 'APPLICATION' },
  { value: 'INTERVIEW', label: 'INTERVIEW' },
  { value: 'SYSTEM', label: 'SYSTEM' },
]

function formatDateTimeFull(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function describeNotificationError(err, fallback) {
  const message = describeAdminError(err, fallback)
  return message
}

/**
 * A23 — Admin notifications over the existing A7.5 endpoints
 * (notifications:manage). Authorization is deliberately recipient-scoped on
 * the backend: this page always shows the signed-in staff member's own
 * in-app notifications — the permission never unlocks anyone else's inbox.
 */
export default function AdminNotifications() {
  const [page, setPage] = useState(0)
  const [type, setType] = useState('')
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [unreadCount, setUnreadCount] = useState(null)
  const [actionPendingId, setActionPendingId] = useState(null)
  const [feedback, setFeedback] = useState({ error: null, success: null })

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listAdminNotifications({ page, size: 20, type, unreadOnly })
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
  }, [page, type, unreadOnly, reloadKey])

  useEffect(() => {
    let active = true
    getAdminNotificationUnreadCount()
      .then(count => {
        if (active) setUnreadCount(count)
      })
      .catch(() => {
        if (active) setUnreadCount(null)
      })
    return () => {
      active = false
    }
  }, [reloadKey])

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  function replaceItem(updated) {
    setData(previous => previous ? {
      ...previous,
      items: previous.items.map(item => (item.id === updated.id ? updated : item)),
    } : previous)
  }

  async function toggleRead(item) {
    if (actionPendingId != null) return
    const isUnread = item.readAt == null
    setActionPendingId(item.id)
    resetFeedback()
    try {
      const updated = isUnread
        ? await markAdminNotificationRead(item.id)
        : await markAdminNotificationUnread(item.id)
      replaceItem(updated)
      try {
        const count = await getAdminNotificationUnreadCount()
        setUnreadCount(count)
      } catch {
        // The badge is best-effort; a failed recount never blocks the row update.
      }
    } catch (err) {
      setFeedback({
        error: describeNotificationError(err, 'The notification could not be updated. Please try again.'),
        success: null,
      })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = (data && data.items) || []

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Notifications</h1>
          <p className="admin-portal__muted">
            Your in-app notifications as a signed-in staff member. Access is
            recipient-scoped: this inbox shows only your own notices, no one
            else's.
          </p>
        </div>
        <div>
          <p className="admin-portal__page-note">
            {unreadCount != null
              ? `${unreadCount} unread notification${unreadCount === 1 ? '' : 's'}`
              : ''}
          </p>
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={event => event.preventDefault()}>
        <label className="admin-portal__filter-field">
          <span>Type</span>
          <select
            className="admin-portal__filter-select"
            value={type}
            onChange={event => {
              setPage(0)
              setType(event.target.value)
            }}
          >
            {TYPE_FILTER_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
        <label className="admin-portal__filter-field">
          <span>Unread only</span>
          <select
            className="admin-portal__filter-select"
            value={unreadOnly ? 'yes' : 'no'}
            onChange={event => {
              setPage(0)
              setUnreadOnly(event.target.value === 'yes')
            }}
          >
            <option value="no">All notifications</option>
            <option value="yes">Unread only</option>
          </select>
        </label>
      </form>

      <SectionFeedback error={feedback.error} success={feedback.success} />

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
            Notifications could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={unreadOnly ? 'No unread notifications' : 'No notifications yet'}
          description={unreadOnly
            ? 'You are all caught up.'
            : 'System and workflow notifications for your account will appear here.'}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Notification</th>
                  <th scope="col">Type</th>
                  <th scope="col">Received</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>
                      <div className="admin-portal__cell-strong">{item.title || '—'}</div>
                      <div className="admin-portal__cell-muted">{item.body || ''}</div>
                      {item.entityType && (
                        <div className="admin-portal__cell-muted">
                          {item.entityType}
                          {item.entityId != null ? ` #${item.entityId}` : ''}
                        </div>
                      )}
                    </td>
                    <td className="admin-portal__cell-muted">{item.type || '—'}</td>
                    <td className="admin-portal__cell-muted">{formatDateTimeFull(item.createdAt)}</td>
                    <td>
                      <span className={`admin-portal__status admin-portal__status--${item.readAt ? 'READ' : 'NEW'}`}>
                        {item.readAt ? 'Read' : 'Unread'}
                      </span>
                    </td>
                    <td>
                      <Button
                        variant="outline"
                        size="small"
                        disabled={actionPendingId != null}
                        onClick={() => toggleRead(item)}
                      >
                        {actionPendingId === item.id
                          ? 'Working…'
                          : item.readAt ? 'Mark unread' : 'Mark read'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <ListPagination
            data={data}
            loading={loading}
            onPage={setter => setPage(setter)}
            noun="notification"
          />

          <p className="admin-portal__page-note">
            The type filter values above are examples — the backend accepts any
            stored type string. Received times are shown in your local time
            zone ({formatDay(new Date().toISOString())} today).
          </p>
        </>
      )}
    </Container>
  )
}
