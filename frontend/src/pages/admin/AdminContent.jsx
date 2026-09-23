import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listWebsiteContent,
  createWebsiteContent,
  updateWebsiteContent,
  publishWebsiteContent,
  archiveWebsiteContent,
  deleteWebsiteContent,
} from '../../services/adminService.js'
import {
  describeAdminError,
  isValidKey,
  parseJsonField,
  lifecycleActions,
  SectionFeedback,
  ListPagination,
  formatDay,
} from './adminShared.jsx'

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'DRAFT', label: 'Draft' },
  { value: 'PUBLISHED', label: 'Published' },
  { value: 'ARCHIVED', label: 'Archived' },
]

const EMPTY_FORM = {
  pageKey: '', sectionKey: '', title: '', contentJson: '',
}

function validateJson(value) {
  const result = parseJsonField(value)
  return result.ok ? null : 'Content JSON must be valid JSON (or left empty).'
}

/**
 * A22 — Admin website content management over the existing A4 APIs
 * (content:manage for list/create/edit; content:publish for
 * publish/archive/delete — enforced server-side; the UI simply offers the
 * actions). Sections start DRAFT; the (pageKey, sectionKey) pair is
 * immutable after creation; content is stored as JSONB, so the form
 * validates JSON client-side as UX while the backend remains the
 * authority. No rich text/HTML rendering exists in the contract — the
 * value is treated as data, never injected into the DOM.
 */
export default function AdminContent() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [pageKeyInput, setPageKeyInput] = useState('')
  const [pageKey, setPageKey] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const [form, setForm] = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [showCreate, setShowCreate] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [actionPendingId, setActionPendingId] = useState(null)
  const [feedback, setFeedback] = useState({ error: null, success: null })

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listWebsiteContent({ page, size: 20, status, pageKey })
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
  }, [page, status, pageKey, reloadKey])

  function applyFilters(event) {
    event.preventDefault()
    setPage(0)
    setPageKey(pageKeyInput.trim())
  }

  function clearFilters() {
    setPageKeyInput('')
    setPageKey('')
    setStatus('')
    setPage(0)
  }

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  async function handleCreate(event) {
    event.preventDefault()
    if (saving) return
    const jsonProblem = validateJson(form.contentJson)
    if (!isValidKey(form.pageKey.trim()) || !isValidKey(form.sectionKey.trim()) || jsonProblem) {
      setFormError(
        !isValidKey(form.pageKey.trim())
          ? 'Page key must contain only lowercase letters, digits, dot, dash or underscore.'
          : !isValidKey(form.sectionKey.trim())
            ? 'Section key must contain only lowercase letters, digits, dot, dash or underscore.'
            : jsonProblem
      )
      return
    }
    setSaving(true)
    setFormError(null)
    resetFeedback()
    try {
      const created = await createWebsiteContent({
        pageKey: form.pageKey.trim(),
        sectionKey: form.sectionKey.trim(),
        title: form.title,
        contentJson: form.contentJson,
      })
      setData(previous => previous ? {
        ...previous,
        items: [created, ...previous.items].slice(0, previous.size || 20),
        totalItems: previous.totalItems + 1,
      } : previous)
      setForm(EMPTY_FORM)
      setShowCreate(false)
      setFeedback({ error: null, success: `Section "${created.pageKey}/${created.sectionKey}" created as DRAFT.` })
    } catch (err) {
      setFormError(describeAdminError(err, 'The section could not be created. Please try again.'))
    } finally {
      setSaving(false)
    }
  }

  function startEditing(item) {
    resetFeedback()
    setShowCreate(false)
    setEditingId(item.id)
    setEditForm({
      pageKey: item.pageKey || '',
      sectionKey: item.sectionKey || '',
      title: item.title || '',
      contentJson: item.contentJson || '',
    })
  }

  function cancelEditing() {
    setEditingId(null)
    setEditForm(EMPTY_FORM)
  }

  async function handleUpdate(event) {
    event.preventDefault()
    if (saving || editingId == null) return
    const jsonProblem = validateJson(editForm.contentJson)
    if (jsonProblem) {
      setFormError(jsonProblem)
      return
    }
    setSaving(true)
    setFormError(null)
    resetFeedback()
    try {
      const updated = await updateWebsiteContent(editingId, {
        title: editForm.title,
        contentJson: editForm.contentJson,
      })
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(item => (item.id === editingId ? updated : item)),
      } : previous)
      cancelEditing()
      setFeedback({ error: null, success: 'Section updated.' })
    } catch (err) {
      setFormError(describeAdminError(err, 'The section could not be updated. Please try again.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleLifecycle(item, action) {
    if (actionPendingId != null || saving) return
    if (action === 'archive') {
      const confirmed = window.confirm(
        `Archive "${item.pageKey}/${item.sectionKey}"? Archiving is permanent.`
      )
      if (!confirmed) return
    }
    setActionPendingId(item.id)
    resetFeedback()
    try {
      const updated = action === 'publish'
        ? await publishWebsiteContent(item.id)
        : await archiveWebsiteContent(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(row => (row.id === item.id ? updated : row)),
      } : previous)
      setFeedback({
        error: null,
        success: action === 'publish' ? 'Section published.' : 'Section archived.',
      })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The change could not be applied. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  async function handleDelete(item) {
    if (actionPendingId != null || saving) return
    const confirmed = window.confirm(
      `Delete "${item.pageKey}/${item.sectionKey}" permanently? This cannot be undone.`
    )
    if (!confirmed) return
    setActionPendingId(item.id)
    resetFeedback()
    try {
      await deleteWebsiteContent(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.filter(row => row.id !== item.id),
        totalItems: Math.max(0, previous.totalItems - 1),
      } : previous)
      setFeedback({ error: null, success: 'Section deleted.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The section could not be deleted. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || pageKey)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Website content</h1>
          <p className="admin-portal__muted">
            Manage the foundation website's page sections. Sections are
            identified by an immutable page/section key pair, start as drafts
            and must be published explicitly. Content is stored as JSON.
          </p>
        </div>
        <Button
          variant="primary"
          size="medium"
          disabled={saving}
          onClick={() => {
            resetFeedback()
            setFormError(null)
            cancelEditing()
            setShowCreate(value => !value)
          }}
        >
          {showCreate ? 'Close form' : 'New section'}
        </Button>
      </div>

      {(showCreate || editingId != null) && (
        <form
          className="admin-portal__panel"
          onSubmit={editingId != null ? handleUpdate : handleCreate}
          style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null
              ? `Edit section: ${editForm.pageKey}/${editForm.sectionKey}`
              : 'New content section'}
          </h2>
          <div className="admin-portal__facts">
            <label className="admin-portal__filter-field">
              <span>Page key *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                value={form.pageKey}
                disabled={editingId != null}
                onChange={event => setForm(previous => ({ ...previous, pageKey: event.target.value }))}
                placeholder="home"
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Section key *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                value={form.sectionKey}
                disabled={editingId != null}
                onChange={event => setForm(previous => ({ ...previous, sectionKey: event.target.value }))}
                placeholder="hero"
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Title</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={255}
                value={form.title}
                onChange={event => setForm(previous => ({ ...previous, title: event.target.value }))}
              />
            </label>
          </div>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Content (JSON, max 100,000 characters)</span>
            <textarea
              className="admin-portal__filter-input"
              rows={8}
              maxLength={100000}
              value={form.contentJson}
              onChange={event => setForm(previous => ({ ...previous, contentJson: event.target.value }))}
              placeholder='{"headline": "Welcome"}'
            />
          </label>
          {formError && <p className="admin-portal__error" role="alert">{formError}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save changes' : 'Create section'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="medium"
              disabled={saving}
              onClick={() => {
                setFormError(null)
                if (editingId != null) cancelEditing()
                else setShowCreate(false)
                setForm(EMPTY_FORM)
              }}
            >
              Cancel
            </Button>
          </div>
          <p className="admin-portal__page-note">
            Keys are immutable after creation and cannot be edited later.
            Editing a section never changes its publish status; use the row
            actions to publish or archive.
          </p>
        </form>
      )}

      <form className="admin-portal__filters" onSubmit={applyFilters}>
        <label className="admin-portal__filter-field">
          <span>Page key</span>
          <input
            id="admin-content-pagekey"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Exact page key…"
            value={pageKeyInput}
            onChange={event => setPageKeyInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-content-status"
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
            Content sections could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No sections match' : 'No content sections yet'}
          description={hasFilters
            ? 'No sections match your filters. Try a different page key or clear the filters.'
            : 'Create the first section to manage website copy, such as the home page hero.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <div className="admin-portal__table-wrap">
          <table className="admin-portal__table">
            <thead>
              <tr>
                <th scope="col">Section</th>
                <th scope="col">Title</th>
                <th scope="col">Status</th>
                <th scope="col">Updated</th>
                <th scope="col">Actions</th>
              </tr>
            </thead>
            <tbody>
              {items.map(item => (
                <tr key={item.id}>
                  <td>
                    <div className="admin-portal__cell-strong">{item.pageKey}/{item.sectionKey}</div>
                    <div className="admin-portal__cell-muted">
                      {item.contentJson ? `${item.contentJson.length} chars` : 'empty'}
                    </div>
                  </td>
                  <td>{item.title || '—'}</td>
                  <td>
                    <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                      {item.status}
                    </span>
                  </td>
                  <td className="admin-portal__cell-muted">{formatDay(item.updatedAt)}</td>
                  <td>
                    <div className="admin-portal__page-actions">
                      <Button
                        variant="outline"
                        size="small"
                        disabled={saving || actionPendingId != null}
                        onClick={() => startEditing(item)}
                      >
                        Edit
                      </Button>
                      {lifecycleActions(item.status).map(action => (
                        <Button
                          key={action.key}
                          variant="ghost"
                          size="small"
                          disabled={saving || actionPendingId != null}
                          onClick={() => handleLifecycle(item, action.key)}
                        >
                          {actionPendingId === item.id ? 'Working…' : action.label}
                        </Button>
                      ))}
                      <Button
                        variant="ghost"
                        size="small"
                        disabled={saving || actionPendingId != null}
                        onClick={() => handleDelete(item)}
                      >
                        {actionPendingId === item.id ? 'Working…' : 'Delete'}
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && items.length > 0 && (
        <ListPagination data={data} loading={loading} onPage={setPage} noun="section" />
      )}
    </Container>
  )
}
