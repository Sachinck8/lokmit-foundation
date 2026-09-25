import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Icon from '../../../components/Icon/Icon.jsx'
import Badge from '../../../components/Badge/Badge.jsx'
import Button from '../../../components/Button/Button.jsx'
import { servicesContent } from '../../../constants/servicesContent.js'
import { getPublishedService } from '../../../services/publicContentService.js'
import './Services.css'

/**
 * A27: public service detail page (GET /api/v1/services/{slug}, anonymous,
 * published only). Follows the JobDetail pattern: skeleton while loading, a
 * masked not-available state on 404 (unknown slug and non-published service
 * are indistinguishable by design), a retry state on other errors, and a
 * back link to the listing. No fallback record is invented for unknown
 * slugs — the API's 404 semantics are preserved.
 */
export default function ServiceDetail() {
  const { slug } = useParams()
  const [service, setService] = useState(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [error, setError] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setNotFound(false)
    setError(false)
    getPublishedService(slug)
      .then(data => {
        if (active) setService(data)
      })
      .catch(err => {
        if (!active) return
        const status = err && err.response && err.response.status
        if (status === 404) {
          // Unknown slug AND non-published service are indistinguishable by design.
          setNotFound(true)
        } else {
          setError(true)
        }
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [slug, reloadKey])

  return (
    <div className="services-page">
      <PageHero
        title={service ? service.title : servicesContent.hero.title}
        subtitle={service ? (service.summary || '') : servicesContent.hero.subtitle}
      />

      <section className="section sv__section">
        <Container>
          <p className="jobs-detail__back">
            <Link to="/services" className="jobs-detail__back-link">
              <Icon name="link" /> Back to all services
            </Link>
          </p>

          {loading && (
            <div className="sv__card" aria-hidden="true">
              <div className="jobs__skeleton jobs__skeleton--title" />
              <div className="jobs__skeleton jobs__skeleton--line" />
              <div className="jobs__skeleton jobs__skeleton--line jobs__skeleton--short" />
            </div>
          )}

          {!loading && notFound && (
            <div className="sv__card">
              <h2 className="sv__card-title">This service is not available</h2>
              <p className="sv__card-desc">
                The service you are looking for does not exist or is not currently published.
              </p>
              <p style={{ marginTop: 'var(--spacing-4)' }}>
                <Link to="/services">
                  <Button variant="primary" size="medium">View all services</Button>
                </Link>
              </p>
            </div>
          )}

          {!loading && error && (
            <div className="sv__card">
              <h2 className="sv__card-title">Something went wrong</h2>
              <p className="sv__card-desc">
                This service could not be loaded right now. Please try again.
              </p>
              <p style={{ marginTop: 'var(--spacing-4)' }}>
                <Button variant="primary" size="medium" onClick={() => setReloadKey(key => key + 1)}>
                  Try again
                </Button>
              </p>
            </div>
          )}

          {!loading && service && (
            <article className="sv__card sv__detail">
              <div className="sv__card-head">
                <h2 className="sv__card-title">{service.title}</h2>
                {service.category && <Badge label={service.category.name} variant="default" />}
              </div>

              {service.summary && (
                <p className="sv__card-desc">{service.summary}</p>
              )}

              {service.description && (
                <p className="sv__detail__text">{service.description}</p>
              )}
            </article>
          )}
        </Container>
      </section>
    </div>
  )
}
