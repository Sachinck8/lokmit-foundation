import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import {
  getMyProfile,
  updateMyProfile,
  listMySkills,
  addMySkill,
  removeMySkill,
  listSkillCatalog,
} from '../../services/candidateService.js'
import { candidateContent } from '../../constants/candidateContent.js'

/**
 * A10 candidate profile page. Reads and updates ONLY the caller's own
 * profile through the server-side ownership-resolved self-service API;
 * no candidateId is ever sent. Display-only identity (name/email) comes
 * from the authenticated user, editable fields from the candidate record.
 */
export default function CandidateProfile() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState(null)
  const [saveError, setSaveError] = useState(null)
  const [form, setForm] = useState({
    phone: '',
    currentLocation: '',
    summary: '',
    expectedSalaryMin: '',
    expectedSalaryMax: '',
    availability: '',
  })

  // --- A11 skills state ---
  const [skills, setSkills] = useState(null)
  const [skillsError, setSkillsError] = useState(false)
  const [catalog, setCatalog] = useState(null)
  const [selectedSkillId, setSelectedSkillId] = useState('')
  const [selectedProficiency, setSelectedProficiency] = useState('')
  const [addingSkill, setAddingSkill] = useState(false)
  const [skillMessage, setSkillMessage] = useState(null)
  const [skillMessageError, setSkillMessageError] = useState(false)
  const [removingSkillId, setRemovingSkillId] = useState(null)

  useEffect(() => {
    let active = true
    getMyProfile()
      .then(data => {
        if (!active) return
        setProfile(data)
        setForm({
          phone: (data && data.phone) || '',
          currentLocation: (data && data.currentLocation) || '',
          summary: (data && data.summary) || '',
          expectedSalaryMin:
            data && data.expectedSalaryMin != null ? String(data.expectedSalaryMin) : '',
          expectedSalaryMax:
            data && data.expectedSalaryMax != null ? String(data.expectedSalaryMax) : '',
          availability: (data && data.availability) || '',
        })
      })
      .catch(() => {
        if (active) setLoadError(true)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [])

  // A11: load the caller's own skills + the ACTIVE catalog alongside the profile.
  useEffect(() => {
    let active = true
    listMySkills()
      .then(data => {
        if (active) setSkills(data)
      })
      .catch(() => {
        if (active) setSkillsError(true)
      })
    listSkillCatalog()
      .then(data => {
        if (active) setCatalog(data)
      })
      .catch(() => {
        // The picker simply stays empty; the skills list still renders.
      })
    return () => {
      active = false
    }
  }, [])

  async function handleAddSkill(event) {
    event.preventDefault()
    if (!selectedSkillId || addingSkill) return
    setAddingSkill(true)
    setSkillMessage(null)
    setSkillMessageError(false)
    try {
      const created = await addMySkill({
        skillId: Number(selectedSkillId),
        proficiency: selectedProficiency || null,
      })
      setSkills(previous => {
        const list = Array.isArray(previous) ? previous.filter(s => s.skillId !== created.skillId) : []
        return [...list, created].sort((a, b) =>
          String(a.skillName).localeCompare(String(b.skillName)))
      })
      setSelectedSkillId('')
      setSelectedProficiency('')
      setSkillMessage('Skill added to your profile.')
      setSkillMessageError(false)
    } catch (error) {
      const status = error && error.response && error.response.status
      const apiErrors = error && error.response && error.response.data
        && error.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null
      setSkillMessageError(true)
      if (status === 409) {
        setSkillMessage('That skill is already on your profile.')
      } else if (status === 401) {
        setSkillMessage('Your session has expired. Please log in again.')
      } else {
        setSkillMessage(firstMessage || 'We could not add that skill right now. Please try again.')
      }
    } finally {
      setAddingSkill(false)
    }
  }

  async function handleRemoveSkill(skillId) {
    if (removingSkillId) return
    setRemovingSkillId(skillId)
    setSkillMessage(null)
    setSkillMessageError(false)
    try {
      await removeMySkill(skillId)
      setSkills(previous => (Array.isArray(previous)
        ? previous.filter(s => s.skillId !== skillId) : []))
      setSkillMessage('Skill removed from your profile.')
      setSkillMessageError(false)
    } catch (error) {
      const status = error && error.response && error.response.status
      setSkillMessageError(true)
      if (status === 401) {
        setSkillMessage('Your session has expired. Please log in again.')
      } else {
        setSkillMessage('We could not remove that skill right now. Please try again.')
      }
    } finally {
      setRemovingSkillId(null)
    }
  }

  function updateField(field, value) {
    setForm(previous => ({ ...previous, [field]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setSaveMessage(null)
    setSaveError(null)

    const patch = {}
    if (form.phone !== (profile.phone || '')) patch.phone = form.phone
    if (form.currentLocation !== (profile.currentLocation || '')) {
      patch.currentLocation = form.currentLocation
    }
    if (form.summary !== (profile.summary || '')) patch.summary = form.summary
    if (form.expectedSalaryMin !== '') {
      patch.expectedSalaryMin = form.expectedSalaryMin
    }
    if (form.expectedSalaryMax !== '') {
      patch.expectedSalaryMax = form.expectedSalaryMax
    }
    if (form.availability && form.availability !== profile.availability) {
      patch.availability = form.availability
    }

    if (Object.keys(patch).length === 0) {
      setSaveMessage('No changes to save.')
      setSaving(false)
      return
    }

    try {
      const updated = await updateMyProfile(patch)
      setProfile(updated)
      setSaveMessage('Profile updated successfully.')
    } catch (error) {
      const apiErrors = error && error.response && error.response.data
        && error.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null
      setSaveError(firstMessage || 'We could not save your changes. Please try again.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <Container>
        <div className="candidate-portal__panel" aria-hidden="true">
          <div className="candidate-portal__skeleton candidate-portal__skeleton--title" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
          <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
        </div>
      </Container>
    )
  }

  if (loadError || !profile) {
    return (
      <Container>
        <div className="candidate-portal__panel">
          <p className="candidate-portal__error" role="alert">
            We could not load your profile right now. Please try again.
          </p>
          <Button variant="primary" size="medium" onClick={() => window.location.reload()}>
            Reload
          </Button>
        </div>
      </Container>
    )
  }

  return (
    <Container>
      <div className="candidate-portal__panel">
        <h1 className="candidate-portal__title">My profile</h1>
        <dl className="candidate-portal__facts">
          <div className="candidate-portal__fact">
            <dt>Account email</dt>
            <dd>{user && user.email ? user.email : '—'}</dd>
          </div>
          <div className="candidate-portal__fact">
            <dt>Availability</dt>
            <dd>
              {candidateContent.availability[profile.availability] || profile.availability || '—'}
            </dd>
          </div>
        </dl>
        <p className="candidate-portal__hint">
          Your account email and name are managed by the foundation office.
        </p>
      </div>

      <div className="candidate-portal__panel">
        <h2 className="candidate-portal__panel-title">Skills</h2>

        {skills === null && !skillsError && (
          <div aria-hidden="true">
            <div className="candidate-portal__skeleton candidate-portal__skeleton--line" />
            <div className="candidate-portal__skeleton candidate-portal__skeleton--short" />
          </div>
        )}

        {skillsError && (
          <p className="candidate-portal__error" role="alert">
            We could not load your skills right now. Please try again later.
          </p>
        )}

        {Array.isArray(skills) && skills.length === 0 && (
          <p className="candidate-portal__muted">
            {candidateContent.empty.skills.text}
          </p>
        )}

        {Array.isArray(skills) && skills.length > 0 && (
          <ul className="jobs-detail__skills" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
            {skills.map(skill => (
              <li key={skill.skillId} className="jobs-detail__skill">
                <span>{skill.skillName}</span>
                {skill.proficiency && (
                  <em>
                    {candidateContent.proficiency[skill.proficiency] || skill.proficiency}
                  </em>
                )}
                <button
                  type="button"
                  className="candidate-portal__skill-remove"
                  onClick={() => handleRemoveSkill(skill.skillId)}
                  disabled={removingSkillId !== null}
                  aria-label={`Remove ${skill.skillName} from your profile`}
                >
                  {removingSkillId === skill.skillId ? 'Removing…' : 'Remove'}
                </button>
              </li>
            ))}
          </ul>
        )}

        <form className="candidate-portal__form" onSubmit={handleAddSkill} noValidate>
          <div className="candidate-portal__form-row">
            <div>
              <label className="candidate-portal__label" htmlFor="skill-select">Add a skill</label>
              <select
                id="skill-select"
                className="candidate-portal__select"
                value={selectedSkillId}
                onChange={event => setSelectedSkillId(event.target.value)}
                disabled={addingSkill || !Array.isArray(catalog) || catalog.length === 0}
              >
                <option value="">
                  {catalog === null
                    ? 'Loading catalogue…'
                    : Array.isArray(catalog) && catalog.length === 0
                      ? 'No skills available'
                      : 'Select a skill…'}
                </option>
                {Array.isArray(catalog) && catalog
                  .filter(option => !Array.isArray(skills)
                    || !skills.some(owned => owned.skillId === option.id))
                  .map(option => (
                    <option key={option.id} value={option.id}>{option.name}</option>
                  ))}
              </select>
            </div>
            <div>
              <label className="candidate-portal__label" htmlFor="skill-proficiency">
                Proficiency (optional)
              </label>
              <select
                id="skill-proficiency"
                className="candidate-portal__select"
                value={selectedProficiency}
                onChange={event => setSelectedProficiency(event.target.value)}
                disabled={addingSkill}
              >
                <option value="">Not specified</option>
                <option value="BEGINNER">Beginner</option>
                <option value="INTERMEDIATE">Intermediate</option>
                <option value="ADVANCED">Advanced</option>
                <option value="EXPERT">Expert</option>
              </select>
            </div>
          </div>

          {skillMessage && (
            <p
              className={skillMessageError ? 'candidate-portal__error' : 'candidate-portal__success'}
              role={skillMessageError ? 'alert' : 'status'}
            >
              {skillMessage}
            </p>
          )}

          <div className="candidate-portal__actions-row">
            <Button
              variant="primary"
              size="medium"
              type="submit"
              disabled={addingSkill || !selectedSkillId}
            >
              {addingSkill ? 'Adding…' : 'Add skill'}
            </Button>
          </div>
        </form>
      </div>

      <div className="candidate-portal__panel">
        <h2 className="candidate-portal__panel-title">Editable details</h2>
        <form className="candidate-portal__form" onSubmit={handleSubmit} noValidate>
          <div className="candidate-portal__form-row">
            <div>
              <label className="candidate-portal__label" htmlFor="profile-phone">Phone</label>
              <input
                id="profile-phone"
                className="candidate-portal__input"
                type="tel"
                autoComplete="tel"
                value={form.phone}
                onChange={event => updateField('phone', event.target.value)}
                disabled={saving}
              />
            </div>
            <div>
              <label className="candidate-portal__label" htmlFor="profile-location">Current location</label>
              <input
                id="profile-location"
                className="candidate-portal__input"
                type="text"
                value={form.currentLocation}
                onChange={event => updateField('currentLocation', event.target.value)}
                disabled={saving}
              />
            </div>
          </div>

          <div>
            <label className="candidate-portal__label" htmlFor="profile-summary">Professional summary</label>
            <textarea
              id="profile-summary"
              className="candidate-portal__textarea"
              value={form.summary}
              onChange={event => updateField('summary', event.target.value)}
              disabled={saving}
              maxLength={20000}
            />
          </div>

          <div className="candidate-portal__form-row">
            <div>
              <label className="candidate-portal__label" htmlFor="profile-salary-min">
                Expected salary (minimum)
              </label>
              <input
                id="profile-salary-min"
                className="candidate-portal__input"
                type="number"
                min="0"
                step="0.01"
                value={form.expectedSalaryMin}
                onChange={event => updateField('expectedSalaryMin', event.target.value)}
                disabled={saving}
              />
            </div>
            <div>
              <label className="candidate-portal__label" htmlFor="profile-salary-max">
                Expected salary (maximum)
              </label>
              <input
                id="profile-salary-max"
                className="candidate-portal__input"
                type="number"
                min="0"
                step="0.01"
                value={form.expectedSalaryMax}
                onChange={event => updateField('expectedSalaryMax', event.target.value)}
                disabled={saving}
              />
            </div>
          </div>

          <div>
            <label className="candidate-portal__label" htmlFor="profile-availability">Availability</label>
            <select
              id="profile-availability"
              className="candidate-portal__select"
              value={form.availability}
              onChange={event => updateField('availability', event.target.value)}
              disabled={saving}
            >
              <option value="">Keep current value</option>
              <option value="ACTIVELY_LOOKING">Actively looking</option>
              <option value="OPEN_TO_OFFERS">Open to offers</option>
              <option value="NOT_LOOKING">Not currently looking</option>
            </select>
          </div>

          {saveMessage && (
            <p className="candidate-portal__success" role="status">{saveMessage}</p>
          )}
          {saveError && (
            <p className="candidate-portal__error" role="alert">{saveError}</p>
          )}

          <div className="candidate-portal__actions-row">
            <Button variant="primary" size="medium" type="submit" disabled={saving}>
              {saving ? 'Saving…' : 'Save changes'}
            </Button>
          </div>
        </form>
      </div>
    </Container>
  )
}
