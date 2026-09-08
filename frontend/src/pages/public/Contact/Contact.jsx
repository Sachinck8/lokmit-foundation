import { useState } from 'react'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { contactContent } from '../../../constants/contactContent.js'
import { company } from '../../../constants/siteIdentity.js'
import './Contact.css'

export default function Contact() {
  const hero = contactContent.hero
  const info = contactContent.info
  const enquiryCategories = contactContent.enquiryCategories
  const form = contactContent.form
  const note = contactContent.note

  const [formState, setFormState] = useState({
    name: '',
    email: '',
    phone: '',
    category: '',
    subject: '',
    message: '',
  })
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [touched, setTouched] = useState({})

  const isFieldInvalid = (name, value) => {
    if (!form.fields[name]?.required) return false
    if (!touched[name]) return false
    return !value || value.trim() === ''
  }

  const updateField = (name, value) => {
    setFormState(prev => ({ ...prev, [name]: value }))
    setTouched(prev => ({ ...prev, [name]: true }))
    setError(false)
  }

  const handleSubmit = (event) => {
    event.preventDefault()
    const required = ['name', 'email', 'category', 'subject', 'message']
    const invalid = required.filter(field => !formState[field]?.trim())
    if (invalid.length) {
      setTouched(Object.fromEntries(invalid.map(f => [f, true])))
      setError(true)
      return
    }
    setSubmitting(true)
    setError(false)
    setTimeout(() => {
      setSubmitting(false)
      setSubmitted(true)
    }, 600)
  }

  const isFormValid = () => {
    const required = ['name', 'email', 'category', 'subject', 'message']
    return required.every(field => formState[field]?.trim())
  }

  return (
    <div className="contact-page">
      <PageHero
        title={hero.title}
        subtitle={hero.subtitle}
        background={hero.background}
      />

      <Container>
        <div className="contact-page__layout">
          <div className="contact-page__info">
            <address className="contact-page__address">
              <span className="contact-page__org-name">{info.name}</span>
              <p className="contact-page__org-lines">{info.address}</p>
              <p className="contact-page__org-email">
                Email: <a href={`mailto:${info.email}`}>{info.email}</a>
              </p>
              <p className="contact-page__org-director">
                Director: {info.director}, {info.directorTitle}
              </p>
            </address>

            {note && (
              <p className="contact-page__note">{note}</p>
            )}

            <div className="contact-page__categories">
              <h2 className="contact-page__categories-title">Enquiry Categories</h2>
              <ul className="contact-page__categories-list">
                {enquiryCategories.map(cat => (
                  <li key={cat.value} className="contact-page__category">
                    <select
                      value={formState.category}
                      onChange={(e) => updateField('category', e.target.value)}
                      className={`contact-page__category-select${isFieldInvalid('category', formState.category) ? ' contact-page__category-select--invalid' : ''}`}
                      aria-label={cat.label}
                      disabled={submitted}
                    >
                      <option value="" disabled>{cat.label}</option>
                      {enquiryCategories.map(c => (
                        <option key={c.value} value={c.value}>{c.label}</option>
                      ))}
                    </select>
                  </li>
                ))}
              </ul>
            </div>

            <div className="contact-page__quick-links">
              <a href={`mailto:${info.email}`} className="contact-page__quick-link">
                <Icon name="mail" />
                Email Us
              </a>
              <a href={`/contact#form`} className="contact-page__quick-link">
                <Icon name="link" />
                Use Contact Form
              </a>
            </div>
          </div>

          <div className="contact-page__form" id="form">
            <h2 className="contact-page__form-title">Send an Enquiry</h2>
            {submitted ? (
              <div className="contact-page__success">
                <div className="contact-page__success-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                </div>
                <h3 className="contact-page__success-title">{form.headings.success}</h3>
                <p className="contact-page__success-text">{form.headings.placeholder}</p>
                <Button variant="primary" size="medium" onClick={() => setSubmitted(false)}>Send Another Enquiry</Button>
              </div>
            ) : (
              <form className="contact-page__form-inner" onSubmit={handleSubmit} noValidate>
                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-name">
                    {form.fields.name.label}
                    {form.fields.name.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <input
                    id="contact-name"
                    name="name"
                    type="text"
                    value={formState.name}
                    onChange={(e) => updateField('name', e.target.value)}
                    placeholder={form.fields.name.placeholder}
                    className={`contact-page__input${isFieldInvalid('name', formState.name) ? ' contact-page__input--invalid' : ''}`}
                    aria-invalid={isFieldInvalid('name', formState.name)}
                    aria-describedby={isFieldInvalid('name', formState.name) ? 'contact-name-error' : undefined}
                    required={form.fields.name.required}
                    disabled={submitting}
                  />
                  {isFieldInvalid('name', formState.name) && (
                    <p id="contact-name-error" className="contact-page__field-error">This field is required.</p>
                  )}
                </div>

                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-email">
                    {form.fields.email.label}
                    {form.fields.email.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <input
                    id="contact-email"
                    name="email"
                    type="email"
                    value={formState.email}
                    onChange={(e) => updateField('email', e.target.value)}
                    placeholder={form.fields.email.placeholder}
                    className={`contact-page__input${isFieldInvalid('email', formState.email) ? ' contact-page__input--invalid' : ''}`}
                    aria-invalid={isFieldInvalid('email', formState.email)}
                    aria-describedby={isFieldInvalid('email', formState.email) ? 'contact-email-error' : undefined}
                    required={form.fields.email.required}
                    disabled={submitting}
                  />
                  {isFieldInvalid('email', formState.email) && (
                    <p id="contact-email-error" className="contact-page__field-error">A valid email is required.</p>
                  )}
                </div>

                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-phone">
                    {form.fields.phone.label}
                    {form.fields.phone.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <input
                    id="contact-phone"
                    name="phone"
                    type="tel"
                    value={formState.phone}
                    onChange={(e) => updateField('phone', e.target.value)}
                    placeholder={form.fields.phone.placeholder}
                    className="contact-page__input"
                    disabled={submitting}
                  />
                </div>

                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-category">
                    {form.fields.category.label}
                    {form.fields.category.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <select
                    id="contact-category"
                    name="category"
                    value={formState.category}
                    onChange={(e) => updateField('category', e.target.value)}
                    className={`contact-page__select${isFieldInvalid('category', formState.category) ? ' contact-page__select--invalid' : ''}`}
                    aria-invalid={isFieldInvalid('category', formState.category)}
                    aria-describedby={isFieldInvalid('category', formState.category) ? 'contact-category-error' : undefined}
                    required={form.fields.category.required}
                    disabled={submitting}
                  >
                    <option value="" disabled>{form.fields.category.placeholder}</option>
                    {enquiryCategories.map(c => (
                      <option key={c.value} value={c.value}>{c.label}</option>
                    ))}
                  </select>
                  {isFieldInvalid('category', formState.category) && (
                    <p id="contact-category-error" className="contact-page__field-error">Please select a category.</p>
                  )}
                </div>

                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-subject">
                    {form.fields.subject.label}
                    {form.fields.subject.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <input
                    id="contact-subject"
                    name="subject"
                    type="text"
                    value={formState.subject}
                    onChange={(e) => updateField('subject', e.target.value)}
                    placeholder={form.fields.subject.placeholder}
                    className={`contact-page__input${isFieldInvalid('subject', formState.subject) ? ' contact-page__input--invalid' : ''}`}
                    aria-invalid={isFieldInvalid('subject', formState.subject)}
                    aria-describedby={isFieldInvalid('subject', formState.subject) ? 'contact-subject-error' : undefined}
                    required={form.fields.subject.required}
                    disabled={submitting}
                  />
                  {isFieldInvalid('subject', formState.subject) && (
                    <p id="contact-subject-error" className="contact-page__field-error">This field is required.</p>
                  )}
                </div>

                <div className="contact-page__field">
                  <label className="contact-page__label" htmlFor="contact-message">
                    {form.fields.message.label}
                    {form.fields.message.required && <span className="contact-page__required" aria-hidden="true">*</span>}
                  </label>
                  <textarea
                    id="contact-message"
                    name="message"
                    rows={6}
                    value={formState.message}
                    onChange={(e) => updateField('message', e.target.value)}
                    placeholder={form.fields.message.placeholder}
                    className={`contact-page__textarea${isFieldInvalid('message', formState.message) ? ' contact-page__textarea--invalid' : ''}`}
                    aria-invalid={isFieldInvalid('message', formState.message)}
                    aria-describedby={isFieldInvalid('message', formState.message) ? 'contact-message-error' : undefined}
                    required={form.fields.message.required}
                    disabled={submitting}
                  />
                  {isFieldInvalid('message', formState.message) && (
                    <p id="contact-message-error" className="contact-page__field-error">This field is required.</p>
                  )}
                </div>

                {error && !isFieldInvalid('name', formState.name) && (
                  <p className="contact-page__form-error">Something went wrong. Please try again.</p>
                )}

                <button
                  type="submit"
                  className="contact-page__submit"
                  disabled={submitting || !isFormValid()}
                >
                  {submitting ? form.headings.sending : form.submit}
                </button>

                <p className="contact-page__privacy-note">{form.privacyNote}</p>
              </form>
            )}
          </div>
        </div>
      </Container>
    </div>
  )
}
