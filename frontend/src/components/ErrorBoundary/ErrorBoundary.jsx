import { Component } from 'react'

/**
 * Application-level error boundary (A24 hardening).
 *
 * React unmounts the whole tree when a render/lifecycle error escapes all
 * route-level error handling; without a boundary that means a blank page.
 * This boundary is the last line of defense:
 *
 * - Catches unexpected render/runtime errors and shows a controlled,
 *   production-safe fallback (no stack traces, no internal messages).
 * - Offers safe recovery actions (reload, home) instead of dead ends.
 * - Logs the error through console.error only — never token/credential
 *   material, and no external monitoring service is introduced.
 *
 * In development (import.meta.env.DEV) the raw error is additionally
 * logged for diagnosis; production builds log only the message digest.
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error) {
    if (import.meta.env.DEV) {
      // Dev-only diagnostics; stripped from production behavior.
      console.error('[ErrorBoundary]', error)
    } else {
      console.error('[ErrorBoundary] An unexpected application error occurred.')
    }
  }

  handleReload = () => {
    window.location.reload()
  }

  handleHome = () => {
    this.setState({ hasError: false })
    window.location.assign('/')
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children
    }
    return (
      <div
        role="alert"
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          textAlign: 'center',
          padding: 'var(--spacing-10, 4rem) var(--spacing-6, 1.5rem)',
          fontFamily: 'inherit',
          background: 'var(--blue-50, #eef4f9)',
          color: 'var(--blue-900, #081a29)',
        }}
      >
        <h1
          style={{
            fontSize: 'clamp(1.5rem, 2.8vw, 1.9rem)',
            marginBottom: 'var(--spacing-3, 0.75rem)',
          }}
        >
          Something went wrong
        </h1>
        <p
          style={{
            maxWidth: '420px',
            marginBottom: 'var(--spacing-6, 2rem)',
            lineHeight: 1.6,
            color: 'var(--color-text-light, #4a5a68)',
          }}
        >
          An unexpected error occurred and the page could not be displayed.
          Please try again — your data is safe.
        </p>
        <div style={{ display: 'flex', gap: 'var(--spacing-3, 0.75rem)' }}>
          <button
            type="button"
            onClick={this.handleReload}
            style={{
              padding: '0.65rem 1.4rem',
              borderRadius: '999px',
              border: 'none',
              background: 'var(--blue-800, #0c2438)',
              color: '#ffffff',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Reload page
          </button>
          <button
            type="button"
            onClick={this.handleHome}
            style={{
              padding: '0.65rem 1.4rem',
              borderRadius: '999px',
              border: '1px solid var(--blue-800, #0c2438)',
              background: 'transparent',
              color: 'var(--blue-800, #0c2438)',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            Go home
          </button>
        </div>
      </div>
    )
  }
}

export default ErrorBoundary
