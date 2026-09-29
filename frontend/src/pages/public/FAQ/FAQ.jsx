import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import { faqContent } from '../../../constants/faqContent.js'
import './FAQ.css'

export default function FAQ() {
  return (
    <div className="faq-page">
      <PageHero
        title={faqContent.hero.title}
        subtitle={faqContent.hero.subtitle}
      />
      <Container>
        <div className="faq-page__categories">
          {faqContent.categories.map(category => (
            <section key={category.name} className="faq-page__category">
              <h2 className="faq-page__category-title">{category.name}</h2>
              <Accordion items={category.items} alwaysOpenFirst />
            </section>
          ))}
        </div>
      </Container>
    </div>
  )
}
