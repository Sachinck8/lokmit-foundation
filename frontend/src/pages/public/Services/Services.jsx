import { useEffect, useState } from 'react'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { servicesContent } from '../../../constants/servicesContent.js'
import {
  listPublishedServices,
  listServiceCategories,
  listExpertiseAreas,
} from '../../../services/publicContentService.js'
import './Services.css'

const valueOutcomes = {
  'Project Approval Consultancy': 'Stronger, approval-ready applications with complete, quality documentation.',
  'Technical Mentorship': 'Confident day-to-day execution with expert guidance at every project stage.',
  'Professional Consultancy': 'Standardized systems and policies that keep your organization audit-ready.',
}

/**
 * A26: the services grid and expertise tags render live CMS data when the
 * public API has published content (GET /api/v1/service-categories +
 * /services + /expertise-areas, anonymous read-only).
 *
 * Fallback contract: on API failure, or when the CMS has no published
 * content yet, the page renders exactly the static servicesContent layout it
 * had before A26 — the public site must never show an error state for
 * content. The static "Outcome" callouts are keyed to specific static
 * titles, so they render only in the static fallback.
 */
export default function Services() {
  const [live, setLive] = useState(null) // { categories, itemsByCategory, areas } | null = fallback

  useEffect(() => {
    let cancelled = false
    Promise.all([
      listServiceCategories().catch(() => null),
      listPublishedServices({ page: 0, size: 100 }).catch(() => null),
      listExpertiseAreas().catch(() => null),
    ]).then(([categories, services, areas]) => {
      if (cancelled) return
      if (categories && categories.length > 0) {
        const itemsByCategory = {}
        for (const item of (services ? services.items : [])) {
          const categoryId = item.category && item.category.id
          if (categoryId != null) {
            if (!itemsByCategory[categoryId]) itemsByCategory[categoryId] = []
            itemsByCategory[categoryId].push(item)
          }
        }
        setLive({ categories, itemsByCategory, areas })
      } else {
        setLive(null)
      }
    })
    return () => {
      cancelled = true
    }
  }, [])

  const { categories, expertise } = servicesContent

  return (
    <div className="services-page">
      <PageHero
        title={servicesContent.hero.title}
        subtitle={servicesContent.hero.subtitle}
      />

      <section className="section sv__section">
        <Container>
          <SectionHeader
            badge="Service Architecture"
            title="Three Practice Areas. One Standard of Quality."
            subtitle="Each engagement combines clear deliverables, practical guidance, and compliance-ready documentation."
            align="centered"
          />

          {live ? (
            <div className="sv__grid">
              {live.categories.map((category, index) => {
                const items = live.itemsByCategory[category.id] || []
                return (
                  <article key={category.slug} className="sv__card">
                    <div className="sv__card-head">
                      <span className="sv__card-num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                      <h3 className="sv__card-title">{category.name}</h3>
                    </div>
                    {category.description && (
                      <p className="sv__card-desc">{category.description}</p>
                    )}
                    {items.length > 0 && (
                      <ul className="sv__card-items">
                        {items.map(item => (
                          <li key={item.slug} className="sv__card-item">
                            <svg className="sv__check" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                            {item.title}
                          </li>
                        ))}
                      </ul>
                    )}
                    {items.some(item => item.summary) && (
                      <div className="sv__card-outcome">
                        <span className="sv__outcome-label">Highlights</span>
                        <p className="sv__outcome-text">
                          {items.find(item => item.summary).summary}
                        </p>
                      </div>
                    )}
                  </article>
                )
              })}
            </div>
          ) : (
            <div className="sv__grid">
              {categories.map((category, index) => (
                <article key={category.title} className="sv__card">
                  <div className="sv__card-head">
                    <span className="sv__card-num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                    <h3 className="sv__card-title">{category.title}</h3>
                  </div>
                  <p className="sv__card-desc">{category.description}</p>
                  <ul className="sv__card-items">
                    {category.items.map(item => (
                      <li key={item} className="sv__card-item">
                        <svg className="sv__check" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                          <polyline points="20 6 9 17 4 12" />
                        </svg>
                        {item}
                      </li>
                    ))}
                  </ul>
                  <div className="sv__card-outcome">
                    <span className="sv__outcome-label">Outcome</span>
                    <p className="sv__outcome-text">{valueOutcomes[category.title]}</p>
                  </div>
                </article>
              ))}
            </div>
          )}
        </Container>
      </section>

      <section className="section section--tinted sv__expertise">
        <Container>
          <SectionHeader
            title={expertise.title}
            subtitle={expertise.subtitle}
            align="centered"
          />
          <ul className="sv__expertise-tags">
            {(live && live.areas && live.areas.length > 0
              ? live.areas.map(area => ({ name: area.name }))
              : expertise.items
            ).map(item => (
              <li key={item.name} className="sv__expertise-tag">{item.name}</li>
            ))}
          </ul>
          <p className="sv__disclaimer">{expertise.note}</p>
        </Container>
      </section>

      <section className="section section--dark sv__cta">
        <Container>
          <div className="sv__cta-inner">
            <h2 className="sv__cta-title">Ready to Discuss Your Project?</h2>
            <p className="sv__cta-subtitle">
              Whether you need technical consultancy, documentation support, compliance guidance, or skill development assistance, we are ready to help.
            </p>
            <div className="sv__cta-actions">
              <Link to="/contact">
                <Button variant="secondary" size="large">Get in Touch</Button>
              </Link>
              <Link to="/expertise">
                <Button variant="dark-outline" size="large">View Expertise</Button>
              </Link>
            </div>
          </div>
        </Container>
      </section>
    </div>
  )
}
