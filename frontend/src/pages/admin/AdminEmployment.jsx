import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import EmptyState from '../../components/EmptyState/EmptyState.jsx'
import {
  listAdminCandidates,
  updateAdminCandidate,
  listAdminEmployers,
  updateAdminEmployer,
  listAdminSkills,
  createAdminSkill,
  updateAdminSkill,
  deleteAdminSkill,
  listAdminJobCategories,
  createAdminJobCategory,
  updateAdminJobCategory,
  deleteAdminJobCategory,
} from '../../services/adminService.js'
import {
  describeAdminError,
  isValidKey,
  ListPagination,
  SectionFeedback,
  formatDay,
} from './adminShared.jsx'

const AVAILABILITY_OPTIONS = [
  { value: 'ACTIVELY_LOOKING', label: 'Actively looking' },
  { value: 'OPEN_TO_OFFERS', label: 'Open to offers' },
  { value: 'NOT_LOOKING', label: 'Not looking' },
]

const GENDER_OPTIONS = [
  { value: 'MALE', label: 'MALE' },
  { value: 'FEMALE', label: 'FEMALE' },
  { value: 'OTHER', label: 'OTHER' },
]

const VERIFICATION_OPTIONS = [
  { value: 'UNVERIFIED', label: 'Unverified' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'VERIFIED', label: 'Verified' },
  { value: 'REJECTED', label: 'Rejected' },
]

const EMPLOYER_STATUS_OPTIONS = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'SUSPENDED', label: 'Suspended' },
]

const ACTIVE_INACTIVE_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
]

const SCOPES = ['candidates', 'employers', 'skills', 'categories']

const EMPTY_SKILL_FORM = { name: '', status: 'ACTIVE' }
const EMPTY_CATEGORY_FORM = { name: '', slug: '', description: '', displayOrder: '' }

function salary(value) {
  if (value == null || value === '') return '—'
  return String(value)
}

/**
 * A23 — Admin directory console over the existing A7.1 APIs. One page,
 * four scoped sections sharing the established filters/table/pagination
 * conventions:
 *
 * - Candidates (candidates:manage): list/search/filter, profile PATCH.
 *   There is deliberately no create (profiles come from candidate signup)
 *   and no delete (deleting a profile row would cascade to the user).
 * - Employers (employment:manage): same model; lifecycle via
 *   status/verificationStatus changes only.
 * - Skills (employment:manage): full CRUD; delete cascades into
 *   candidate_skills/job_skills per the documented V8 semantics.
 * - Job categories (employment:manage): full CRUD; the slug is immutable
 *   and delete detaches jobs (never deletes them).
 *
 * All edits ride the backend, which stays authoritative on every rule.
 */
export default function AdminEmployment() {
  const [scope, setScope] = useState('candidates')

  const [page, setPage] = useState(0)
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [availability, setAvailability] = useState('')
  const [gender, setGender] = useState('')
  const [verificationStatus, setVerificationStatus] = useState('')
  const [status, setStatus] = useState('')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const [editingId, setEditingId] = useState(null)
  const [profileForm, setProfileForm] = useState(null)
  const [skillForm, setSkillForm] = useState(EMPTY_SKILL_FORM)
  const [categoryForm, setCategoryForm] = useState(EMPTY_CATEGORY_FORM)
  const [showCreate, setShowCreate] = useState(false)
  const [saving, setSaving] = useState(false)
  const [actionPendingId, setActionPendingId] = useState(null)
  const [feedback, setFeedback] = useState({ error: null, success: null })

  useEffect(() => {
    let active = true
    setLoading(true)
    setError(false)
    const params = { page, size: 20, search }
    let request
    if (scope === 'candidates') {
      request = listAdminCandidates({ ...params, availability, gender })
    } else if (scope === 'employers') {
      request = listAdminEmployers({ ...params, verificationStatus, status })
    } else if (scope === 'skills') {
      request = listAdminSkills({ ...params, status })
    } else {
      request = listAdminJobCategories({ ...params, status })
    }
    request
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
  }, [scope, page, search, availability, gender, verificationStatus, status, reloadKey])

  function resetFeedback() {
    setFeedback({ error: null, success: null })
  }

  function switchScope(next) {
    setScope(next)
    setPage(0)
    setSearchInput('')
    setSearch('')
    setAvailability('')
    setGender('')
    setVerificationStatus('')
    setStatus('')
    setData(null)
    setEditingId(null)
    setProfileForm(null)
    setSkillForm(EMPTY_SKILL_FORM)
    setCategoryForm(EMPTY_CATEGORY_FORM)
    setShowCreate(false)
    resetFeedback()
  }

  function applySearch(event) {
    event.preventDefault()
    setPage(0)
    setSearch(searchInput.trim())
  }

  function clearFilters() {
    setSearchInput('')
    setSearch('')
    setAvailability('')
    setGender('')
    setVerificationStatus('')
    setStatus('')
    setPage(0)
  }

  const hasFilters = Boolean(search || availability || gender || verificationStatus || status)

  function replaceItem(updated) {
    setData(previous => previous ? {
      ...previous,
      items: previous.items.map(entry => (entry.id === updated.id ? updated : entry)),
    } : previous)
  }

  function prependItem(created) {
    setData(previous => previous ? {
      ...previous,
      items: [created, ...previous.items],
      totalItems: previous.totalItems + 1,
    } : previous)
  }

  function removeItem(id) {
    setData(previous => previous ? {
      ...previous,
      items: previous.items.filter(entry => entry.id !== id),
      totalItems: Math.max(0, previous.totalItems - 1),
    } : previous)
  }

  function startEdit(item) {
    resetFeedback()
    setShowCreate(false)
    setEditingId(item.id)
    if (scope === 'candidates') {
      setProfileForm({
        phone: item.phone || '',
        currentLocation: item.currentLocation || '',
        gender: item.gender || '',
        availability: item.availability || '',
        expectedSalaryMin: item.expectedSalaryMin != null ? String(item.expectedSalaryMin) : '',
        expectedSalaryMax: item.expectedSalaryMax != null ? String(item.expectedSalaryMax) : '',
      })
    } else if (scope === 'employers') {
      setProfileForm({
        companyName: item.companyName || '',
        contactPersonName: item.contactPersonName || '',
        contactPhone: item.contactPhone || '',
        address: item.address || '',
        verificationStatus: item.verificationStatus || 'UNVERIFIED',
        status: item.status || 'ACTIVE',
      })
    } else if (scope === 'skills') {
      setSkillForm({ name: item.name || '', status: item.status || 'ACTIVE' })
    } else {
      setCategoryForm({
        name: item.name || '',
        slug: item.slug || '',
        description: item.description || '',
        displayOrder: item.displayOrder != null ? String(item.displayOrder) : '',
      })
    }
  }

  function cancelEdit() {
    resetFeedback()
    setEditingId(null)
    setProfileForm(null)
    setSkillForm(EMPTY_SKILL_FORM)
    setCategoryForm(EMPTY_CATEGORY_FORM)
    setShowCreate(false)
  }

  async function saveProfile(event) {
    event.preventDefault()
    if (saving || editingId == null) return
    setSaving(true)
    resetFeedback()
    try {
      const updated = scope === 'candidates'
        ? await updateAdminCandidate(editingId, profileForm)
        : await updateAdminEmployer(editingId, profileForm)
      replaceItem(updated)
      setEditingId(null)
      setProfileForm(null)
      setFeedback({ error: null, success: scope === 'candidates'
        ? 'Candidate profile updated.'
        : 'Employer profile updated.' })
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, 'The profile could not be saved. Please try again.'),
        success: null,
      })
    } finally {
      setSaving(false)
    }
  }

  async function submitSkillForm(event) {
    event.preventDefault()
    if (saving) return
    if (!skillForm.name.trim()) {
      setFeedback({ error: 'Skill name is required.', success: null })
      return
    }
    setSaving(true)
    resetFeedback()
    try {
      if (editingId != null) {
        const updated = await updateAdminSkill(editingId, skillForm)
        replaceItem(updated)
        setFeedback({ error: null, success: `Skill "${updated.name}" updated.` })
      } else {
        const created = await createAdminSkill(skillForm)
        prependItem(created)
        setFeedback({ error: null, success: `Skill "${created.name}" created.` })
      }
      cancelEdit()
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, 'The skill could not be saved. Please try again.'),
        success: null,
      })
    } finally {
      setSaving(false)
    }
  }

  async function submitCategoryForm(event) {
    event.preventDefault()
    if (saving) return
    const name = categoryForm.name.trim()
    const slug = categoryForm.slug.trim()
    const displayOrder = categoryForm.displayOrder.trim()
    if (!name || (editingId == null && !isValidKey(slug))) {
      setFeedback({
        error: !name
          ? 'Category name is required.'
          : 'Slug must contain only lowercase letters, digits, dot, dash or underscore.',
        success: null,
      })
      return
    }
    if (editingId == null && displayOrder === '') {
      setFeedback({ error: 'Display order is required.', success: null })
      return
    }
    setSaving(true)
    resetFeedback()
    try {
      if (editingId != null) {
        const updated = await updateAdminJobCategory(editingId, categoryForm)
        replaceItem(updated)
        setFeedback({ error: null, success: `Category "${updated.name}" updated.` })
      } else {
        const created = await createAdminJobCategory(categoryForm)
        prependItem(created)
        setFeedback({ error: null, success: `Category "${created.name}" created.` })
      }
      cancelEdit()
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, 'The category could not be saved. Please try again.'),
        success: null,
      })
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(item, noun) {
    if (actionPendingId != null || saving) return
    const cascadeNote = noun === 'skill'
      ? 'Skill requirements on jobs and candidate skill entries that reference it are removed too (documented cascade).'
      : 'Jobs referencing this category are detached (uncategorized), never deleted.'
    const confirmed = window.confirm(`Delete the ${noun} "${item.name}"? ${cascadeNote}`)
    if (!confirmed) return
    setActionPendingId(item.id)
    resetFeedback()
    try {
      if (noun === 'skill') {
        await deleteAdminSkill(item.id)
      } else {
        await deleteAdminJobCategory(item.id)
      }
      removeItem(item.id)
      setFeedback({ error: null, success: `The ${noun} "${item.name}" was deleted.` })
    } catch (err) {
      setFeedback({
        error: describeAdminError(err, `The ${noun} could not be deleted. Please try again.`),
        success: null,
      })
    } finally {
      setActionPendingId(null)
    }
  }

  const items = (data && data.items) || []
  const formOpen = showCreate || editingId != null

  return (
    <Container>
      <div className="admin-portal__row" style={{ marginBottom: 'var(--spacing-4, 1.5rem)' }}>
        <div>
          <h1 className="admin-portal__title">Employment directory</h1>
          <p className="admin-portal__muted">
            Candidate and employer profiles plus the skills and job categories
            that power matching. Profiles belong to their user accounts: they
            are edited here, never created or deleted.
          </p>
        </div>
        <div className="admin-portal__page-actions">
          {SCOPES.map(name => (
            <Button
              key={name}
              variant={scope === name ? 'primary' : 'outline'}
              size="small"
              onClick={() => switchScope(name)}
            >
              {name.charAt(0).toUpperCase() + name.slice(1)}
            </Button>
          ))}
        </div>
      </div>

      <form className="admin-portal__filters" onSubmit={applySearch}>
        <label className="admin-portal__filter-field">
          <span>Search</span>
          <input
            type="search"
            className="admin-portal__filter-input"
            placeholder={scope === 'candidates'
              ? 'Search candidate profiles…'
              : scope === 'employers'
                ? 'Search companies…'
                : 'Search names…'}
            value={searchInput}
            onChange={event => setSearchInput(event.target.value)}
          />
        </label>
        {scope === 'candidates' && (
          <>
            <label className="admin-portal__filter-field">
              <span>Availability</span>
              <select
                className="admin-portal__filter-select"
                value={availability}
                onChange={event => {
                  setPage(0)
                  setAvailability(event.target.value)
                }}
              >
                <option value="">All availability</option>
                {AVAILABILITY_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
            <label className="admin-portal__filter-field">
              <span>Gender</span>
              <select
                className="admin-portal__filter-select"
                value={gender}
                onChange={event => {
                  setPage(0)
                  setGender(event.target.value)
                }}
              >
                <option value="">All</option>
                {GENDER_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </>
        )}
        {scope === 'employers' && (
          <>
            <label className="admin-portal__filter-field">
              <span>Verification</span>
              <select
                className="admin-portal__filter-select"
                value={verificationStatus}
                onChange={event => {
                  setPage(0)
                  setVerificationStatus(event.target.value)
                }}
              >
                <option value="">All verification</option>
                {VERIFICATION_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
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
                <option value="">All statuses</option>
                {EMPLOYER_STATUS_OPTIONS.map(option => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </label>
          </>
        )}
        {(scope === 'skills' || scope === 'categories') && (
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
              {ACTIVE_INACTIVE_OPTIONS.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </label>
        )}
        <Button type="submit" variant="primary" size="medium">Apply</Button>
        {hasFilters && (
          <Button type="button" variant="ghost" size="medium" onClick={clearFilters}>
            Clear filters
          </Button>
        )}
      </form>

      {(scope === 'skills' || scope === 'categories') && (
        <div className="admin-portal__page-actions" style={{ margin: 'var(--spacing-3, 0.75rem) 0' }}>
          <Button
            variant="primary"
            size="medium"
            disabled={saving}
            onClick={() => {
              resetFeedback()
              setEditingId(null)
              setProfileForm(null)
              setSkillForm(EMPTY_SKILL_FORM)
              setCategoryForm(EMPTY_CATEGORY_FORM)
              setShowCreate(value => !value)
            }}
          >
            {showCreate ? 'Close form' : scope === 'skills' ? 'New skill' : 'New category'}
          </Button>
        </div>
      )}

      {formOpen && (scope === 'candidates' || scope === 'employers') && editingId != null && (
        <form
          className="admin-portal__panel"
          style={{ marginBottom: 'var(--spacing-3, 0.75rem)' }}
          onSubmit={saveProfile}
        >
          <h2 className="admin-portal__panel-title">
            Edit {scope === 'candidates' ? 'candidate' : 'employer'} profile #{editingId}
          </h2>
          <p className="admin-portal__muted">
            The linked user account is permanent; profiles are never deleted.
            {scope === 'employers'
              ? ' Lifecycle runs through verification and account status.'
              : ''}
          </p>
          <div className="admin-portal__facts">
            {scope === 'candidates' ? (
              <>
                <label className="admin-portal__filter-field">
                  <span>Phone</span>
                  <input
                    className="admin-portal__filter-input"
                    type="tel"
                    maxLength={50}
                    value={profileForm.phone}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, phone: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Current location</span>
                  <input
                    className="admin-portal__filter-input"
                    type="text"
                    maxLength={255}
                    value={profileForm.currentLocation}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, currentLocation: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Gender</span>
                  <select
                    className="admin-portal__filter-select"
                    value={profileForm.gender}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, gender: event.target.value }))}
                  >
                    <option value="">Not set</option>
                    {GENDER_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Availability</span>
                  <select
                    className="admin-portal__filter-select"
                    value={profileForm.availability}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, availability: event.target.value }))}
                  >
                    <option value="">Not set</option>
                    {AVAILABILITY_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Expected salary min</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    step="0.01"
                    min={0}
                    value={profileForm.expectedSalaryMin}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, expectedSalaryMin: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Expected salary max</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    step="0.01"
                    min={0}
                    value={profileForm.expectedSalaryMax}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, expectedSalaryMax: event.target.value }))}
                  />
                </label>
              </>
            ) : (
              <>
                <label className="admin-portal__filter-field">
                  <span>Company name</span>
                  <input
                    className="admin-portal__filter-input"
                    type="text"
                    maxLength={255}
                    value={profileForm.companyName}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, companyName: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Contact person</span>
                  <input
                    className="admin-portal__filter-input"
                    type="text"
                    maxLength={255}
                    value={profileForm.contactPersonName}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, contactPersonName: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Contact phone</span>
                  <input
                    className="admin-portal__filter-input"
                    type="tel"
                    maxLength={50}
                    value={profileForm.contactPhone}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, contactPhone: event.target.value }))}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Verification status</span>
                  <select
                    className="admin-portal__filter-select"
                    value={profileForm.verificationStatus}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, verificationStatus: event.target.value }))}
                  >
                    {VERIFICATION_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Account status</span>
                  <select
                    className="admin-portal__filter-select"
                    value={profileForm.status}
                    disabled={saving}
                    onChange={event => setProfileForm(previous => ({ ...previous, status: event.target.value }))}
                  >
                    {EMPLOYER_STATUS_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
              </>
            )}
          </div>
          {scope === 'employers' && (
            <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
              <span>Address</span>
              <textarea
                className="admin-portal__filter-input"
                rows={2}
                maxLength={500}
                value={profileForm.address}
                disabled={saving}
                onChange={event => setProfileForm(previous => ({ ...previous, address: event.target.value }))}
              />
            </label>
          )}
          {feedback.error && <p className="admin-portal__error" role="alert">{feedback.error}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : 'Save profile'}
            </Button>
            <Button type="button" variant="ghost" size="medium" disabled={saving} onClick={cancelEdit}>
              Cancel
            </Button>
          </div>
        </form>
      )}

      {formOpen && scope === 'skills' && (
        <form
          className="admin-portal__panel"
          style={{ marginBottom: 'var(--spacing-3, 0.75rem)' }}
          onSubmit={submitSkillForm}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? 'Edit skill' : 'New skill'}
          </h2>
          <div className="admin-portal__facts">
            <label className="admin-portal__filter-field">
              <span>Name *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={100}
                value={skillForm.name}
                disabled={saving}
                onChange={event => setSkillForm(previous => ({ ...previous, name: event.target.value }))}
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Status</span>
              <select
                className="admin-portal__filter-select"
                value={skillForm.status}
                disabled={saving}
                onChange={event => setSkillForm(previous => ({ ...previous, status: event.target.value }))}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
          </div>
          {feedback.error && <p className="admin-portal__error" role="alert">{feedback.error}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save skill' : 'Create skill'}
            </Button>
            <Button type="button" variant="ghost" size="medium" disabled={saving} onClick={cancelEdit}>
              Cancel
            </Button>
          </div>
          <p className="admin-portal__page-note">
            Duplicate names are rejected. Deleting a skill removes its
            candidate and job skill entries with it.
          </p>
        </form>
      )}

      {formOpen && scope === 'categories' && (
        <form
          className="admin-portal__panel"
          style={{ marginBottom: 'var(--spacing-3, 0.75rem)' }}
          onSubmit={submitCategoryForm}
        >
          <h2 className="admin-portal__panel-title">
            {editingId != null ? 'Edit job category' : 'New job category'}
          </h2>
          <div className="admin-portal__facts">
            <label className="admin-portal__filter-field">
              <span>Name *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={100}
                value={categoryForm.name}
                disabled={saving}
                onChange={event => setCategoryForm(previous => ({ ...previous, name: event.target.value }))}
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Slug *</span>
              <input
                className="admin-portal__filter-input"
                type="text"
                maxLength={120}
                value={categoryForm.slug}
                disabled={saving || editingId != null}
                onChange={event => setCategoryForm(previous => ({ ...previous, slug: event.target.value }))}
                placeholder="engineering"
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Display order *</span>
              <input
                className="admin-portal__filter-input"
                type="number"
                min={0}
                value={categoryForm.displayOrder}
                disabled={saving}
                onChange={event => setCategoryForm(previous => ({ ...previous, displayOrder: event.target.value }))}
                required
              />
            </label>
            <label className="admin-portal__filter-field">
              <span>Status</span>
              <select
                className="admin-portal__filter-select"
                value={categoryForm.status || 'ACTIVE'}
                disabled={saving}
                onChange={event => setCategoryForm(previous => ({ ...previous, status: event.target.value }))}
              >
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </select>
            </label>
          </div>
          <label className="admin-portal__filter-field" style={{ display: 'block', marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <span>Description</span>
            <textarea
              className="admin-portal__filter-input"
              rows={2}
              maxLength={500}
              value={categoryForm.description}
              disabled={saving}
              onChange={event => setCategoryForm(previous => ({ ...previous, description: event.target.value }))}
            />
          </label>
          {feedback.error && <p className="admin-portal__error" role="alert">{feedback.error}</p>}
          <div className="admin-portal__page-actions" style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
            <Button type="submit" variant="primary" size="medium" disabled={saving}>
              {saving ? 'Saving…' : editingId != null ? 'Save category' : 'Create category'}
            </Button>
            <Button type="button" variant="ghost" size="medium" disabled={saving} onClick={cancelEdit}>
              Cancel
            </Button>
          </div>
          <p className="admin-portal__page-note">
            The slug is permanent. Deleting a category detaches referencing
            jobs (uncategorized) — jobs are never deleted.
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
            This section could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && !error && items.length === 0 && (
        <EmptyState
          title={hasFilters ? 'No records match' : scope === 'candidates'
            ? 'No candidate profiles yet'
            : scope === 'employers'
              ? 'No employer profiles yet'
              : scope === 'skills' ? 'No skills yet' : 'No job categories yet'}
          description={hasFilters
            ? 'Nothing matches your filters. Try different keywords or clear the filters.'
            : 'Records will appear here as people and jobs use the platform.'}
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
                  {scope === 'candidates' && (
                    <>
                      <th scope="col">Candidate</th>
                      <th scope="col">Phone</th>
                      <th scope="col">Location</th>
                      <th scope="col">Availability</th>
                      <th scope="col">Expected salary</th>
                      <th scope="col">Updated</th>
                      <th scope="col">Actions</th>
                    </>
                  )}
                  {scope === 'employers' && (
                    <>
                      <th scope="col">Company</th>
                      <th scope="col">Contact</th>
                      <th scope="col">Phone</th>
                      <th scope="col">Verification</th>
                      <th scope="col">Status</th>
                      <th scope="col">Updated</th>
                      <th scope="col">Actions</th>
                    </>
                  )}
                  {scope === 'skills' && (
                    <>
                      <th scope="col">Skill</th>
                      <th scope="col">Status</th>
                      <th scope="col">Updated</th>
                      <th scope="col">Actions</th>
                    </>
                  )}
                  {scope === 'categories' && (
                    <>
                      <th scope="col">Category</th>
                      <th scope="col">Order</th>
                      <th scope="col">Status</th>
                      <th scope="col">Updated</th>
                      <th scope="col">Actions</th>
                    </>
                  )}
                </tr>
              </thead>
              <tbody>
                {items.map(item => (
                  scope === 'candidates' ? (
                    <tr key={item.id}>
                      <td>
                        <div className="admin-portal__cell-strong">Candidate #{item.id}</div>
                        <div className="admin-portal__cell-muted">user {item.userId}</div>
                      </td>
                      <td className="admin-portal__cell-muted">{item.phone || '—'}</td>
                      <td className="admin-portal__cell-muted">{item.currentLocation || '—'}</td>
                      <td>
                        <span className={`admin-portal__status admin-portal__status--${item.availability}`}>
                          {(item.availability || '—').replace(/_/g, ' ')}
                        </span>
                      </td>
                      <td className="admin-portal__cell-muted">
                        {salary(item.expectedSalaryMin)} – {salary(item.expectedSalaryMax)}
                      </td>
                      <td className="admin-portal__cell-muted">{formatDay(item.updatedAt)}</td>
                      <td>
                        <Button
                          variant="outline"
                          size="small"
                          disabled={saving || actionPendingId != null}
                          onClick={() => (editingId === item.id ? cancelEdit() : startEdit(item))}
                        >
                          {editingId === item.id ? 'Close editor' : 'Edit profile'}
                        </Button>
                      </td>
                    </tr>
                  ) : scope === 'employers' ? (
                    <tr key={item.id}>
                      <td>
                        <div className="admin-portal__cell-strong">{item.companyName}</div>
                        <div className="admin-portal__cell-muted">
                          user {item.userId}{item.websiteUrl ? ` · ${item.websiteUrl}` : ''}
                        </div>
                      </td>
                      <td className="admin-portal__cell-muted">{item.contactPersonName || '—'}</td>
                      <td className="admin-portal__cell-muted">{item.contactPhone || '—'}</td>
                      <td>
                        <span className={`admin-portal__status admin-portal__status--${item.verificationStatus}`}>
                          {item.verificationStatus || '—'}
                        </span>
                      </td>
                      <td>
                        <span className={`admin-portal__status admin-portal__status--${item.status}`}>
                          {item.status || '—'}
                        </span>
                      </td>
                      <td className="admin-portal__cell-muted">{formatDay(item.updatedAt)}</td>
                      <td>
                        <Button
                          variant="outline"
                          size="small"
                          disabled={saving || actionPendingId != null}
                          onClick={() => (editingId === item.id ? cancelEdit() : startEdit(item))}
                        >
                          {editingId === item.id ? 'Close editor' : 'Edit profile'}
                        </Button>
                      </td>
                    </tr>
                  ) : scope === 'skills' ? (
                    <tr key={item.id}>
                      <td>
                        <div className="admin-portal__cell-strong">{item.name}</div>
                      </td>
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
                            onClick={() => (editingId === item.id ? cancelEdit() : startEdit(item))}
                          >
                            {editingId === item.id ? 'Close editor' : 'Edit'}
                          </Button>
                          <Button
                            variant="ghost"
                            size="small"
                            disabled={saving || actionPendingId != null}
                            onClick={() => handleDelete(item, 'skill')}
                          >
                            {actionPendingId === item.id ? 'Working…' : 'Delete'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ) : (
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
                            onClick={() => (editingId === item.id ? cancelEdit() : startEdit(item))}
                          >
                            {editingId === item.id ? 'Close editor' : 'Edit'}
                          </Button>
                          <Button
                            variant="ghost"
                            size="small"
                            disabled={saving || actionPendingId != null}
                            onClick={() => handleDelete(item, 'category')}
                          >
                            {actionPendingId === item.id ? 'Working…' : 'Delete'}
                          </Button>
                        </div>
                      </td>
                    </tr>
                  )
                ))}
              </tbody>
            </table>
          </div>

          <ListPagination
            data={data}
            loading={loading}
            onPage={setter => setPage(setter)}
            noun={scope === 'candidates'
              ? 'candidate'
              : scope === 'employers'
                ? 'employer'
                : scope === 'skills' ? 'skill' : 'category'}
          />

          <p className="admin-portal__page-note">
            {scope === 'candidates' && 'There is no candidate create or delete here: profiles are created at signup and deleting one would cascade to the user account (backend rule).'}
            {scope === 'employers' && 'There is no employer create or delete here: profiles are created at signup; lifecycle runs through verification and account status.'}
            {scope === 'skills' && 'Skill deletion cascades to candidate skills and job skill requirements (documented V8 behavior).'}
            {scope === 'categories' && 'Category deletion detaches referencing jobs (category removed, job kept).'}
          </p>
        </>
      )}
    </Container>
  )
}
