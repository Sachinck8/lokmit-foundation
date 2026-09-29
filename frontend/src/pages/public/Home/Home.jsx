import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import ProcessStepper from '../../../components/ProcessStepper/ProcessStepper.jsx'
import SectionHeader from '../../../components/SectionHeader/SectionHeader.jsx'
import CtaSection from '../../../components/CtaSection/CtaSection.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { homeContent } from '../../../constants/homeContent.js'
import './Home.css'

function CheckItem({ children }) {
  return (
    <li className="home__check-item">
      <svg className="home__check-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="20 6 9 17 4 12" />
      </svg>
      <span>{children}</span>
    </li>
  )
}

function ArrowLink({ to, children }) {
  return (
    <Link to={to} className="home__card-link">
      {children}
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M5 12h14M12 5l7 7-7 7" />
      </svg>
    </Link>
  )
}

export default function Home() {
  const {
    hero,
    whatWeDo,
    services,
    expertise,
    workingModel,
    whyUs,
    process,
    values,
    knowledge,
    cta,
  } = homeContent

  return (
    <div className="home">
      {/* 1 — Hero */}
      <PageHero
        title={hero.title}
        subtitle={hero.subtitle}
        badge={hero.badge}
        actions={
          <>
            <Link to={hero.primaryCta.to}>
              <Button variant="secondary" size="large">{hero.primaryCta.label}</Button>
            </Link>
            <Link to={hero.secondaryCta.to}>
              <Button variant="dark-outline" size="large">{hero.secondaryCta.label}</Button>
            </Link>
          </>
        }
      >
        <div className="home__hero-identity">
          {hero.identity.map(item => (
            <div key={item.label} className="home__hero-identity-item">
              <span className="home__hero-identity-label">{item.label}</span>
              <span className="home__hero-identity-text">{item.text}</span>
            </div>
          ))}
        </div>
      </PageHero>

      {/* 2 — What We Do */}
      <section className="section home__section">
        <Container>
          <SectionHeader badge="What We Do" title={whatWeDo.title} subtitle={whatWeDo.subtitle} align="centered" />
          <div className="home__grid home__grid--4">
            {whatWeDo.items.map(item => (
              <article key={item.title} className="home__card">
                <h3 className="home__card-title">{item.title}</h3>
                <p className="home__card-text">{item.text}</p>
                <ArrowLink to={item.link}>Learn more</ArrowLink>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* 3 — Core Services */}
      <section className="section section--alt home__section">
        <Container>
          <SectionHeader badge="Services" title={services.title} subtitle={services.subtitle} align="centered" />
          <div className="home__grid home__grid--3">
            {services.categories.map((category, index) => (
              <article key={category.title} className="home__service-card">
                <span className="home__service-num" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                <h3 className="home__service-title">{category.title}</h3>
                <p className="home__service-desc">{category.description}</p>
                <ul className="home__service-list">
                  {category.items.map(item => (
                    <CheckItem key={item}>{item}</CheckItem>
                  ))}
                </ul>
                <ArrowLink to={category.link}>Service details</ArrowLink>
              </article>
            ))}
          </div>
          <div className="home__center-cta">
            <Link to={services.cta.to}>
              <Button variant="primary" size="medium">{services.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      {/* 4 — Expertise */}
      <section className="section home__section">
        <Container>
          <SectionHeader badge="Expertise" title={expertise.title} subtitle={expertise.subtitle} align="centered" />
          <div className="home__expertise-tags">
            {expertise.items.map(item => (
              <span key={item} className="home__expertise-tag">{item}</span>
            ))}
          </div>
          <p className="home__note">{expertise.note}</p>
          <div className="home__center-cta">
            <Link to={expertise.cta.to}>
              <Button variant="primary" size="medium">{expertise.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      {/* 5 — Working Model (dark feature band) */}
      <section className="section section--dark home__section home__working-model">
        <Container>
          <ProcessStepper
            accent
            title={workingModel.tagline.lines.join(' ')}
            tagline={workingModel.subtitle}
            taglineLabel={workingModel.tagline.visualLabel}
            steps={workingModel.steps}
          />
          <div className="home__center-cta">
            <Link to={workingModel.cta.to}>
              <Button variant="dark-outline" size="medium">{workingModel.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      {/* 6 — Why LOKMIT FOUNDATION */}
      <section className="section home__section">
        <Container>
          <SectionHeader badge="Why Us" title={whyUs.title} subtitle={whyUs.subtitle} align="centered" />
          <ul className="home__why-grid">
            {whyUs.items.map(item => (
              <CheckItem key={item}>{item}</CheckItem>
            ))}
          </ul>
        </Container>
      </section>

      {/* 7 — How We Work */}
      <section className="section section--tinted home__section">
        <Container>
          <SectionHeader badge={process.eyebrow} title={process.title} subtitle={process.subtitle} align="centered" />
          <div className="home__grid home__grid--process">
            {process.steps.map((step, index) => (
              <article key={step.label} className="home__process-step">
                <span className="home__process-index" aria-hidden="true">{String(index + 1).padStart(2, '0')}</span>
                <h3 className="home__process-label">{step.label}</h3>
                <p className="home__process-desc">{step.description}</p>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* 8 — Values */}
      <section className="section home__section">
        <Container>
          <SectionHeader badge="Values" title={values.title} subtitle={values.subtitle} align="centered" />
          <div className="home__values-grid">
            {values.items.map(value => (
              <div key={value} className="home__value-chip">{value}</div>
            ))}
          </div>
          <div className="home__center-cta">
            <Link to={values.cta.to}>
              <Button variant="outline" size="medium">{values.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      {/* 9 — Knowledge / News */}
      <section className="section section--alt home__section">
        <Container>
          <SectionHeader badge="Knowledge" title={knowledge.title} subtitle={knowledge.subtitle} align="centered" />
          <div className="home__grid home__grid--3">
            {knowledge.cards.map(card => (
              <article key={card.title} className="home__card">
                <h3 className="home__card-title">{card.title}</h3>
                <p className="home__card-text">{card.text}</p>
                <ArrowLink to={card.link}>{card.linkLabel}</ArrowLink>
              </article>
            ))}
          </div>
        </Container>
      </section>

      {/* 10 — Business enquiry CTA */}
      <CtaSection
        eyebrow={cta.eyebrow}
        title={cta.title}
        subtitle={cta.subtitle}
        primaryCta={cta.primaryCta}
        secondaryCta={cta.secondaryCta}
      />
    </div>
  )
}
