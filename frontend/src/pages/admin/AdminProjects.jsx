import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listProjects,
  listProjectCategories,
  createProject,
  updateProject,
  publishProject,
  archiveProject,
  deleteProject,
  listProjectImages,
  createProjectImage,
  updateProjectImage,
  deleteProjectImage,
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

const PROJECT_STATUS_OPTIONS = [
  { value: '', label: 'Any delivery state' },
  { value: 'PLANNING', label: 'Planning' },
  { value: 'ONGOING', label: 'Ongoing' },
  { value: 'COMPLETED', label: 'Completed' },
]

const EMPTY_FORM = {
  slug: '', title: '', summary: '', description: '', categoryId: '',
  projectStatus: '', location: '', startDate: '', endDate: '',
  objectives: '', impactSummary: '',
}

const EMPTY_IMAGE_FORM = { imageUrl: '', altText: '', displayOrder: '' }

function validateProjectForm(current, isEdit) {
  if (!current.title.trim()) return 'Title is required.'
  if (!isEdit && !isValidKey(current.slug.trim())) {
    return 'Slug must contain only lowercase letters, digits, dot, dash or underscore.'
  }
  if (current.startDate && current.endDate && current.endDate < current.startDate) {
    return 'End date must be on or after the start date.'
  }
  const objectives = parseJsonField(current.objectives)
  if (!objectives.ok) return 'Objectives must be valid JSON (or left empty).'
  return null
}

/**
 * A22 — Admin projects over the existing A6 APIs (projects:manage),
 * including image metadata management (the backend stores metadata rows
 * only — image files themselves are served outside this API). Delivery
 * state (PLANNING/ONGOING/COMPLETED) is data, separate from the
 * DRAFT/PUBLISHED/ARCHIVED publication lifecycle.
 */
export default function AdminProjects() {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const [projectStatus, setProjectStatus] = useState('')
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

  // Image metadata management for the project being edited.
  const [images, setImages] = useState(null)
  const [imagesError, setImagesError] = useState(false)
  const [imageForm, setImageForm] = useState(EMPTY_IMAGE_FORM)
  const [imageFormError, setImageFormError] = useState(null)
  const [editingImageId, setEditingImageId] = useState(null)
  const [editImageForm, setEditImageForm] = useState(EMPTY_IMAGE_FORM)
  const [imageSaving, setImageSaving] = useState(false)
  const [imageActionPendingId, setImageActionPendingId] = useState(null)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    listProjects({ page, size: 20, status, projectStatus, categoryId, search })
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
  }, [page, status, projectStatus, categoryId, search, reloadKey])

  useEffect(() => {
    let active = true
    listProjectCategories().then(items => {
      if (active) setCategories(items)
    }).catch(() => {
      if (active) setCategories([])
    })
    return () => {
      active = false
    }
  }, [reloadKey])

  useEffect(() => {
    if (editingId == null) {
      setImages(null)
      setImagesError(false)
      return undefined
    }
    let active = true
    setImagesError(false)
    listProjectImages(editingId)
      .then(items => {
        if (active) setImages(items)
      })
      .catch(() => {
        if (active) setImagesError(true)
      })
    return () => {
      active = false
    }
  }, [editingId])

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setStatus('')
    setProjectStatus('')
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
      projectStatus: item.projectStatus || '',
      location: item.location || '',
      startDate: item.startDate || '',
      endDate: item.endDate || '',
      objectives: item.objectives || '',
      impactSummary: item.impactSummary || '',
    }
  }

  function reloadImages(projectId) {
    return listProjectImages(projectId)
      .then(items => setImages(items))
      .catch(() => setImagesError(true))
  }

  async function submitForm(event, isEdit) {
    event.preventDefault()
    if (saving) return
    const current = isEdit ? editForm : form
    const problem = validateProjectForm(current, isEdit)
    if (problem) {
      setFormError(problem)
      return
    }
    setSaving(true)
    setFormError(null)
    resetFeedback()
    try {
      if (isEdit) {
        const updated = await updateProject(editingId, current)
        setData(previous => previous ? {
          ...previous,
          items: previous.items.map(item => (item.id === editingId ? updated : item)),
        } : previous)
        setEditingId(null)
        setEditForm(EMPTY_FORM)
        setImages(null)
        setFeedback({ error: null, success: 'Project updated.' })
      } else {
        const created = await createProject(current)
        setData(previous => previous ? {
          ...previous,
          items: [created, ...previous.items].slice(0, previous.size || 20),
          totalItems: previous.totalItems + 1,
        } : previous)
        setForm(EMPTY_FORM)
        setShowCreate(false)
        setFeedback({ error: null, success: `Project "${created.slug}" created as DRAFT.` })
      }
    } catch (err) {
      setFormError(describeAdminError(err, 'The project could not be saved. Please try again.'))
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
        ? await publishProject(item.id)
        : await archiveProject(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.map(row => (row.id === item.id ? updated : row)),
      } : previous)
      setFeedback({
        error: null,
        success: action === 'publish' ? 'Project published.' : 'Project archived.',
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
      await deleteProject(item.id)
      setData(previous => previous ? {
        ...previous,
        items: previous.items.filter(row => row.id !== item.id),
        totalItems: Math.max(0, previous.totalItems - 1),
      } : previous)
      setFeedback({ error: null, success: 'Project deleted.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The project could not be deleted. Please try again.'), success: null })
    } finally {
      setActionPendingId(null)
    }
  }

  async function handleAddImage(event) {
    event.preventDefault()
    if (imageSaving || editingId == null) return
    if (!imageForm.imageUrl.trim()) {
      setImageFormError('Image URL is required.')
      return
    }
    setImageSaving(true)
    setImageFormError(null)
    try {
      await createProjectImage(editingId, imageForm)
      await reloadImages(editingId)
      setImageForm(EMPTY_IMAGE_FORM)
      setFeedback({ error: null, success: 'Image added.' })
    } catch (err) {
      setImageFormError(describeAdminError(err, 'The image could not be added. Please try again.'))
    } finally {
      setImageSaving(false)
    }
  }

  async function handleSaveImage(event) {
    event.preventDefault()
    if (imageSaving || editingImageId == null) return
    setImageSaving(true)
    setImageFormError(null)
    try {
      await updateProjectImage(editingId, editingImageId, editImageForm)
      await reloadImages(editingId)
      setEditingImageId(null)
      setEditImageForm(EMPTY_IMAGE_FORM)
      setFeedback({ error: null, success: 'Image updated.' })
    } catch (err) {
      setImageFormError(describeAdminError(err, 'The image could not be updated. Please try again.'))
    } finally {
      setImageSaving(false)
    }
  }

  async function handleDeleteImage(image) {
    if (imageActionPendingId != null) return
    const confirmed = window.confirm('Remove this image metadata row permanently?')
    if (!confirmed) return
    setImageActionPendingId(image.id)
    try {
      await deleteProjectImage(editingId, image.id)
      await reloadImages(editingId)
      setFeedback({ error: null, success: 'Image removed.' })
    } catch (err) {
      setFeedback({ error: describeAdminError(err, 'The image could not be removed. Please try again.'), success: null })
    } finally {
      setImageActionPendingId(null)
    }
  }

  const items = (data && data.items) || []
  const hasFilters = Boolean(status || projectStatus || categoryId || search)

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Projects</h1>
          <p className="admin-portal__muted">
            Manage the foundation's project portfolio. Projects start as
            drafts; publishing is explicit. Delivery state (planning /
            ongoing / completed) is separate from publication status.
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
            setImages(null)
            setForm(EMPTY_FORM)
            setShowCreate(value => !value)
          }}
        >
          {showCreate ? 'Close form' : 'New project'}
        </Button>
      </div>

      {(showCreate || editingId != null) && (
        <form
          className="admin-portal__panel"
          onSubmit={event => submitForm(event, editingId != null)}
          style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? `Edit project: ${editForm.slug}` : 'New project'}
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
                placeholder="rural-water-supply"
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
              <span>Delivery state</span>
              <select
                className="admin-portal__filter-select"
                value={editingId != null ? editForm.projectStatus : form.projectStatus}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, projectStatus: event.target.value }))
                  : setForm(previous => ({ ...previous, projectStatus: event.target.value })))}
              >
                <option value="">Not set</option>
                {PROJECT_STATUS_OPTIONS.slice(1).map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="admin-portal__filter-field">
              <span>Location</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={255}
                value={editingId != null ? editForm.location : form.location}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, location: event.target.value }))
                  : setForm(previous => ({ ...previous, location: event.target.value })))}
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Start date</span>
              <input
                className="admin-portal__filter-input"
                type="date"
                value={editingId != null ? editForm.startDate : form.startDate}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, startDate: event.target.value }))
                  : setForm(previous => ({ ...previous, startDate: event.target.value })))}
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>End date</span>
              <input
                className="admin-portal__filter-input"
                type="date"
                value={editingId != null ? editForm.endDate : form.endDate}
                onChange={event => (editingId != null
                  ? setEditForm(previous => ({ ...previous, endDate: event.target.value }))
                  : setForm(previous => ({ ...previous, endDate: event.target.value })))}
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
              rows={5}
              value={editingId != null ? editForm.description : form.description}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, description: event.target.value }))
                : setForm(previous => ({ ...previous, description: event.target.value })))}
            />
          </label>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Objectives (JSON, optional)</span>
            <textarea
              className="admin-portal__filter-input"
              rows={4}
              value={editingId != null ? editForm.objectives : form.objectives}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, objectives: event.target.value }))
                : setForm(previous => ({ ...previous, objectives: event.target.value })))}
              placeholder='{"goal": "…"}'
            />
          </label>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Impact summary</span>
            <textarea
              className="admin-portal__filter-input"
              rows={3}
              value={editingId != null ? editForm.impactSummary : form.impactSummary}
              onChange={event => (editingId != null
                ? setEditForm(previous => ({ ...previous, impactSummary: event.target.value }))
                : setForm(previous => ({ ...previous, impactSummary: event.target.value })))}
            />
          </label>
          {formError && <p className="admin-portal__error" role="alert">{formError}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save changes' : 'Create project'}
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
                setImages(null)
                setShowCreate(false)
                setForm(EMPTY_FORM)
              }}
            >
              Cancel
            </Button>
          </div>
          <p className="admin-portal__page-note">
            The slug is immutable after creation. The backend rejects end
            dates before start dates (400) and unknown categories (404).
          </p>

          {editingId != null && (
            <div style={{ marginTop: 'var(--spacing-4, 1.5rem)' }}>
              <h3 className="admin-portal__panel-title">Images</h3>
              <p className="admin-portal__page-note">
                Image metadata only — the actual image files must already be
                hosted and reachable at the URL entered here.
              </p>
              {imagesError && (
                <p className="admin-portal__error" role="alert">
                  Images could not be loaded for this project.
                </p>
              )}
              {images && images.length === 0 && (
                <p className="admin-portal__muted">No images attached to this project yet.</p>
              )}
              {images && images.length > 0 && (
                <ul className="admin-portal__sublist">
                  {images.map(image => (
                    <li key={image.id}>
                      {editingImageId === image.id ? (
                        <form onSubmit={handleSaveImage} className="admin-portal__facts">
                          <label className="admin-portal__filter-field">
                            <span>Image URL *</span>
                            <input
                              className="admin-portal__filter-input"
                              type="url"
                              maxLength={500}
                              value={editImageForm.imageUrl}
                              onChange={event => setEditImageForm(previous => ({ ...previous, imageUrl: event.target.value }))}
                              required
                            />
                          </label>
                          <label className="admin-portal__filter-field">
                            <span>Alt text</span>
                            <input
                              className="admin-portal__filter-input"
                              type="text"
                              maxLength={255}
                              value={editImageForm.altText}
                              onChange={event => setEditImageForm(previous => ({ ...previous, altText: event.target.value }))}
                            />
                          </label>
                          <label className="admin-portal__filter-field">
                            <span>Display order</span>
                            <input
                              className="admin-portal__filter-input"
                              type="number"
                              min={0}
                              value={editImageForm.displayOrder}
                              onChange={event => setEditImageForm(previous => ({ ...previous, displayOrder: event.target.value }))}
                            />
                          </label>
                          <div className="admin-portal__page-actions">
                            <Button type="submit" variant="primary" size="small" disabled={imageSaving}>
                              {imageSaving ? 'Saving…' : 'Save'}
                            </Button>
                            <Button
                              type="button"
                              variant="ghost"
                              size="small"
                              disabled={imageSaving}
                              onClick={() => {
                                setEditingImageId(null)
                                setEditImageForm(EMPTY_IMAGE_FORM)
                                setImageFormError(null)
                              }}
                            >
                              Cancel
                            </Button>
                          </div>
                        </form>
                      ) : (
                        <div>
                          <span className="admin-portal__cell-strong">{image.altText || 'Image'}</span>
                          <span className="admin-portal__subitem-meta"> · {image.imageUrl}</span>
                          <span className="admin-portal__subitem-meta"> · order {image.displayOrder}</span>
                          <div className="admin-portal__page-actions">
                            <Button
                              variant="outline"
                              size="small"
                              disabled={imageSaving || imageActionPendingId != null}
                              onClick={() => {
                                setImageFormError(null)
                                setEditingImageId(image.id)
                                setEditImageForm({
                                  imageUrl: image.imageUrl || '',
                                  altText: image.altText || '',
                                  displayOrder: image.displayOrder != null ? String(image.displayOrder) : '',
                                })
                              }}
                            >
                              Edit
                            </Button>
                            <Button
                              variant="ghost"
                              size="small"
                              disabled={imageSaving || imageActionPendingId != null}
                              onClick={() => handleDeleteImage(image)}
                            >
                              {imageActionPendingId === image.id ? 'Removing…' : 'Remove'}
                            </Button>
                          </div>
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              )}
              <form onSubmit={handleAddImage} className="admin-portal__facts" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                <label className="admin-portal__filter-field">
                  <span>Image URL *</span>
                  <input
                    className="admin-portal__filter-input"
                    type="url"
                    maxLength={500}
                    value={imageForm.imageUrl}
                    onChange={event => setImageForm(previous => ({ ...previous, imageUrl: event.target.value }))}
                    placeholder="https://…"
                    required
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Alt text</span>
                  <input
                    className="admin-portal__filter-input"
                    type="text"
                    maxLength={255}
                    value={imageForm.altText}
                    onChange={event => setImageForm(previous => ({ ...previous, altText: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Display order</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    min={0}
                    value={imageForm.displayOrder}
                    onChange={event => setImageForm(previous => ({ ...previous, displayOrder: event.target.value }))}
                  />
                </label>
                <div className="admin-portal__page-actions">
                  <Button type="submit" variant="primary" size="small" disabled={imageSaving}>
                    {imageSaving ? 'Adding…' : 'Add image'}
                  </Button>
                </div>
              </form>
              {imageFormError && <p className="admin-portal__error" role="alert">{imageFormError}</p>}
            </div>
          )}
        </form>
      )}

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search title/summary</span>
          <input
            id="admin-projects-search"
            type="search"
            className="admin-portal__filter-input"
            placeholder="Search projects…"
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        <label className="admin-portal__filter-field">
          <span>Category</span>
          <select
            id="admin-projects-category"
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
          <span>Publication</span>
          <select
            id="admin-projects-status"
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
          <span>Delivery</span>
          <select
            id="admin-projects-delivery"
            className="admin-portal__filter-select"
            value={projectStatus}
            onChange={event => {
              setPage(0)
              setProjectStatus(event.target.value)
            }}
          >
            {PROJECT_STATUS_OPTIONS.map(option => (
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
            Projects could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No projects match' : 'No projects yet'}
          description={hasFilters
            ? 'No projects match your filters. Try different keywords or clear the filters.'
            : 'Create the first project for the public portfolio.'}
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
                  <th scope="col">Project</th>
                  <th scope="col">Category</th>
                  <th scope="col">Delivery</th>
                  <th scope="col">Publication</th>
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
                    <td>{item.projectStatus || '—'}</td>
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
          <ListPagination data={data} loading={loading} onPage={setPage} noun="project" />
        </>
      )}
    </Container>
  )
}
