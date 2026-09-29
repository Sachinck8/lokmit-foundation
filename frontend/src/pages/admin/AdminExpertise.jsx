import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listExpertiseAreas,
  createExpertiseArea,
  updateExpertiseArea,
  publishExpertiseArea,
  archiveExpertiseArea,
  deleteExpertiseArea,
} from '../../services/adminService.js'
import {
  describeAdminError,
  isValidKey,
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

const EMPTY_FORM = { slug: '', name: '', description: '', displayOrder: '' }

/**
 * A22 — Admin expertise areas over the existing A5 APIs
 * (services:manage). Slug immutable; DRAFT → PUBLISHED → ARCHIVED
 * lifecycle identical to services.
 */
export default function AdminExpertise() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
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
    listExpertiseAreas({ page, size: 20, status, search })
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

  function rowToForm(item) {
    return {
      slug: item.slug || '',
      name: item.name || '',
      description: item.description || '',
      displayOrder: item.displayOrder != null ? String(item.displayOrder) : '',
    }
  }

  async function submitForm(event, isEdit) {
    event.preventDefault()
    if (saving) return
    const current = isEdit ? editForm : form
    if (!current.name.trim() || (!isEdit && !isValidKey(current.slug.trim()))) {
      setFormError(!current.name.trim()
        ? 'Name is required.'
        : 'Slug must contain only lowercase letters, digits, dot, dash or underscore.')
      return
    }
    setSaving(true)
    setFormError(null)
    resetFeedback()
    try {
      if (isEdit) {
        const updated = await updateExpertiseArea(editingId, current)
        setData(previous => previous ? {
          ...previous,
          items: previous.items.map(item => (item.id === editingId ? updated : item)),
        } : previous)
        setEditingId(null)
        setEditForm(EMPTY_FORM)
        setFeedback({ error: null, success: 'Expertise area updated.' })
      } else {
        const created = await createExpertiseArea(current)
        setData(previous => previous ? {
          ...previous,
          items: [created, ...previous.items].slice(0, previous.size || 20),
          totalItems: previous.totalItems + 1,
        } : previous)
        setForm(EMPTY_FORM)
        setShowCreate(false)
        setFeedback({ error: null, success: `Expertise area "${created.slug}" created as DRAFT.` })
      }
    } catch (err) {
      setFormError(describeAdminError(err, 'The expertise area could not be saved. Please try again.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleLifecycle(item, action) {
    if (actionPendingId != null || saving) return
    if (action === 'archive') {
      const confirmed = window.confirm(`Archive "${item.name}"? Archiving is permanent.`)
      if (!confirmed) return
    }
    setActionPendingId(item.id)
    resetFeedback()
    try {
      const updated = action === 'publish'
        ? await publishExpertiseArea(item.id)
        : await archiveExpertiseArea(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(row => (row.id === item.id ? updated : row)),
      } : previous)
      setFeedback({
        error: null,
        success: action === 'publish' ? 'Expertise area published.' : 'Expertise area archived.',
      })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The change could not be applied. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  async function handleDelete(item) {
    if (actionPendingId != null || saving) return
    const confirmed = window.confirm(`Delete "${item.name}" permanently? This cannot be undone.`)
    if (!confirmed) return
    setActionPendingId(item.id)
    resetFeedback()
    try {
      await deleteExpertiseArea(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.filter(row => row.id !== item.id),
        totalItems: Math.max(0, previous.totalItems - 1),
      } : previous)
      setFeedback({ error: null, success: 'Expertise area deleted.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The expertise area could not be deleted. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Expertise areas</h1>
          <p className="admin-portal__muted">
            Manage the foundation's expertise areas. Slugs are permanent
            identifiers; areas follow the same draft → published → archived
            lifecycle as services.
          </p>
        </div>
        <Button
          variant="primary"
          size="medium"
          disabled={saving}
          onClick={() => {
            resetFeedback()
            setFormError(null)
            setEditingId(null)
            setForm(EMPTY_FORM)
            setShowCreate(value => !value)
          }}
        >
          {showCreate ? 'Close form' : 'New expertise area'}
        </Button>
      </div>

      {(showCreate || editingId != null) && (
        <form
          className="admin-portal__panel"
          onSubmit={event => submitForm(event, editingId != null)}
          style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? `Edit expertise area: ${editForm.slug}` : 'New expertise area'}
          </h2>
          <div className="admin-portal__facts">
            <label className="admin-portal__filter-field">
              <span>Slug *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={160}
                value={editingId != null ? editForm.slug : form.slug}
                disabled={editingId != null}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, slug: event.target.value }))
                  : setForm(previous => ({ ...previous, slug: event.target.value })))}
                placeholder="bim-modelling"
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Name *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={255}
                value={editingId != null ? editForm.name : form.name}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, name: event.target.value }))
                  : setForm(previous => ({ ...previous, name: event.target.value })))}
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Display order</span>
              <input
                className="admin-portal__filter-input"
                type="number"
                min={0}
                value={editingId != null ? editForm.displayOrder : form.displayOrder}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, displayOrder: event.target.value }))
                  : setForm(previous => ({ ...previous, displayOrder: event.target.value })))}
              />
            </label>
          </div>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Description</span>
            <textarea
              className="admin-portal__filter-input"
              rows={5}
              value={editingId != null ? editForm.description : form.description}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, description: event.target.value }))
                : setForm(previous => ({ ...previous, description: event.target.value })))}
            />
          </label>
          {formError && <p className="admin-portal__error" role="alert">{formError}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save changes' : 'Create expertise area'}
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="medium"
              disabled={saving}
              onClick={() => {
                setFormError(null)
                setEditingId(null)
                setEditForm(EMPTY_FORM)
                setShowCreate(false)
                setForm(EMPTY_FORM)
              }}
            >
              Cancel
            </Button>
          </div>
        </form>
      )}

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search name</span>
          <input
            id="admin-expertise-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search expertise areas…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-expertise-status"
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
            Expertise areas could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No expertise areas match' : 'No expertise areas yet'}
          description={hasFilters
            ? 'No areas match your filters. Try different keywords or clear the filters.'
            : 'Create the first expertise area for the public site.'}
          actionLabel={hasFilters ? 'Clear filters' : undefined}
          onAction={hasFilters ? clearFilters : undefined}
        />
      )}

      {!loading && !error && items.length > 0 && (
        <>
          <div className="admin-portal__table-wrap">
            <table className="admin-portal__table">
              <thead>
                <tr>
                  <th scope="col">Expertise area</th>
                  <th scope="col">Order</th>
                  <th scope="col">Status</th>
                  <th scope="col">Updated</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  <tr key={item.id}>
                    <td>
                      <div className="admin-portal__cell-strong">{item.name}</div>
                      <div className="admin-portal__cell-muted">{item.slug}</div>
                    </td>
                    <td className="admin-portal__cell-muted">{item.displayOrder}</td>
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
                          onClick={() => {
                            resetFeedback()
                            setFormError(null)
                            setShowCreate(false)
                            setEditingId(item.id)
                            setEditForm(rowToForm(item))
                          }}
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
          <ListPagination data={data} loading={loading} onPage={setPage} noun="area" />
        </>
      )}
    </Container>
  )
}
