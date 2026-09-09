import './TeamCardGrid.css'

export default function TeamCardGrid({ children, cols = 'repeat(auto-fit, minmax(260px, 1fr))' }) {
  return (
    <div className="team-card-grid" style={{ gridTemplateColumns: cols }}>
      {children}
    </div>
  )
}
