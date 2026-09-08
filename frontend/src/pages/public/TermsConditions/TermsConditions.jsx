import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import FooterLegal from '../../../components/FooterLegal/FooterLegal.jsx'
import { legalContent } from '../../../constants/legalContent.js'
import '../LegalPage/LegalPage.css'

const termsItems = legalContent.termsConditions.sections.map(section => ({
  question: section.heading,
  answer: section.body,
}))

export default function TermsConditions() {
  return (
    <div className="legal-page">
      <PageHero
        title={legalContent.termsConditions.title}
        subtitle={legalContent.termsConditions.intro}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <Accordion items={termsItems} />
        <FooterLegal />
      </Container>
    </div>
  )
}
