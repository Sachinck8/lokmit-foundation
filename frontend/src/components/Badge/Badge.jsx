export default function Badge({ label, variant = 'default', className = '' }) {
  return (
    <span className={`badge badge--${variant}${className ? ` ${className}` : ''}`}>
      {label}
    </span>
  )
}
