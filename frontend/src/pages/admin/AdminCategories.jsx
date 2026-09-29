import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listServiceCategories,
  createServiceCategory,
  updateServiceCategory,
  deleteServiceCategory,
  listProjectCategories,
  createProjectCategory,
  updateProjectCategory,
  deleteProjectCategory,
} from '../../services/adminService.js'
import {
  describeAdminError,
  isValidKey,
  SectionFeedback,
  formatDay,
} from './adminShared.jsx'

const EMPTY_FORM = { name: '', slug: '', description: '', displayOrder: '' }

/**
 * A22 — Admin categories over the existing A5/A6 category APIs
 * (services:manage / projects:manage). One page, two scoped tabs-like
 * sections: service categories and project categories. Categories have no
 * publish lifecycle of their own (status is backend-managed); deleting a
 * category detaches its referencing records — it never deletes them.
 */
export default function AdminCategories() {
  const [scope, setScope] = useState('services')
  const [serviceItems, setServiceItems] = useState(null)
  const [projectItems, setProjectItems] = useState(null)
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
    Promise.all([
      listServiceCategories().catch(() => 'error'),
      listProjectCategories().catch(() => 'error'),
    ]).then(([s, p]) => {
      if (!active) return
      setServiceItems(s === 'error' ? null : s)
      setProjectItems(p === 'error' ? null : p)
      setError(s === 'error' && p === 'error')
      setLoading(false)
    })
    return () => {
      active = false
    }
  }, [reloadKey])

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  function currentItems() {
    return scope === 'services' ? serviceItems : projectItems
  }

  function replaceItem(updated) {
    if (scope === 'services') {
      setServiceItems(previous => previous
        ? previous.map(item => (item.id === updated.id ? updated : item))
        : previous)
    } else {
      setProjectItems(previous => previous
        ? previous.map(item => (item.id === updated.id ? updated : item))
        : previous)
    }
  }

  function removeItem(id) {
    if (scope === 'services') {
      setServiceItems(previous => previous ? previous.filter(item => item.id !== id) : previous)
    } else {
      setProjectItems(previous => previous ? previous.filter(item => item.id !== id) : previous)
    }
  }

  function prependItem(created) {
    if (scope === 'services') {
      setServiceItems(previous => (previous ? [created, ...previous] : [created]))
    } else {
      setProjectItems(previous => (previous ? [created, ...previous] : [created]))
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
        const updated = scope === 'services'
          ? await updateServiceCategory(editingId, current)
          : await updateProjectCategory(editingId, current)
        replaceItem(updated)
        setEditingId(null)
        setEditForm(EMPTY_FORM)
        setFeedback({ error: null, success: 'Category updated.' })
      } else {
        const created = scope === 'services'
          ? await createServiceCategory(current)
          : await createProjectCategory(current)
        prependItem(created)
        setForm(EMPTY_FORM)
        setShowCreate(false)
        setFeedback({ error: null, success: `Category "${created.slug}" created.` })
      }
    } catch (err) {
      setFormError(describeAdminError(err, 'The category could not be saved. Please try again.'))
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(item) {
    if (actionPendingId != null || saving) return
    const confirmed = window.confirm(
      `Delete the category "${item.name}"? Records using it are detached (uncategorized), never deleted.`
    )
    if (!confirmed) return
    setActionPendingId(item.id)
    resetFeedback()
    try {
      if (scope === 'services') {
        await deleteServiceCategory(item.id)
      } else {
        await deleteProjectCategory(item.id)
      }
      removeItem(item.id)
      setFeedback({ error: null, success: 'Category deleted.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The category could not be deleted. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = currentItems() || []

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Categories</h1>
          <p className="admin-portal__muted">
            Manage service and project categories used across the catalogue
            and portfolio. Slugs are permanent identifiers; deleting a
            category never deletes the records that referenced it.
          </p>
        </div>
        <div className="admin-portal__page-actions">
          <Button
            variant={scope === 'services' ? 'primary' : 'outline'}
            size="small"
            onClick={() => {
              setScope('services')
              resetFeedback()
              setFormError(null)
              setEditingId(null)
              setShowCreate(false)
              setForm(EMPTY_FORM)
            }}
          >
            Service categories
          </Button>
          <Button
            variant={scope === 'projects' ? 'primary' : 'outline'}
            size="small"
            onClick={() => {
              setScope('projects')
              resetFeedback()
              setFormError(null)
              setEditingId(null)
              setShowCreate(false)
              setForm(EMPTY_FORM)
            }}
          >
            Project categories
          </Button>
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
          {showCreate ? 'Close form' : 'New category'}
        </Button>
      </div>

      {(showCreate || editingId != null) && (
        <form
          className="admin-portal__panel"
          onSubmit={event => submitForm(event, editingId != null)}
          style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? 'Edit category' : `New ${scope === 'services' ? 'service' : 'project'} category`}
          </h2>
          <div className="admin-portal__facts">
            <label className="admin-portal__filter-field">
              <span>Name *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={255}
                value={editingId != null ? editForm.name : form.name}
                disabled={editingId != null}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, name: event.target.value }))
                  : setForm(previous => ({ ...previous, name: event.target.value })))}
                required
              />
            </label>
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
                placeholder="infrastructure"
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
              rows={3}
              value={editingId != null ? editForm.description : form.description}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, description: event.target.value }))
                : setForm(previous => ({ ...previous, description: event.target.value })))}
            />
          </label>
          {formError && <p className="admin-portal__error" role="alert">{formError}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save changes' : 'Create category'}
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
            Name and slug are immutable after creation (the backend updates
            description and display order only).
          </p>
        </form>
      )}

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
            Categories could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={scope === 'services' ? 'No service categories yet' : 'No project categories yet'}
          description="Create a category to group the catalogue or portfolio entries."
        />
      )}

      {!loading && !error && items.length > 0 && (
        <div className="admin-portal__table-wrap">
          <table className="admin-portal__table">
            <thead>
              <tr>
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
                          setEditForm({
                            name: item.name || '',
                            slug: item.slug || '',
                            description: item.description || '',
                            displayOrder: item.displayOrder != null ? String(item.displayOrder) : '',
                          })
                        }}
                      >
                        Edit
                      </Button>
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
    </Container>
  )
}
