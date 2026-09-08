export default function Grid({ children, cols = 'repeat(auto-fit, minmax(260px, 1fr))', gap = 'var(--spacing-6)' }) {
  return (
    <div className="grid" style={{ gridTemplateColumns: cols, gap }}>
      {children}
    </div>
  )
}
