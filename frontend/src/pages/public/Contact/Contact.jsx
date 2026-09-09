import { useState } from 'react'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import { contactContent } from '../../../constants/contactContent.js'
import { company } from '../../../constants/siteIdentity.js'
import { submitEnquiry as submitEnquiryToApi } from '../../../services/contactService.js'
import './Contact.css'

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const REQUIRED_FIELDS = ['name', 'email', 'category', 'subject', 'message']

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
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [submitted, setSubmitted] = useState(false)
  const [touched, setTouched] = useState({})

  const getFieldError = (name, value) => {
    if (!form.fields[name]?.required) return null
    if (!value || value.trim() === '') {
      if (name === 'email') return 'A valid email is required.'
      if (name === 'category') return 'Please select a category.'
      return 'This field is required.'
    }
    if (name === 'email' && !EMAIL_PATTERN.test(value.trim())) {
      return 'Please enter a valid email address.'
    }
    return null
  }

  const isFieldInvalid = (name, value) => Boolean(touched[name] && getFieldError(name, value))

  const updateField = (name, value) => {
    setFormState(prev => ({ ...prev, [name]: value }))
    setTouched(prev => ({ ...prev, [name]: true }))
  }

  const submitEnquiry = (event) => {
    event.preventDefault()
    if (submitting) {
      // A submission is already in progress — ignore duplicate submits.
      return
    }
    setTouched(prev => ({
      ...prev,
      ...Object.fromEntries(REQUIRED_FIELDS.map(field => [field, true])),
    }))

    const invalid = REQUIRED_FIELDS.filter(field => getFieldError(field, formState[field]))
    if (invalid.length) {
      setError('Please complete the highlighted fields before sending your enquiry.')
      return
    }

    setError('')
    setSubmitting(true)

    submitEnquiryToApi({
      name: formState.name.trim(),
      email: formState.email.trim(),
      phone: formState.phone.trim() || undefined,
      category: formState.category,
      subject: formState.subject.trim(),
      message: formState.message.trim(),
    })
      .then(() => {
        setSubmitted(true)
        setFormState({ name: '', email: '', phone: '', category: '', subject: '', message: '' })
        setTouched({})
      })
      .catch(() => {
        setError('We could not send your enquiry right now. Please try again, or email us directly.')
      })
      .finally(() => {
        setSubmitting(false)
      })
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
            {submitted && (
              <div className="contact-page__success" role="status">
                <h3 className="contact-page__success-title">Thank you — your enquiry has been received.</h3>
                <p className="contact-page__success-text">
                  Our team will review your message and respond to you at the email address you provided.
                </p>
              </div>
            )}
            <form className="contact-page__form-inner" onSubmit={submitEnquiry} noValidate>
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
                  />
                  {isFieldInvalid('name', formState.name) && (
                    <p id="contact-name-error" className="contact-page__field-error">{getFieldError('name', formState.name)}</p>
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
                  />
                  {isFieldInvalid('email', formState.email) && (
                    <p id="contact-email-error" className="contact-page__field-error">{getFieldError('email', formState.email)}</p>
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
                  >
                    <option value="" disabled>{form.fields.category.placeholder}</option>
                    {enquiryCategories.map(c => (
                      <option key={c.value} value={c.value}>{c.label}</option>
                    ))}
                  </select>
                  {isFieldInvalid('category', formState.category) && (
                    <p id="contact-category-error" className="contact-page__field-error">{getFieldError('category', formState.category)}</p>
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
                  />
                  {isFieldInvalid('subject', formState.subject) && (
                    <p id="contact-subject-error" className="contact-page__field-error">{getFieldError('subject', formState.subject)}</p>
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
                  />
                  {isFieldInvalid('message', formState.message) && (
                    <p id="contact-message-error" className="contact-page__field-error">{getFieldError('message', formState.message)}</p>
                  )}
                </div>

                {error && (
                  <p className="contact-page__form-error" role="alert">{error}</p>
                )}

                <button type="submit" className="contact-page__submit" disabled={submitting}>
                  {submitting ? 'Sending…' : form.submit}
                </button>

                <p className="contact-page__privacy-note">{form.privacyNote}</p>
            </form>
          </div>
        </div>
      </Container>
    </div>
  )
}
