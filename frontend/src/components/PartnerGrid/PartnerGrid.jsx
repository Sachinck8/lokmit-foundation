import './PartnerGrid.css'

export default function PartnerGrid({ count = 6, label = 'Partner' }) {
  return (
    <div className="partner-grid">
      {Array.from({ length: count }).map((_, index) => (
        <div key={index} className="partner-grid__placeholder" aria-label={`${label} placeholder`}>
          <span className="partner-grid__mark" aria-hidden="true">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect x="4" y="4" width="40" height="40" rx="8" fill="currentColor" opacity="0.12" />
              <rect x="12" y="12" width="24" height="24" rx="4" fill="currentColor" opacity="0.55" />
            </svg>
          </span>
          <span className="partner-grid__label">{label}</span>
        </div>
      ))}
    </div>
  )
}
