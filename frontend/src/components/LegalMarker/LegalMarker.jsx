import { company } from '../../constants/siteIdentity.js'
import './LegalMarker.css'

export default function LegalMarker({ compact = false }) {
  return (
    <p className={`legal-marker${compact ? ' legal-marker--compact' : ''}`}>
      <span className="legal-marker__text">
        {company.name} is {company.legalStatus}.
      </span>
    </p>
  )
}
