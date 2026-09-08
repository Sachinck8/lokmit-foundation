import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import FooterLegal from '../../../components/FooterLegal/FooterLegal.jsx'
import { legalContent } from '../../../constants/legalContent.js'
import '../LegalPage/LegalPage.css'

const cookieItems = legalContent.cookiePolicy.sections.map(section => ({
  question: section.heading,
  answer: section.body,
}))

export default function CookiePolicy() {
  return (
    <div className="legal-page">
      <PageHero
        title={legalContent.cookiePolicy.title}
        subtitle={legalContent.cookiePolicy.intro}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <Accordion items={cookieItems} />
        <FooterLegal />
      </Container>
    </div>
  )
}
