import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import { listAuditLogs } from '../../services/adminService.js'
import {
  ListPagination,
  formatDay,
} from './adminShared.jsx'

const COMMON_ENTITY_TYPES = [
  'USER', 'CANDIDATE', 'EMPLOYER', 'JOB', 'APPLICATION', 'INTERVIEW',
  'RESUME', 'SKILL', 'JOB_CATEGORY', 'WEBSITE_CONTENT', 'SERVICE',
  'EXPERTISE_AREA', 'PROJECT', 'SERVICE_CATEGORY', 'PROJECT_CATEGORY',
  'CONTACT_MESSAGE', 'NOTIFICATION',
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

function prettyJson(details) {
  if (!details) return null
  try {
    const parsed = JSON.parse(details)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return details
  }
}

/**
 * A23 — Read-only audit trail over the existing A7.5 endpoints
 * (users:manage — SUPER_ADMIN only on the backend). Audit rows are
 * append-only and written by backend services: this console deliberately
 * offers filters and viewing only — no create, update or delete exists
 * anywhere in the API.
 */
export default function AuditLogs() {
  const [page, setPage] = useState(0)
  const [actorInput, setActorInput] = useState('')
  const [actorUserId, setActorUserId] = useState('')
  const [entityType, setEntityType] = useState('')
  const [entityIdInput, setEntityIdInput] = useState('')
  const [entityId, setEntityId] = useState('')
  const [actionInput, setActionInput] = useState('')
  const [action, setAction] = useState('')
  const [fromInput, setFromInput] = useState('')
  const [from, setFrom] = useState('')
  const [toInput, setToInput] = useState('')
  const [to, setTo] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)
  const [openId, setOpenId] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listAuditLogs({ page, size: 20, actorUserId, entityType, entityId, action, from, to })
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
  }, [page, actorUserId, entityType, entityId, action, from, to, reloadKey])

  function applyFilters(event) {
    event.preventDefault()
    setPage(0)
    setActorUserId(actorInput.trim())
    setEntityType(entityType.trim())
    setEntityId(entityIdInput.trim())
    setAction(actionInput.trim())
    setFrom(fromInput ? new Date(fromInput).toISOString() : '')
    setTo(toInput ? new Date(toInput).toISOString() : '')
  }

  function clearFilters() {
    setActorInput('')
    setActorUserId('')
    setEntityType('')
    setEntityIdInput('')
    setEntityId('')
    setActionInput('')
    setAction('')
    setFromInput('')
    setFrom('')
    setToInput('')
    setTo('')
    setPage(0)
  }

  const hasFilters = Boolean(actorUserId || entityType || entityId || action || from || to)

  const items = (data && data.items) || []

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Audit logs</h1>
          <p className="admin-portal__muted">
            The immutable trail of administrative actions, written by backend
            services. Filters run server-side; records can only be read here —
            never edited or deleted.
          </p>
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={applyFilters}>
        <label className="admin-portal__filter-field">
          <span>Actor user id</span>
          <input
            type="number"
            min={1}
            className="admin-portal__filter-input"
            placeholder="e.g. 42"
            value={actorInput}
            onChange={event => setActorInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Entity type</span>
          <input
            type="text"
            className="admin-portal__filter-input"
            list="audit-entity-types"
            placeholder="e.g. APPLICATION"
            value={entityType}
            onChange={event => setEntityType(event.target.value)}
          />
          <datalist id="audit-entity-types">
            {COMMON_ENTITY_TYPES.map(name => (
              <option key={name} value={name} />
            ))}
          </datalist>
        </label>
        <label className="admin-portal__filter-field">
          <span>Entity id</span>
          <input
            type="number"
            min={1}
            className="admin-portal__filter-input"
            placeholder="e.g. 17"
            value={entityIdInput}
            onChange={event => setEntityIdInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Action</span>
          <input
            type="text"
            className="admin-portal__filter-input"
            placeholder="e.g. CREATE"
            value={actionInput}
            onChange={event => setActionInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>From</span>
          <input
            type="datetime-local"
            className="admin-portal__filter-input"
            value={fromInput}
            onChange={event => setFromInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>To</span>
          <input
            type="datetime-local"
            className="admin-portal__filter-input"
            value={toInput}
            onChange={event => setToInput(event.target.value)}
          />
        </label>
        <Button type="submit" variant="primary" size="medium">Apply</Button>
        {hasFilters && (
          <Button type="button" variant="ghost" size="medium" onClick={clearFilters}>
            Clear filters
          </Button>
        )}
      </form>

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
            Audit logs could not be loaded right now. If this persists, your
            account may not have SUPER_ADMIN access to the audit trail.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No audit records match' : 'No audit records yet'}
          description={hasFilters
            ? 'Nothing matches your filters. Try widening the time range or clearing filters.'
            : 'Administrative actions will be recorded here as staff use the platform.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          {openId != null && (() => {
            const record = items.find(entry => entry.id === openId)
            if (!record) return null
            const pretty = prettyJson(record.details)
            return (
              <div className="admin-portal__panel" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
                <div className="admin-portal__row">
                  <h2 className="admin-portal__panel-title">Audit record #{record.id}</h2>
                  <Button variant="ghost" size="small" onClick={() => setOpenId(null)}>
                    Close
                  </Button>
                </div>
                <div className="admin-portal__facts">
                  <div>
                    <h3 className="admin-portal__subitem-title">Actor</h3>
                    <p className="admin-portal__cell-muted">
                      {record.actorUserId != null ? `User #${record.actorUserId}` : 'System action'}
                    </p>
                  </div>
                  <div>
                    <h3 className="admin-portal__subitem-title">Action</h3>
                    <p className="admin-portal__cell-strong">{record.action || '—'}</p>
                  </div>
                  <div>
                    <h3 className="admin-portal__subitem-title">Entity</h3>
                    <p className="admin-portal__cell-muted">
                      {record.entityType || '—'}
                      {record.entityId != null ? ` #${record.entityId}` : ''}
                    </p>
                  </div>
                  <div>
                    <h3 className="admin-portal__subitem-title">Recorded</h3>
                    <p className="admin-portal__cell-muted">{formatDateTimeFull(record.createdAt)}</p>
                  </div>
                </div>
                {pretty && (
                  <>
                    <h3 className="admin-portal__subitem-title" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                      Details
                    </h3>
                    <pre
                      className="admin-portal__page-note"
                      style={{
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                        margin: 0,
                        padding: 'var(--spacing-3, 0.75rem)',
                        background: 'var(--surface-muted, rgba(0, 0, 0, 0.03))',
                        borderRadius: '0.5rem',
                        overflowX: 'auto',
                      }}
                    >
                      {pretty}
                    </pre>
                  </>
                )}
              </div>
            )
          })()}

          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Recorded</th>
                  <th scope="col">Actor</th>
                  <th scope="col">Action</th>
                  <th scope="col">Entity</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(record => (
                  <tr key={record.id}>
                    <td className="admin-portal__cell-muted">{formatDay(record.createdAt)}</td>
                    <td className="admin-portal__cell-muted">
                      {record.actorUserId != null ? `User #${record.actorUserId}` : 'System'}
                    </td>
                    <td className="admin-portal__cell-strong">{record.action || '—'}</td>
                    <td className="admin-portal__cell-muted">
                      {record.entityType || '—'}
                      {record.entityId != null ? ` #${record.entityId}` : ''}
                    </td>
                    <td>
                      <Button
                        variant="outline"
                        size="small"
                        onClick={() => setOpenId(current => (current === record.id ? null : record.id))}
                      >
                        {openId === record.id ? 'Close' : 'View'}
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
            noun="record"
          />

          <p className="admin-portal__page-note">
            Time filters are applied in UTC (ISO 8601). The details payload is
            the redacted JSON stored with each record; it is read-only by
            design — audit rows are append-only and written by backend
            services only.
          </p>
        </>
      )}
    </Container>
  )
}
