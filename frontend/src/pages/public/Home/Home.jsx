import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import ProcessStepper from '../../../components/ProcessStepper/ProcessStepper.jsx'
import StatBlock from '../../../components/StatBlock/StatBlock.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import CtaSection from '../../../components/CtaSection/CtaSection.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { homeContent } from '../../../constants/homeContent.js'
import { company } from '../../../constants/siteIdentity.js'
import './Home.css'

function HomeServiceCard({ item }) {
  return (
    <div className="home__service-card">
      <h3 className="home__service-card-title">{item.title}</h3>
      <p className="home__service-card-text">{item.description}</p>
      <Link to={item.link} className="home__service-card-link">
        Learn more
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M5 12h14M12 5l7 7-7 7" />
        </svg>
      </Link>
    </div>
  )
}

export default function Home() {
  const { hero, workingModel, whyChooseUs, ourProcess, servicesPreview, expertisePreview, cta } = homeContent

  return (
    <div className="home">
      <PageHero
        title={hero.title}
        subtitle={hero.subtitle}
        badge={hero.badge}
        background={hero.background}
        actions={
          <>
            <Link to={hero.primaryCta.to}>
              <Button variant="primary" size="large">{hero.primaryCta.label}</Button>
            </Link>
            <Link to={hero.secondaryCta.to}>
              <Button variant="outline" size="large" className="home__hero-secondary">{hero.secondaryCta.label}</Button>
            </Link>
          </>
        }
      />

      <section className="section home__working-model section--alt">
        <Container>
          <ProcessStepper
            title={workingModel.title}
            tagline={workingModel.subtitle}
            steps={workingModel.steps}
            accent
          />
          <p className="home__working-model-note">{workingModel.description}</p>
          <div className="home__working-model-cta">
            <Link to="/about">
              <Button variant="primary" size="medium">{workingModel.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      <section className="section home__services">
        <Container>
          <h2 className="home__section-title">{servicesPreview.title}</h2>
          <p className="home__section-subtitle">{servicesPreview.subtitle}</p>
          <div className="home__services-grid">
            {servicesPreview.items.map(item => (
              <HomeServiceCard key={item.title} item={item} />
            ))}
          </div>
          <div className="home__services-cta">
            <Link to="/services">
              <Button variant="primary" size="medium">View All Services</Button>
            </Link>
          </div>
        </Container>
      </section>

      <section className="section home__expertise section--alt">
        <Container>
          <h2 className="home__section-title">{expertisePreview.title}</h2>
          <p className="home__section-subtitle">{expertisePreview.subtitle}</p>
          <div className="home__expertise-tags">
            {expertisePreview.items.map(item => (
              <span key={item.title} className="home__expertise-tag">{item.title}</span>
            ))}
          </div>
          <div className="home__expertise-cta">
            <Link to="/expertise">
              <Button variant="outline" size="medium">{expertisePreview.cta.label}</Button>
            </Link>
          </div>
        </Container>
      </section>

      <section className="section home__why-choose">
        <Container>
          <h2 className="home__section-title">{whyChooseUs.title}</h2>
          <p className="home__section-subtitle">{whyChooseUs.subtitle}</p>
          <div className="home__why-grid">
            {whyChooseUs.items.map((item, index) => (
              <div key={index} className="home__why-item">
                <svg className="home__why-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                <span>{item.title}</span>
              </div>
            ))}
          </div>
        </Container>
      </section>

      <section className="section home__process section--alt">
        <Container>
          <ProcessStepper
            title={ourProcess.headingLabel}
            tagline={ourProcess.subtitle}
            steps={ourProcess.steps}
          />
        </Container>
      </section>

      <CtaSection
        background={cta.background || undefined}
        title={cta.title}
        subtitle={cta.subtitle}
        primaryCta={cta.primaryCta}
        secondaryCta={cta.secondaryCta}
      />

      <footer className="home__footer-legal">
        <Container>
          <p className="home__footer-legal-text">
            {company.legalStatus}. CIN: {company.cin}. ROC: {company.roc}. Date of Incorporation: {company.incorporationDate}.
          </p>
          <p className="home__footer-legal-email">
            Official Email: <a href={`mailto:${company.officialEmail}`}>{company.officialEmail}</a>
          </p>
        </Container>
      </footer>
    </div>
  )
}
