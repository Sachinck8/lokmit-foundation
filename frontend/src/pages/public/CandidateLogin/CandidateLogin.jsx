import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Button from '../../../components/Button/Button.jsx'
import { useAuth } from '../../../auth/AuthContext.jsx'
import './../PortalLogin/PortalLogin.css'

/**
 * Candidate login (A10) — the first real, working portal login.
 * Wired to the existing backend POST /api/v1/auth/login (JWT bearer,
 * refresh rotation, lockout behavior preserved server-side).
 */
export default function CandidateLogin() {
  const { login, logout, user, isCandidate } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

  const returnTo = searchParams.get('returnTo')
  const fallbackDestination = isCandidate ? '/candidate' : '/jobs'
  const destination = returnTo && returnTo.startsWith('/') ? returnTo : fallbackDestination

  // Already signed in: signpost instead of a redundant form.
  if (user && !submitting && !formError) {
    return (
      <div className="portal-login">
        <PageHero
          title="Candidate Login"
          subtitle="You are already signed in."
        />
        <Container>
          <div className="portal-login__status">
            <span className="portal-login__status-label">Signed in</span>
            <p className="portal-login__status-text">
              {isCandidate
                ? 'Your candidate session is active. Continue to the candidate portal.'
                : `You are signed in as ${user.userType || 'a staff account'}. The candidate portal is for candidate accounts.`}
            </p>
          </div>
          <div className="portal-login__actions">
            {isCandidate && (
              <Button variant="primary" size="large" onClick={() => navigate('/candidate')}>
                Go to candidate portal
              </Button>
            )}
            <Link to="/jobs">
              <Button variant="outline" size="large">Browse jobs</Button>
            </Link>
          </div>
          <div className="portal-login__actions" style={{ marginTop: 'var(--spacing-4)' }}>
            <Button
              variant="ghost"
              size="medium"
              onClick={async () => {
                await logout()
              }}
            >
              Sign out
            </Button>
          </div>
        </Container>
      </div>
    )
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError(null)

    if (!email.trim() || !password) {
      setFormError('Please enter your email address and password.')
      return
    }

    setSubmitting(true)
    try {
      const signedIn = await login(email.trim(), password)
      const candidateUser = signedIn
        && Array.isArray(signedIn.roles)
        && signedIn.roles.includes('ROLE_CANDIDATE')
      // A18: staff accounts (employment:manage) honoring an /admin-panel
      // returnTo land on the admin console; candidates go to their portal;
      // everything else falls back to public jobs.
      const staffPermissions = Array.isArray(signedIn.permissions)
        ? signedIn.permissions
        : []
      const adminTarget = destination.startsWith('/admin-panel')
        && staffPermissions.includes('employment:manage')
      const target = adminTarget
        ? destination
        : (destination === '/candidate' && !candidateUser
          ? '/jobs'
          : (candidateUser ? destination : '/jobs'))
      navigate(target, { replace: true })
      if (!candidateUser && destination.startsWith('/candidate')) {
        // Non-candidates are routed to public jobs; the portal guard will
        // explain the restriction if they navigate there directly.
      }
    } catch (error) {
      const status = error && error.response && error.response.status
      const apiErrors = error && error.response && error.response.data
        && error.response.data.errors
      const firstMessage = Array.isArray(apiErrors) && apiErrors.length > 0
        ? apiErrors[0].message
        : null

      if (status === 401) {
        setFormError(firstMessage || 'Invalid email or password.')
      } else if (status === 423 || (firstMessage && /lock/i.test(firstMessage))) {
        setFormError(firstMessage
          || 'Your account is temporarily locked after repeated failed attempts. Please try again later.')
      } else if (status === 429) {
        setFormError('Too many attempts. Please wait a moment and try again.')
      } else if (firstMessage) {
        setFormError(firstMessage)
      } else {
        setFormError('Something went wrong. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="portal-login">
      <PageHero
        title="Candidate Login"
        subtitle="Sign in to manage your profile, resume, and job applications."
      />
      <Container>
        <form className="portal-login__form" onSubmit={handleSubmit} noValidate>
          <div className="portal-login__field">
            <label className="portal-login__label" htmlFor="candidate-login-email">
              Email address
            </label>
            <input
              id="candidate-login-email"
              className="portal-login__input"
              type="email"
              name="email"
              autoComplete="email"
              value={email}
              onChange={event => setEmail(event.target.value)}
              disabled={submitting}
              required
            />
          </div>

          <div className="portal-login__field">
            <label className="portal-login__label" htmlFor="candidate-login-password">
              Password
            </label>
            <input
              id="candidate-login-password"
              className="portal-login__input"
              type="password"
              name="password"
              autoComplete="current-password"
              value={password}
              onChange={event => setPassword(event.target.value)}
              disabled={submitting}
              required
            />
          </div>

          {formError && (
            <p className="portal-login__error" role="alert">
              {formError}
            </p>
          )}

          <div className="portal-login__actions">
            <Button
              variant="primary"
              size="large"
              type="submit"
              disabled={submitting}
              ariaLabel={submitting ? 'Signing in' : 'Sign in'}
            >
              {submitting ? 'Signing in…' : 'Sign in'}
            </Button>
            <Link to="/jobs" className="portal-login__secondary-link">
              Browse jobs without an account
            </Link>
          </div>
        </form>

        <div className="portal-login__status">
          <span className="portal-login__status-label">No candidate account yet</span>
          <p className="portal-login__status-text">
            Candidate self-registration is not available yet — it is planned for a
            later phase. For now, accounts are provisioned by the foundation
            office. Questions? <Link to="/contact">Contact us</Link>.
          </p>
        </div>
      </Container>
    </div>
  )
}
