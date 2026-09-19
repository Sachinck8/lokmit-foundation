import { useEffect, useState } from 'react'
import Container from '../../components/Container/Container.jsx'
import Button from '../../components/Button/Button.jsx'
import { useAuth } from '../../auth/AuthContext.jsx'
import { getMyProfile, updateMyProfile } from '../../services/candidateService.js'
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
