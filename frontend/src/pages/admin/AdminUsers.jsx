import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listUsers,
  updateUserStatus,
  updateUserRoles,
} from '../../services/adminService.js'

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'LOCKED', label: 'Locked' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'DELETED', label: 'Deleted' },
]

const ROLE_OPTIONS = [
  { value: '', label: 'All roles' },
  { value: 'SUPER_ADMIN', label: 'SUPER_ADMIN' },
  { value: 'ADMIN', label: 'ADMIN' },
  { value: 'EDITOR', label: 'EDITOR' },
  { value: 'MODERATOR', label: 'MODERATOR' },
  { value: 'CANDIDATE', label: 'CANDIDATE' },
  { value: 'EMPLOYER', label: 'EMPLOYER' },
  { value: 'CLIENT', label: 'CLIENT' },
]

const ROLE_CHECKBOXES = ROLE_OPTIONS.filter(option => option.value !== '')

const USER_TYPE_LABELS = {
  CANDIDATE: 'Candidate',
  EMPLOYER: 'Employer',
  CLIENT: 'Client',
  STAFF: 'Staff',
}

const STATUS_LABELS = {
  ACTIVE: 'Active',
  LOCKED: 'Locked',
  SUSPENDED: 'Suspended',
  DELETED: 'Deleted',
}

/**
 * Status actions offered per row. DELETED is deliberately never offered as
 * a click-to-apply action: deletion is a data-lifecycle decision with
 * cascade effects (candidates/resumes/applications), not a routine status
 * toggle — the backend still accepts it through the API if ever needed.
 */
const STATUS_ACTIONS = [
  { value: 'ACTIVE', label: 'Set active' },
  { value: 'SUSPENDED', label: 'Suspend' },
  { value: 'LOCKED', label: 'Lock' },
]

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

function describeError(err, fallback) {
  const status = err && err.response && err.response.status
  const apiErrors = err && err.response && err.response.data
    && err.response.data.errors
  const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
    ? apiErrors[0].message
    : null
  if (status === 400) return firstMessage || 'Invalid request. The change was rejected by the server.'
  if (status === 403) return 'You do not have permission to manage users.'
  if (status === 401) return 'Your session has expired. Please sign in again.'
  if (status === 404) return 'User not found.'
  if (status === 409) return 'The change conflicts with the current account state.'
  return firstMessage || fallback
}

/**
 * A21 — Admin users management over the existing A3 APIs (users:manage).
 * The backend exposes list/detail/status/roles only (there is no
 * create-user endpoint), so the UI offers exactly that and nothing more.
 * Only the safe AdminUserResponse fields are ever displayed.
 */
export default function AdminUsers() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [role, setRole] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  // Per-row action state: status change and role editing.
  const [statusPendingId, setStatusPendingId] = useState(null)
  const [rowError, setRowError] = useState(null)
  const [rowSuccess, setRowSuccess] = useState(null)
  const [editingUserId, setEditingUserId] = useState(null)
  const [editRoles, setEditRoles] = useState([])
  const [savingRoles, setSavingRoles] = useState(false)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    setRowError(null)
    setRowSuccess(null)
    listUsers({ page, size: 20, status, role, search })
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
  }, [page, status, role, search, reloadKey])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setRole('')
    setPage(0)
  }

  function resetFeedback() {
    setRowError(null)
    setRowSuccess(null)
  }

  async function handleStatusChange(user, nextStatus) {
    if (statusPendingId != null || savingRoles) return
    // Destructive/status-sensitive changes get confirmation, matching the
    // console's existing confirmation convention.
    if (nextStatus === 'DELETED') return // no delete action is offered; defensive only
    const needsConfirm = nextStatus !== 'ACTIVE'
    if (needsConfirm) {
      const confirmed = window.confirm(
        `Set the account ${user.email} to ${STATUS_LABELS[nextStatus] || nextStatus}?`
      )
      if (!confirmed) return
    }
    setStatusPendingId(user.id)
    resetFeedback()
    try {
      const updated = await updateUserStatus(user.id, nextStatus)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(item => (item.id === user.id ? updated : item)),
      } : previous)
      setRowSuccess(`Account ${updated.email} is now ${STATUS_LABELS[updated.status] || updated.status}.`)
    } catch (err) {
      setRowError(describeError(err, 'The status could not be changed. Please try again.'))
    } finally {
      setStatusPendingId(null)
    }
  }

  function startEditingRoles(user) {
    resetFeedback()
    setEditingUserId(user.id)
    setEditRoles(Array.isArray(user.roles) ? [...user.roles].sort() : [])
  }

  function cancelEditingRoles() {
    setEditingUserId(null)
    setEditRoles([])
  }

  function toggleRole(code) {
    setEditRoles(previous => (
      previous.includes(code)
        ? previous.filter(item => item !== code)
        : [...previous, code]
    ))
  }

  async function handleSaveRoles(event) {
    event.preventDefault()
    if (savingRoles || editingUserId == null) return
    setSavingRoles(true)
    resetFeedback()
    try {
      const updated = await updateUserRoles(editingUserId, editRoles)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(item => (item.id === editingUserId ? updated : item)),
      } : previous)
      setRowSuccess(
        `Roles for ${updated.email} saved (${updated.roles && updated.roles.length > 0
          ? [...updated.roles].sort().join(', ')
          : 'none'}).`
      )
      cancelEditingRoles()
    } catch (err) {
      setRowError(describeError(err, 'The roles could not be saved. Please try again.'))
    } finally {
      setSavingRoles(false)
    }
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || role || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Users</h1>
          <p className="admin-portal__muted">
            Manage platform accounts: review status, suspend or restore access,
            and assign roles. Account protection rules are enforced by the
            backend.
          </p>
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search email or name</span>
          <input
            id="admin-users-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search users…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-users-status"
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
        <label className="admin-portal__filter-field">
          <span>Role</span>
          <select
            id="admin-users-role"
            className="admin-portal__filter-select"
            value={role}
            onChange={event => {
              setPage(0)
              setRole(event.target.value)
            }}
          >
            {ROLE_OPTIONS.map(option => (
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
            Users could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No users match' : 'No users yet'}
          description={hasFilters
            ? 'No accounts match your filters. Try different keywords or clear the filters.'
            : 'Platform accounts will appear here once people sign up or are provisioned.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          {(rowError || rowSuccess) && (
            <p
              className={rowError ? 'admin-portal__error' : 'admin-portal__muted'}
              role={rowError ? 'alert' : 'status'}
            >
              {rowError || rowSuccess}
            </p>
          )}

          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Account</th>
                  <th scope="col">Type</th>
                  <th scope="col">Status</th>
                  <th scope="col">Roles</th>
                  <th scope="col">Email verified</th>
                  <th scope="col">Last login</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(user => (
                  <tr key={user.id}>
                    <td>
                      <div className="admin-portal__cell-strong">{user.fullName || '—'}</div>
                      <div className="admin-portal__cell-muted">{user.email}</div>
                    </td>
                    <td>{USER_TYPE_LABELS[user.userType] || user.userType || '—'}</td>
                    <td>
                      <span className={`admin-portal__status admin-portal__status--${user.status}`}>
                        {STATUS_LABELS[user.status] || user.status}
                      </span>
                    </td>
                    <td>
                      {editingUserId === user.id ? (
                        <form onSubmit={handleSaveRoles} className="admin-users__roles-editor">
                          <fieldset className="admin-users__roles-fieldset">
                            <legend>Roles (full replacement)</legend>
                            {ROLE_CHECKBOXES.map(option => (
                              <label key={option.value} className="admin-users__role-option">
                                <input
                                  type="checkbox"
                                  checked={editRoles.includes(option.value)}
                                  onChange={() => toggleRole(option.value)}
                                  disabled={savingRoles}
                                />
                                {' '}
                                {option.label}
                              </label>
                            ))}
                          </fieldset>
                          <div className="admin-portal__page-actions">
                            <Button
                              type="submit"
                              variant="primary"
                              size="small"
                              disabled={savingRoles}
                            >
                              {savingRoles ? 'Saving…' : 'Save roles'}
                            </Button>
                            <Button
                              type="button"
                              variant="ghost"
                              size="small"
                              disabled={savingRoles}
                              onClick={cancelEditingRoles}
                            >
                              Cancel
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <span className="admin-portal__cell-muted">
                          {(user.roles && user.roles.length > 0)
                            ? [...user.roles].sort().join(', ')
                            : '—'}
                        </span>
                      )}
                    </td>
                    <td className="admin-portal__cell-muted">
                      {user.emailVerified ? 'Yes' : 'No'}
                    </td>
                    <td className="admin-portal__cell-muted">{formatDateTime(user.lastLoginAt)}</td>
                    <td>
                      <div className="admin-portal__page-actions">
                        <Button
                          variant="outline"
                          size="small"
                          disabled={statusPendingId != null || savingRoles}
                          onClick={() => startEditingRoles(user)}
                        >
                          Roles
                        </Button>
                        {STATUS_ACTIONS
                          .filter(action => action.value !== user.status)
                          .map(action => (
                            <Button
                              key={action.value}
                              variant="ghost"
                              size="small"
                              disabled={statusPendingId != null || savingRoles}
                              onClick={() => handleStatusChange(user, action.value)}
                            >
                              {statusPendingId === user.id ? 'Working…' : action.label}
                            </Button>
                          ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="admin-portal__pagination">
            <span className="admin-portal__page-note">
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} · {data.totalItems} account{data.totalItems === 1 ? '' : 's'}
            </span>
            <div className="admin-portal__page-actions">
              <Button
                variant="outline"
                size="small"
                disabled={data.page === 0 || loading}
                onClick={() => setPage(current => Math.max(0, current - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="small"
                disabled={data.page + 1 >= data.totalPages || loading}
                onClick={() => setPage(current => current + 1)}
              >
                Next
              </Button>
            </div>
          </div>

          <p className="admin-portal__page-note">
            The backend enforces account-protection rules: an administrator
            cannot deactivate their own account or strip their own roles, the
            SUPER_ADMIN role can only be granted by a SUPER_ADMIN, and the
            last active SUPER_ADMIN cannot be disabled. There is no
            account-creation flow: accounts are created when people register
            on the platform, and this console manages their status and roles
            only.
          </p>
        </>
      )}
    </Container>
  )
}
