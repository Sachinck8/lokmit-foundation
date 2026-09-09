import './Container.css'

export default function Container({ children, size = 'default' }) {
  return (
    <div className={`container container--${size}`}>
      {children}
    </div>
  )
}
