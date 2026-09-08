import { useState } from 'react'
import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { servicesContent } from '../../../constants/servicesContent.js'
import './Services.css'

function ServiceCategoryItem({ category, index, isActive, onClick }) {
  return (
    <li className={`services-page__category${isActive ? ' services-page__category--active' : ''}`}>
      <button
        type="button"
        className="services-page__category-trigger"
        aria-expanded={isActive}
        aria-controls={`services-category-${index}`}
        onClick={onClick}
      >
        <span className="services-page__category-title">{category.title}</span>
        <svg className="services-page__category-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>
      {isActive && (
        <div className="services-page__category-panel" id={`services-category-${index}`}>
          <p className="services-page__category-description">{category.description}</p>
          <ul className="services-page__category-items">
            {category.items.map((item, i) => (
              <li key={i} className="services-page__category-item">
                <svg className="services-page__category-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                {item}
              </li>
            ))}
          </ul>
        </div>
      )}
    </li>
  )
}

export default function Services() {
  const [activeCategory, setActiveCategory] = useState(0)
  const { categories, expertise } = servicesContent

  return (
    <div className="services-page">
      <PageHero
        title={servicesContent.hero.title}
        subtitle={servicesContent.hero.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />

      <Container>
        <section className="services-page__categories">
          <h2 className="services-page__categories-title">Our Service Categories</h2>
          <ul className="services-page__categories-list">
            {categories.map((category, index) => (
              <ServiceCategoryItem
                key={category.title}
                category={category}
                index={index}
                isActive={activeCategory === index}
                onClick={() => setActiveCategory(activeCategory === index ? -1 : index)}
              />
            ))}
          </ul>
        </section>

        <section className="services-page__expertise">
          <h2 className="services-page__expertise-title">{expertise.title}</h2>
          <p className="services-page__expertise-subtitle">{expertise.subtitle}</p>
          <BulletList items={expertise.items.map(e => e.name)} />
        </section>

        <section className="services-page__disclaimer">
          <p className="services-page__disclaimer-text">{expertise.note}</p>
        </section>

        <section className="services-page__cta">
          <div className="services-page__cta-inner" style={{ textAlign: 'center' }}>
            <h2 className="services-page__cta-title" style={{ color: 'var(--color-surface)' }}>Ready to Discuss Your Project?</h2>
            <p className="services-page__cta-subtitle" style={{ color: 'rgba(255,255,255,0.88)' }}>
              Whether you need technical consultancy, documentation support, compliance guidance, or skill development assistance, we are ready to help.
            </p>
            <div className="services-page__cta-actions" style={{ display: 'flex', gap: 'var(--spacing-4)', justifyContent: 'center', flexWrap: 'wrap' }}>
              <Link to="/contact">
                <Button variant="primary" size="large">Get in Touch</Button>
              </Link>
              <Link to="/expertise">
                <Button variant="outline" size="large">View Expertise</Button>
              </Link>
            </div>
          </div>
        </section>
      </Container>
    </div>
  )
}
