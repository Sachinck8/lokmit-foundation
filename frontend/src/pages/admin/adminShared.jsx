/**
 * A22 — small shared helpers for the admin content-platform pages
 * (website content, services, expertise, projects). These mirror the
 * conventions established in A18–A21 (status chips, error mapping,
 * pagination block) so each page stays focused on its own fields.
 *
 * Backend-authoritative lifecycle rules shared by all four domains
 * (identical transition matrices in their services): new records always
 * start DRAFT; publish is allowed from DRAFT (idempotent for PUBLISHED);
 * archive is allowed from DRAFT or PUBLISHED; ARCHIVED is terminal and
 * can only be deleted (where the backend exposes delete).
 */
import Button from '../../components/Button/Button.jsx'

export function describeAdminError(err, fallback) {
  const status = err && err.response && err.response.status
  const apiErrors = err && err.response && err.response.data
    && err.response.data.errors
  const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
    ? apiErrors[0].message
    : null
  if (status === 400) return firstMessage || 'Invalid request. Please check the entered values.'
  if (status === 401) return 'Your session has expired. Please sign in again.'
  if (status === 403) return 'You do not have permission for this action.'
  if (status === 404) return 'The record was not found. It may have been deleted.'
  if (status === 409) return firstMessage || 'A record with these details already exists.'
  return firstMessage || fallback
}

/** Backend slug/pageKey/sectionKey pattern ([a-z0-9_.-]+). */
export function isValidKey(value) {
  return /^[a-z0-9_.-]+$/.test(value)
}

/** Parses and validates a JSON string for JSONB-backed fields. */
export function parseJsonField(value) {
  const trimmed = (value || '').trim()
  if (trimmed === '') return { ok: true, value: '' }
  try {
    return { ok: true, value: trimmed, parsed: JSON.parse(trimmed) }
  } catch {
    return { ok: false }
  }
}

/** Lifecycle actions available for a DRAFT/PUBLISHED/ARCHIVED record. */
export function lifecycleActions(status) {
  if (status === 'DRAFT') {
    return [{ key: 'publish', label: 'Publish' }, { key: 'archive', label: 'Archive' }]
  }
  if (status === 'PUBLISHED') return [{ key: 'archive', label: 'Archive' }]
  return [] // ARCHIVED is terminal
}

export function SectionFeedback({ error, success }) {
  if (!error && !success) return null
  return (
    <p
      className={error ? 'admin-portal__error' : 'admin-portal__muted'}
      role={error ? 'alert' : 'status'}
    >
      {error || success}
    </p>
  )
}

export function ListPagination({ data, loading, onPage, noun }) {
  if (!data) return null
  return (
    <div className="admin-portal__pagination">
      <span className="admin-portal__page-note">
        Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalItems} {noun}{data.totalItems === 1 ? '' : 's'}
      </span>
      <div className="admin-portal__page-actions">
        <Button
          variant="outline"
          size="small"
          disabled={data.page === 0 || loading}
          onClick={() => onPage(current => Math.max(0, current - 1))}
        >
          Previous
        </Button>
        <Button
          variant="outline"
          size="small"
          disabled={data.page + 1 >= data.totalPages || loading}
          onClick={() => onPage(current => current + 1)}
        >
          Next
        </Button>
      </div>
    </div>
  )
}

export function formatDay(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}
