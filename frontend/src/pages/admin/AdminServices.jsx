import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listServices,
  listServiceCategories,
  createService,
  updateService,
  publishService,
  archiveService,
  deleteService,
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

const EMPTY_FORM = {
  slug: '', title: '', summary: '', description: '',
  categoryId: '', displayOrder: '',
}

/**
 * A22 — Admin services management over the existing A5 APIs
 * (services:manage). Slug is immutable after creation; category and
 * display order are editable; lifecycle follows the backend's
 * DRAFT → PUBLISHED → ARCHIVED matrix.
 */
export default function AdminServices() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [data, setData] = useState(null)
  const [categories, setCategories] = useState([])
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
    listServices({ page, size: 20, status, categoryId, search })
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
  }, [page, status, categoryId, search, reloadKey])

  useEffect(() => {
    let active = true
    listServiceCategories().then(items => {
      if (active) setCategories(items)
    }).catch(() => {
      if (active) setCategories([])
    })
    return () => {
      active = false
    }
  }, [reloadKey])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setCategoryId('')
    setPage(0)
  }

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  function rowToForm(item) {
    return {
      slug: item.slug || '',
      title: item.title || '',
      summary: item.summary || '',
      description: item.description || '',
      categoryId: item.category && item.category.id != null ? String(item.category.id) : '',
      displayOrder: item.displayOrder != null ? String(item.displayOrder) : '',
    }
  }

  async function submitForm(event, isEdit) {
    event.preventDefault()
    if (saving) return
    const current = isEdit ? editForm : form
    if (!current.title.trim() || (!isEdit && !isValidKey(current.slug.trim()))) {
      setFormError(!current.title.trim()
        ? 'Title is required.'
        : 'Slug must contain only lowercase letters, digits, dot, dash or underscore.')
      return
    }
    setSaving(true)
    setFormError(null)
    resetFeedback()
    try {
      if (isEdit) {
        const updated = await updateService(editingId, current)
        setData(previous => previous ? {
          ...previous,
          items: previous.items.map(item => (item.id === editingId ? updated : item)),
        } : previous)
        setEditingId(null)
        setEditForm(EMPTY_FORM)
        setFeedback({ error: null, success: 'Service updated.' })
      } else {
        const created = await createService(current)
        setData(previous => previous ? {
          ...previous,
          items: [created, ...previous.items].slice(0, previous.size || 20),
          totalItems: previous.totalItems + 1,
        } : previous)
        setForm(EMPTY_FORM)
        setShowCreate(false)
        setFeedback({ error: null, success: `Service "${created.slug}" created as DRAFT.` })
      }
    } catch (err) {
      setFormError(describeAdminError(err, 'The service could not be saved. Please try again.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleLifecycle(item, action) {
    if (actionPendingId != null || saving) return
    if (action === 'archive') {
      const confirmed = window.confirm(`Archive "${item.title}"? Archiving is permanent.`)
      if (!confirmed) return
    }
    setActionPendingId(item.id)
    resetFeedback()
    try {
      const updated = action === 'publish'
        ? await publishService(item.id)
        : await archiveService(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(row => (row.id === item.id ? updated : row)),
      } : previous)
      setFeedback({
        error: null,
        success: action === 'publish' ? 'Service published.' : 'Service archived.',
      })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The change could not be applied. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  async function handleDelete(item) {
    if (actionPendingId != null || saving) return
    const confirmed = window.confirm(`Delete "${item.title}" permanently? This cannot be undone.`)
    if (!confirmed) return
    setActionPendingId(item.id)
    resetFeedback()
    try {
      await deleteService(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.filter(row => row.id !== item.id),
        totalItems: Math.max(0, previous.totalItems - 1),
      } : previous)
      setFeedback({ error: null, success: 'Service deleted.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The service could not be deleted. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || categoryId || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Services</h1>
          <p className="admin-portal__muted">
            Manage the foundation's service catalogue. Services start as
            drafts and must be published explicitly; the slug is the
            permanent identifier.
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
          {showCreate ? 'Close form' : 'New service'}
        </Button>
      </div>

      {(showCreate || editingId != null) && (
        <form
          className="admin-portal__panel"
          onSubmit={event => submitForm(event, editingId != null)}
          style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? `Edit service: ${editForm.slug}` : 'New service'}
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
                placeholder="structural-design"
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Title *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={255}
                value={editingId != null ? editForm.title : form.title}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, title: event.target.value }))
                  : setForm(previous => ({ ...previous, title: event.target.value })))}
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Category</span>
              <select
                className="admin-portal__filter-select"
                value={editingId != null ? editForm.categoryId : form.categoryId}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, categoryId: event.target.value }))
                  : setForm(previous => ({ ...previous, categoryId: event.target.value })))}
              >
                <option value="">Uncategorized</option>
                {categories.map(category => (
                  <option key={category.id} value={String(category.id)}>{category.name}</option>
                ))}
              </select>
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
            <span>Summary (max 500 characters)</span>
            <textarea
              className="admin-portal__filter-input"
              rows={2}
              maxLength={500}
              value={editingId != null ? editForm.summary : form.summary}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, summary: event.target.value }))
                : setForm(previous => ({ ...previous, summary: event.target.value })))}
            />
          </label>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Description</span>
            <textarea
              className="admin-portal__filter-input"
              rows={6}
              value={editingId != null ? editForm.description : form.description}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, description: event.target.value }))
                : setForm(previous => ({ ...previous, description: event.target.value })))}
            />
          </label>
          {formError && <p className="admin-portal__error" role="alert">{formError}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save changes' : 'Create service'}
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
          <p className="admin-portal__page-note">
            The slug is immutable after creation. Clearing the category
            detaches the service (it becomes uncategorized); it is never
            deleted by a category change.
          </p>
        </form>
      )}

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search title/summary</span>
          <input
            id="admin-services-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search services…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Category</span>
          <select
            id="admin-services-category"
            className="admin-portal__filter-select"
            value={categoryId}
            onChange={event => {
              setPage(0)
              setCategoryId(event.target.value)
            }}
          >
            <option value="">All categories</option>
            {categories.map(category => (
              <option key={category.id} value={String(category.id)}>{category.name}</option>
            ))}
          </select>
        </label>
        <label className="admin-portal__filter-field">
          <span>Status</span>
          <select
            id="admin-services-status"
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
            Services could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No services match' : 'No services yet'}
          description={hasFilters
            ? 'No services match your filters. Try different keywords or clear the filters.'
            : 'Create the first service entry for the public catalogue.'}
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
                  <th scope="col">Service</th>
                  <th scope="col">Category</th>
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
                      <div className="admin-portal__cell-strong">{item.title}</div>
                      <div className="admin-portal__cell-muted">{item.slug}</div>
                    </td>
                    <td>{item.category ? item.category.name : '—'}</td>
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
          <ListPagination data={data} loading={loading} onPage={setPage} noun="service" />
        </>
      )}
    </Container>
  )
}
