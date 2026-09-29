import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listContactMessages,
  updateContactMessage,
} from '../../services/adminService.js'
import {
  describeAdminError,
  ListPagination,
  SectionFeedback,
  formatDay,
} from './adminShared.jsx'

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'NEW', label: 'New' },
  { value: 'READ', label: 'Read' },
  { value: 'REPLIED', label: 'Replied' },
  { value: 'ARCHIVED', label: 'Archived' },
]

const STATUS_LABELS = {
  NEW: 'New',
  READ: 'Read',
  REPLIED: 'Replied',
  ARCHIVED: 'Archived',
}

const STATUS_ACTIONS = [
  { value: 'READ', label: 'Mark read' },
  { value: 'REPLIED', label: 'Mark replied' },
  { value: 'ARCHIVED', label: 'Archive' },
]

function formatDateTimeLocal(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

/**
 * A23 — Admin contact messages over the existing A7.3 public/contact APIs
 * (messages:manage). Staff can read enquiries, move them through the
 * NEW → READ → REPLIED lifecycle, archive them, and keep an internal note.
 * Sender identity and message content are never editable — the backend
 * PATCH DTO does not even carry those fields.
 */
export default function AdminMessages() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const [openId, setOpenId] = useState(null)
  const [noteDraft, setNoteDraft] = useState('')
  const [saving, setSaving] = useState(false)
  const [actionPendingId, setActionPendingId] = useState(null)
  const [feedback, setFeedback] = useState({ error: null, success: null })

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listContactMessages({ page, size: 20, status, search })
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
  }, [page, status, search, reloadKey])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setPage(0)
  }

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  const hasFilters = Boolean(status || search)

  function replaceItem(updated) {
    setData(previous => previous ? {
      ...previous,
      items: previous.items.map(item => (item.id === updated.id ? updated : item)),
    } : previous)
  }

  function toggleOpen(item) {
    resetFeedback()
    if (openId === item.id) {
      setOpenId(null)
      setNoteDraft('')
      return
    }
    setOpenId(item.id)
    setNoteDraft(item.internalNote || '')
  }

  async function applyStatus(item, nextStatus) {
    if (actionPendingId != null || saving) return
    const needsConfirm = nextStatus === 'ARCHIVED'
    if (needsConfirm) {
      const confirmed = window.confirm(
        `Archive the enquiry from ${item.senderName || item.senderEmail}? It stays in the archive filter.`
      )
      if (!confirmed) return
    }
    setActionPendingId(item.id)
    resetFeedback()
    try {
      const updated = await updateContactMessage(item.id, { status: nextStatus })
      replaceItem(updated)
      setFeedback({
        error: null,
        success: `Enquiry from ${updated.senderName || updated.senderEmail} marked ${STATUS_LABELS[updated.status] || updated.status}.`,
      })
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, 'The status could not be changed. Please try again.'),
        success: null,
      })
    } finally {
      setActionPendingId(null)
    }
  }

  async function saveNote(item) {
    if (saving || actionPendingId != null) return
    setSaving(true)
    resetFeedback()
    try {
      const updated = await updateContactMessage(item.id, {
        internalNote: noteDraft.trim() === '' ? null : noteDraft,
      })
      replaceItem(updated)
      setNoteDraft(updated.internalNote || '')
      setFeedback({
        error: null,
        success: updated.internalNote
          ? 'Internal note saved.'
          : 'Internal note cleared.',
      })
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, 'The note could not be saved. Please try again.'),
        success: null,
      })
    } finally {
      setSaving(false)
    }
  }

  const items = (data && data.items) || []
  const openItem = items.find(item => item.id === openId) || null

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Contact messages</h1>
          <p className="admin-portal__muted">
            Enquiries submitted through the public website contact form. Track
            them through the staff lifecycle and keep internal notes; sender
            details and message text can never be changed.
          </p>
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search</span>
          <input
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search sender, subject or message…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            className="admin-portal__filter-select"
            value={status}
            onChange={event => {
              setPage(0)
              setStatus(event.target.value)
            }}
          >
            {STATUS_OPTIONS.map(option => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </label>
        <Button type="submit" variant="primary" size="medium">Apply</Button>
        {hasFilters && (
          <Button type="button" variant="ghost" size="medium" onClick={clearFilters}>
            Clear filters
          </Button>
        )}
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
            Messages could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No messages match' : 'No enquiries yet'}
          description={hasFilters
            ? 'Nothing matches your filters. Try different keywords or clear the filters.'
            : 'Contact-form submissions will appear here for staff review.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          {openItem && (
            <div className="admin-portal__panel" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
              <div className="admin-portal__row">
                <h2 className="admin-portal__panel-title">
                  Enquiry from {openItem.senderName || openItem.senderEmail}
                </h2>
                <Button variant="ghost" size="small" onClick={() => toggleOpen(openItem)}>
                  Close
                </Button>
              </div>
              <div className="admin-portal__facts">
                <div>
                  <h3 className="admin-portal__subitem-title">Sender</h3>
                  <p className="admin-portal__cell-strong">{openItem.senderName || '—'}</p>
                  <p className="admin-portal__cell-muted">{openItem.senderEmail}</p>
                  <p className="admin-portal__cell-muted">{openItem.senderPhone || 'No phone provided'}</p>
                </div>
                <div>
                  <h3 className="admin-portal__subitem-title">Received</h3>
                  <p className="admin-portal__cell-muted">{formatDateTimeLocal(openItem.createdAt)}</p>
                  <p className="admin-portal__cell-muted">
                    Last updated {formatDateTimeLocal(openItem.updatedAt)}
                  </p>
                  <span className={`admin-portal__status admin-portal__status--${openItem.status}`}>
                    {STATUS_LABELS[openItem.status] || openItem.status}
                  </span>
                </div>
              </div>
              <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                Subject
              </h3>
              <p className="admin-portal__cell-strong">{openItem.subject || '—'}</p>
              <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                Message
              </h3>
              <p style={{ whiteSpace: 'pre-wrap' }}>{openItem.message || '—'}</p>
              <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                Status actions
              </h3>
              <div className="admin-portal__page-actions">
                {STATUS_ACTIONS
                  .filter(action => action.value !== openItem.status)
                  .map(action => (
                    <Button
                      key={action.value}
                      variant={action.value === 'ARCHIVED' ? 'ghost' : 'outline'}
                      size="small"
                      disabled={saving || actionPendingId != null}
                      onClick={() => applyStatus(openItem, action.value)}
                    >
                      {actionPendingId === openItem.id ? 'Working…' : action.label}
                    </Button>
                  ))}
              </div>
              <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                Internal note
              </h3>
              <textarea
                className="admin-portal__filter-input"
                rows={3}
                maxLength={10000}
                placeholder="Staff-only notes (calls made, follow-ups promised…)"
                value={noteDraft}
                disabled={saving || actionPendingId != null}
                onChange={event => setNoteDraft(event.target.value)}
              />
              <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                <Button
                  variant="primary"
                  size="small"
                  disabled={saving || actionPendingId != null}
                  onClick={() => saveNote(openItem)}
                >
                  {saving ? 'Saving…' : 'Save note'}
                </Button>
                <Button
                  variant="ghost"
                  size="small"
                  disabled={saving || actionPendingId != null || !noteDraft}
                  onClick={() => setNoteDraft('')}
                >
                  Clear draft
                </Button>
              </div>
              <p className="admin-portal__page-note">
                Saving an empty note clears it. Notes are staff-only and never
                shown to the sender.
              </p>
            </div>
          )}

          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">From</th>
                  <th scope="col">Subject</th>
                  <th scope="col">Received</th>
                  <th scope="col">Status</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>
                      <div className="admin-portal__cell-strong">{item.senderName || '—'}</div>
                      <div className="admin-portal__cell-muted">{item.senderEmail}</div>
                    </td>
                    <td>
                      <div className="admin-portal__cell-strong">{item.subject || '—'}</div>
                      <div className="admin-portal__cell-muted">
                        {(item.message || '').length > 80
                          ? `${item.message.slice(0, 80)}…`
                          : item.message}
                      </div>
                    </td>
                    <td className="admin-portal__cell-muted">{formatDay(item.createdAt)}</td>
                    <td>
                      <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                        {STATUS_LABELS[item.status] || item.status}
                      </span>
                    </td>
                    <td>
                      <Button
                        variant="outline"
                        size="small"
                        disabled={saving || actionPendingId != null}
                        onClick={() => toggleOpen(item)}
                      >
                        {openId === item.id ? 'Close' : 'Open'}
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
            noun="enquiry"
          />

          <p className="admin-portal__page-note">
            Archiving hides an enquiry from the default queue (use the status
            filter to see it again). The internal note travels with the
            enquiry and is visible to staff only.
          </p>
        </>
      )}
    </Container>
  )
}
