export default function Badge({ label, className = '' }) {
  return (
    <span className={`badge${className ? ` ${className}` : ''}`}>
      {label}
    </span>
  )
}
