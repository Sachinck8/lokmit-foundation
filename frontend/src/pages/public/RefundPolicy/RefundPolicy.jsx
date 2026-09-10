import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import Accordion from '../../../components/Accordion/Accordion.jsx'
import FooterLegal from '../../../components/FooterLegal/FooterLegal.jsx'
import { legalContent } from '../../../constants/legalContent.js'
import '../LegalPage/LegalPage.css'

const refundItems = legalContent.refundPolicy.sections.map(section => ({
  question: section.heading,
  answer: section.body,
}))

export default function RefundPolicy() {
  return (
    <div className="legal-page">
      <PageHero
        title={legalContent.refundPolicy.title}
        subtitle={legalContent.refundPolicy.intro}
      />
      <Container>
        <Accordion items={refundItems} />
        <FooterLegal />
      </Container>
    </div>
  )
}
