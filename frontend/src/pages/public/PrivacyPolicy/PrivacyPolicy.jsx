import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import FooterLegal from '../../../components/FooterLegal/FooterLegal.jsx'
import { legalContent } from '../../../constants/legalContent.js'
import '../LegalPage/LegalPage.css'

const privacyItems = legalContent.privacyPolicy.sections.map(section => ({
  question: section.heading,
  answer: section.body,
}))

export default function PrivacyPolicy() {
  return (
    <div className="legal-page">
      <PageHero
        title={legalContent.privacyPolicy.title}
        subtitle={legalContent.privacyPolicy.intro}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <Accordion items={privacyItems} />
        <FooterLegal />
      </Container>
    </div>
  )
}
