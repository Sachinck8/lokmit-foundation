import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import {
  getJob,
  createJob,
  updateJob,
  publishJob,
  closeJob,
  archiveJob,
  deleteJob,
  getJobSkills,
  assignJobSkill,
  removeJobSkill,
  listJobCategories,
  listEmployers,
  listSkillsCatalog,
} from '../../services/adminService.js'

const STATUS_LABELS = {
  DRAFT: 'Draft',
  PUBLISHED: 'Published',
  CLOSED: 'Closed',
  ARCHIVED: 'Archived',
}

const TYPE_OPTIONS = [
  { value: 'FULL_TIME', label: 'Full time' },
  { value: 'PART_TIME', label: 'Part time' },
  { value: 'CONTRACT', label: 'Contract' },
  { value: 'INTERNSHIP', label: 'Internship' },
  { value: 'TEMPORARY', label: 'Temporary' },
]

const MODE_OPTIONS = [
  { value: 'ONSITE', label: 'Onsite' },
  { value: 'REMOTE', label: 'Remote' },
  { value: 'HYBRID', label: 'Hybrid' },
]

const CURRENCY_OPTIONS = ['INR', 'USD', 'EUR', 'GBP', 'AUD', 'CAD']

const EMPTY_FORM = {
  employerId: '',
  title: '',
  description: '',
  requirements: '',
  categoryId: '',
  employmentType: '',
  workMode: '',
  workLocation: '',
  salaryMin: '',
  salaryMax: '',
  salaryCurrency: 'INR',
  openings: '',
  applicationDeadline: '',
}

/**
 * Backend-authoritative lifecycle (mirrors JobService exactly — the UI never
 * invents lifecycle rules):
 *  - publish:  DRAFT → PUBLISHED (stamps published_at; anything else → 400/409)
 *  - close:    PUBLISHED → CLOSED (anything else → 400)
 *  - archive:  DRAFT/PUBLISHED/CLOSED → ARCHIVED (terminal; already archived → 409)
 *  - delete:   DRAFT only (server-side rule); permanent
 */
function availableActions(status) {
  if (status === 'DRAFT') {
    return [
      { key: 'publish', label: 'Publish', kind: 'primary' },
      { key: 'archive', label: 'Archive', kind: 'outline', needsConfirm: true },
      { key: 'delete', label: 'Delete draft', kind: 'ghost', needsConfirm: true },
    ]
  }
  if (status === 'PUBLISHED') {
    return [
      { key: 'close', label: 'Close', kind: 'primary', needsConfirm: true },
      { key: 'archive', label: 'Archive', kind: 'outline', needsConfirm: true },
    ]
  }
  if (status === 'CLOSED') {
    return [{ key: 'archive', label: 'Archive', kind: 'outline', needsConfirm: true }]
  }
  return [] // ARCHIVED is terminal
}

function formatDateTime(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-IN', {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

function formatSalary(min, max, currency) {
  if (min == null && max == null) return '—'
  const code = currency ? ` ${currency}` : ''
  if (min != null && max != null) return `${min} – ${max}${code}`
  return `${min != null ? min : max}${code}`
}

function optionLabel(options, code) {
  const match = options.find(option => option.value === code)
  return match ? match.label : code || '—'
}

function snapshotOf(job) {
  return {
    employerId: '',
    title: job.title || '',
    description: job.description || '',
    requirements: job.requirements || '',
    categoryId: job.category ? String(job.category.id) : '',
    employmentType: job.employmentType || '',
    workMode: job.workMode || '',
    workLocation: job.workLocation || '',
    salaryMin: job.salaryMin != null ? String(job.salaryMin) : '',
    salaryMax: job.salaryMax != null ? String(job.salaryMax) : '',
    salaryCurrency: job.salaryCurrency || 'INR',
    openings: job.openings != null ? String(job.openings) : '',
    applicationDeadline: job.applicationDeadline || '',
  }
}

/**
 * Builds a PATCH payload containing only the fields the admin actually
 * changed, so the backend's explicit-null clears (requirements, location,
 * salary pair, deadline, category detach) stay intentional and unchanged
 * fields can never be wiped by accident. Returns null when nothing changed.
 */
function buildUpdatePayload(form, original) {
  const payload = {}
  let changed = false

  function track(key, value) {
    if (value !== original[key]) {
      payload[key] = value
      changed = true
    }
  }

  const clearableText = key => {
    const value = form[key]
    track(key, String(value).trim() === '' ? null : String(value).trim())
  }
  const clearableNumber = key => {
    const value = String(form[key]).trim()
    track(key, value === '' ? null : Number(value))
  }

  track('title', String(form.title).trim())
  track('description', String(form.description).trim())
  clearableText('requirements')
  const categoryId = String(form.categoryId).trim()
  track('categoryId', categoryId === '' ? null : Number(categoryId))
  track('employmentType', form.employmentType || null)
  track('workMode', form.workMode || null)
  clearableText('workLocation')
  clearableNumber('salaryMin')
  clearableNumber('salaryMax')
  track('salaryCurrency', String(form.salaryCurrency || '').trim() || null)
  clearableNumber('openings')
  const deadline = String(form.applicationDeadline || '').trim()
  track('applicationDeadline', deadline === '' ? null : deadline)

  return changed ? payload : null
}

function extractErrorMessage(err, fallback) {
  const data = err && err.response && err.response.data
  if (data && typeof data.message === 'string' && data.message.trim() !== '') {
    return data.message
  }
  return fallback
}

/**
 * A19 — Admin job detail / create / edit page over the EXISTING admin job
 * lifecycle APIs (list facts + edit form + publish/close/archive/delete +
 * skill-requirement management + reference lists). /admin-panel/jobs/new
 * creates a DRAFT job; everything else is read from and written to the
 * backend, which remains authoritative for all rules.
 */
export default function AdminJobDetail() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const isNew = jobId === 'new'

  const [job, setJob] = useState(null)
  const [loading, setLoading] = useState(!isNew)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  const [editing, setEditing] = useState(isNew)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState('')
  const [categories, setCategories] = useState([])
  const [employers, setEmployers] = useState([])
  const [refError, setRefError] = useState(false)
  const originalRef = useRef(null)

  const [actionBusy, setActionBusy] = useState('')
  const [actionError, setActionError] = useState('')

  const [skills, setSkills] = useState([])
  const [skillsLoading, setSkillsLoading] = useState(false)
  const [skillsError, setSkillsError] = useState(false)
  const [skillsCatalog, setSkillsCatalog] = useState([])
  const [newSkillId, setNewSkillId] = useState('')
  const [skillBusy, setSkillBusy] = useState(false)
  const [skillError, setSkillError] = useState('')

  useEffect(() => {
    if (isNew) {
      setJob(null)
      setEditing(true)
      setForm(EMPTY_FORM)
      originalRef.current = null
      setLoading(false)
      setNotFound(false)
      setError(false)
      return
    }
    let active = true
    setLoading(true)
    setError(false)
    setNotFound(false)
    setEditing(false)
    getJob(jobId)
      .then(data => {
        if (active) setJob(data)
      })
      .catch(err => {
        if (!active) return
        if (err && err.response && err.response.status === 404) setNotFound(true)
        else setError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [jobId, isNew])

  const loadSkills = useCallback(() => {
    let active = true
    setSkillsLoading(true)
    setSkillsError(false)
    getJobSkills(jobId)
      .then(items => {
        if (active) setSkills(Array.isArray(items) ? items : [])
      })
      .catch(() => {
        if (active) setSkillsError(true)
      })
      .finally(() => {
        if (active) setSkillsLoading(false)
      })
    return () => {
      active = false
    }
  }, [jobId])

  useEffect(() => {
    if (isNew) return undefined
    return loadSkills()
  }, [isNew, loadSkills, reloadKey])

  // Skill catalog for the assignment dropdown (single request, ≤100 rows).
  useEffect(() => {
    if (isNew) return undefined
    let active = true
    listSkillsCatalog()
      .then(items => {
        if (active) setSkillsCatalog(Array.isArray(items) ? items : [])
      })
      .catch(() => {
        if (active) setSkillsCatalog([])
      })
    return () => {
      active = false
    }
  }, [isNew])

  function setField(key, value) {
    setForm(current => ({ ...current, [key]: value }))
  }

  function startEditing() {
    originalRef.current = snapshotOf(job)
    setForm(originalRef.current)
    setFormError('')
    setRefError(false)
    Promise.all([
      listJobCategories().catch(() => 'error'),
      listEmployers().catch(() => 'error'),
    ]).then(([cats, emps]) => {
      const catsOk = Array.isArray(cats)
      const empsOk = Array.isArray(emps)
      setCategories(catsOk ? cats : [])
      setEmployers(empsOk ? emps : [])
      setRefError(!catsOk || !empsOk)
    })
  }

  function cancelEditing() {
    setEditing(false)
    setFormError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setFormError('')
    try {
      if (isNew) {
        const payload = {
          slug: String(form.title || '')
            .toLowerCase()
            .replace(/[^a-z0-9]+/g, '-')
            .replace(/^-+|-+$/g, '')
            .slice(0, 180),
          title: String(form.title || '').trim(),
          description: String(form.description || '').trim(),
          employerId: Number(form.employerId),
        }
        if (String(form.categoryId).trim() !== '') payload.categoryId = Number(form.categoryId)
        if (form.employmentType) payload.employmentType = form.employmentType
        if (form.workMode) payload.workMode = form.workMode
        if (String(form.workLocation).trim() !== '') payload.workLocation = String(form.workLocation).trim()
        if (String(form.salaryMin).trim() !== '') payload.salaryMin = Number(form.salaryMin)
        if (String(form.salaryMax).trim() !== '') payload.salaryMax = Number(form.salaryMax)
        if (String(form.salaryCurrency || '').trim() !== '') payload.salaryCurrency = String(form.salaryCurrency).trim()
        if (String(form.openings).trim() !== '') payload.openings = Number(form.openings)
        if (String(form.applicationDeadline || '').trim() !== '') payload.applicationDeadline = String(form.applicationDeadline).trim()
        const created = await createJob(payload)
        navigate(`/admin-panel/jobs/${created.id}`, { replace: true })
      } else {
        const payload = buildUpdatePayload(form, originalRef.current || {})
        if (payload === null) {
          setEditing(false)
          return
        }
        const updated = await updateJob(jobId, payload)
        setJob(updated)
        setEditing(false)
      }
    } catch (err) {
      setFormError(extractErrorMessage(err, 'The job could not be saved. Please check the fields and try again.'))
    } finally {
      setSaving(false)
    }
  }

  async function runAction(action) {
    setActionBusy(action)
    setActionError('')
    try {
      if (action === 'publish') setJob(await publishJob(jobId))
      if (action === 'close') setJob(await closeJob(jobId))
      if (action === 'archive') setJob(await archiveJob(jobId))
      if (action === 'delete') {
        await deleteJob(jobId)
        navigate('/admin-panel/jobs')
      }
    } catch (err) {
      setActionError(extractErrorMessage(err, 'The action could not be completed. Please try again.'))
    } finally {
      setActionBusy('')
    }
  }

  function handleAction(action) {
    setActionError('')
    if (action.needsConfirm && !window.confirm(`Are you sure you want to ${action.label.toLowerCase()} this job?`)) {
      return
    }
    runAction(action.key)
  }

  async function handleAssignSkill(event) {
    event.preventDefault()
    if (!newSkillId) return
    setSkillBusy(true)
    setSkillError('')
    try {
      await assignJobSkill(jobId, Number(newSkillId))
      setNewSkillId('')
      loadSkills()
    } catch (err) {
      setSkillError(extractErrorMessage(err, 'The skill could not be assigned. Please try again.'))
    } finally {
      setSkillBusy(false)
    }
  }

  async function handleRemoveSkill(skillId) {
    setSkillBusy(true)
    setSkillError('')
    try {
      await removeJobSkill(jobId, skillId)
      loadSkills()
    } catch (err) {
      setSkillError(extractErrorMessage(err, 'The skill could not be removed. Please try again.'))
    } finally {
      setSkillBusy(false)
    }
  }

  const actions = job && !editing ? availableActions(job.status) : []
  const assignedSkillIds = new Set(skills.map(skill => skill.skillId))
  const assignableSkills = skillsCatalog.filter(skill => !assignedSkillIds.has(skill.id))
  const currencyOptions = job && job.salaryCurrency && !CURRENCY_OPTIONS.includes(job.salaryCurrency)
    ? [...CURRENCY_OPTIONS, job.salaryCurrency]
    : CURRENCY_OPTIONS

  return (
    <Container>
      <div className="admin-portal__back">
        <Link className="admin-portal__back-link" to="/admin-panel/jobs">
          ← Back to jobs
        </Link>
      </div>

      {loading && (
        <div className="admin-portal__panel" aria-hidden="true">
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
        </div>
      )}

      {!loading && notFound && (
        <div className="admin-portal__panel">
          <h1 className="admin-portal__title">Job not found</h1>
          <p className="admin-portal__muted">
            This job does not exist or is no longer available.
          </p>
          <Link to="/admin-panel/jobs">
            <Button variant="primary" size="medium">Back to jobs</Button>
          </Link>
        </div>
      )}

      {!loading && error && (
        <div className="admin-portal__panel">
          <h1 className="admin-portal__title">Something went wrong</h1>
          <p className="admin-portal__muted">
            This job could not be loaded right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
            Try again
          </Button>
        </div>
      )}

      {!loading && (isNew || job) && (
        <div className="admin-portal__panel">
          <div className="admin-portal__row">
            <h1 className="admin-portal__title">
              {job ? `Job #${job.id} — ${job.title}` : 'New job'}
            </h1>
            {job && !editing && (
              <span className={`admin-portal__status admin-portal__status--${job.status}`}>
                {STATUS_LABELS[job.status] || job.status}
              </span>
            )}
          </div>

          {actionError && <p className="admin-portal__error">{actionError}</p>}

          {/* Facts / read view ----------------------------------------- */}
          {job && !editing && (
            <>
              <section className="admin-portal__section">
                <h2 className="admin-portal__panel-title">Job details</h2>
                <dl className="admin-portal__facts">
                  <div className="admin-portal__fact">
                    <dt>Employer</dt>
                    <dd>{job.employer ? job.employer.companyName : '—'}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Category</dt>
                    <dd>{job.category ? job.category.name : '—'}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Employment type</dt>
                    <dd>{optionLabel(TYPE_OPTIONS, job.employmentType)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Work mode</dt>
                    <dd>{optionLabel(MODE_OPTIONS, job.workMode)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Location</dt>
                    <dd>{job.workLocation || '—'}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Salary</dt>
                    <dd>{formatSalary(job.salaryMin, job.salaryMax, job.salaryCurrency)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Openings</dt>
                    <dd>{job.openings != null ? job.openings : '—'}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Application deadline</dt>
                    <dd>{formatDate(job.applicationDeadline)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Published</dt>
                    <dd>{formatDateTime(job.publishedAt)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Created</dt>
                    <dd>{formatDateTime(job.createdAt)}</dd>
                  </div>
                  <div className="admin-portal__fact">
                    <dt>Last updated</dt>
                    <dd>{formatDateTime(job.updatedAt)}</dd>
                  </div>
                </dl>
              </section>

              <section className="admin-portal__section">
                <h2 className="admin-portal__panel-title">Description</h2>
                <p className="admin-portal__muted" style={{ whiteSpace: 'pre-wrap' }}>
                  {job.description || '—'}
                </p>
                {job.requirements && (
                  <>
                    <h2 className="admin-portal__panel-title" style={{ marginTop: 'var(--spacing-4, 1.5rem)' }}>
                      Requirements
                    </h2>
                    <p className="admin-portal__muted" style={{ whiteSpace: 'pre-wrap' }}>
                      {job.requirements}
                    </p>
                  </>
                )}
              </section>

              <section className="admin-portal__section">
                <div className="admin-portal__row">
                  <h2 className="admin-portal__panel-title">Lifecycle</h2>
                  {job.status === 'PUBLISHED' && (
                    <span className="admin-portal__cell-muted">
                      This job is publicly visible on /jobs.
                    </span>
                  )}
                </div>
                <div className="admin-portal__actions-row">
                  {actions.map(action => (
                    <Button
                      key={action.key}
                      variant={action.kind === 'primary' ? 'primary' : action.kind === 'outline' ? 'outline' : 'ghost'}
                      size="small"
                      disabled={actionBusy !== ''}
                      onClick={() => handleAction(action)}
                    >
                      {actionBusy === action.key ? 'Working…' : action.label}
                    </Button>
                  ))}
                  <Button
                    variant="outline"
                    size="small"
                    disabled={actionBusy !== ''}
                    onClick={startEditing}
                  >
                    Edit details
                  </Button>
                </div>
                {actions.length === 0 && (
                  <p className="admin-portal__muted">
                    This job is archived. Archived jobs have no further lifecycle actions.
                  </p>
                )}
              </section>

              {/* Skill requirements ------------------------------------- */}
              <section className="admin-portal__section">
                <h2 className="admin-portal__panel-title">Skill requirements</h2>
                {skillsLoading && <p className="admin-portal__muted">Loading skills…</p>}
                {skillsError && (
                  <div>
                    <p className="admin-portal__muted">Skills could not be loaded right now.</p>
                    <Button variant="outline" size="small" onClick={loadSkills}>
                      Try again
                    </Button>
                  </div>
                )}
                {!skillsLoading && !skillsError && (
                  <>
                    {skills.length === 0 ? (
                      <p className="admin-portal__muted">No skill requirements assigned yet.</p>
                    ) : (
                      <ul className="admin-portal__sublist">
                        {skills.map(skill => (
                          <li key={skill.skillId}>
                            <span className="admin-portal__subitem-title">{skill.skillName}</span>
                            <Button
                              variant="ghost"
                              size="small"
                              disabled={skillBusy}
                              onClick={() => handleRemoveSkill(skill.skillId)}
                            >
                              Remove
                            </Button>
                          </li>
                        ))}
                      </ul>
                    )}
                    {assignableSkills.length > 0 && (
                      <form className="admin-portal__filters" onSubmit={handleAssignSkill} style={{ marginTop: 'var(--spacing-3, 0.75rem)' }}>
                        <label className="admin-portal__filter-field">
                          <span>Add a skill requirement</span>
                          <select
                            className="admin-portal__filter-select"
                            value={newSkillId}
                            onChange={event => setNewSkillId(event.target.value)}
                          >
                            <option value="">Select a skill…</option>
                            {assignableSkills.map(skill => (
                              <option key={skill.id} value={skill.id}>{skill.name}</option>
                            ))}
                          </select>
                        </label>
                        <Button type="submit" variant="primary" size="medium" disabled={skillBusy || !newSkillId}>
                          Assign
                        </Button>
                      </form>
                    )}
                    {skillError && <p className="admin-portal__error">{skillError}</p>}
                  </>
                )}
              </section>
            </>
          )}

          {/* Create / edit form ---------------------------------------- */}
          {(editing || isNew) && (
            <form className="admin-portal__section" onSubmit={handleSubmit}>
              {isNew && (
                <p className="admin-portal__muted">
                  New jobs are created as drafts. Publish them from this page
                  when they are ready to appear on the public site.
                </p>
              )}
              {refError && (
                <p className="admin-portal__error">
                  Employers or categories could not be loaded. Reload the page
                  to retry before saving.
                </p>
              )}
              <div className="admin-portal__facts">
                {isNew && (
                  <label className="admin-portal__filter-field">
                    <span>Employer *</span>
                    <select
                      className="admin-portal__filter-select"
                      value={form.employerId}
                      onChange={event => setField('employerId', event.target.value)}
                      required
                    >
                      <option value="">Select an employer…</option>
                      {employers.map(employer => (
                        <option key={employer.id} value={employer.id}>{employer.companyName}</option>
                      ))}
                    </select>
                  </label>
                )}
                <label className="admin-portal__filter-field">
                  <span>Title *</span>
                  <input
                    className="admin-portal__filter-input"
                    type="text"
                    maxLength={255}
                    value={form.title}
                    onChange={event => setField('title', event.target.value)}
                    required
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Category</span>
                  <select
                    className="admin-portal__filter-select"
                    value={form.categoryId}
                    onChange={event => setField('categoryId', event.target.value)}
                  >
                    <option value="">No category</option>
                    {categories.map(category => (
                      <option key={category.id} value={category.id}>{category.name}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Employment type *</span>
                  <select
                    className="admin-portal__filter-select"
                    value={form.employmentType}
                    onChange={event => setField('employmentType', event.target.value)}
                    required
                  >
                    <option value="">Select a type…</option>
                    {TYPE_OPTIONS.map(option => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Work mode *</span>
                  <select
                    className="admin-portal__filter-select"
                    value={form.workMode}
                    onChange={event => setField('workMode', event.target.value)}
                    required
                  >
                    <option value="">Select a mode…</option>
                    {MODE_OPTIONS.map(option => (
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
                    value={form.workLocation}
                    onChange={event => setField('workLocation', event.target.value)}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Salary min</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.salaryMin}
                    onChange={event => setField('salaryMin', event.target.value)}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Salary max</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    min="0"
                    step="0.01"
                    value={form.salaryMax}
                    onChange={event => setField('salaryMax', event.target.value)}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Currency</span>
                  <select
                    className="admin-portal__filter-select"
                    value={form.salaryCurrency}
                    onChange={event => setField('salaryCurrency', event.target.value)}
                  >
                    {currencyOptions.map(code => (
                      <option key={code} value={code}>{code}</option>
                    ))}
                  </select>
                </label>
                <label className="admin-portal__filter-field">
                  <span>Openings</span>
                  <input
                    className="admin-portal__filter-input"
                    type="number"
                    min="1"
                    step="1"
                    value={form.openings}
                    onChange={event => setField('openings', event.target.value)}
                  />
                </label>
                <label className="admin-portal__filter-field">
                  <span>Application deadline</span>
                  <input
                    className="admin-portal__filter-input"
                    type="date"
                    value={form.applicationDeadline}
                    onChange={event => setField('applicationDeadline', event.target.value)}
                  />
                </label>
                <label
                  className="admin-portal__filter-field"
                  style={{ gridColumn: '1 / -1' }}
                >
                  <span>Description *</span>
                  <textarea
                    className="admin-portal__filter-input"
                    rows={6}
                    value={form.description}
                    onChange={event => setField('description', event.target.value)}
                    required
                  />
                </label>
                <label
                  className="admin-portal__filter-field"
                  style={{ gridColumn: '1 / -1' }}
                >
                  <span>Requirements</span>
                  <textarea
                    className="admin-portal__filter-input"
                    rows={5}
                    value={form.requirements}
                    onChange={event => setField('requirements', event.target.value)}
                  />
                </label>
              </div>
              {formError && <p className="admin-portal__error">{formError}</p>}
              <div className="admin-portal__actions-row">
                <Button type="submit" variant="primary" size="medium" disabled={saving}>
                  {saving ? 'Saving…' : isNew ? 'Create draft job' : 'Save changes'}
                </Button>
                {!isNew && (
                  <Button type="button" variant="ghost" size="medium" disabled={saving} onClick={cancelEditing}>
                    Cancel
                  </Button>
                )}
              </div>
            </form>
          )}
        </div>
      )}
    </Container>
  )
}
