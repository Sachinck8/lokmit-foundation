import './Card.css'

export default function Card({ children, className = '', hover = false, ...props }) {
  return (
    <div className={`card${hover ? ' card--hover' : ''}${className ? ` ${className}` : ''}`} {...props}>
      {children}
    </div>
  )
}

export function CardImage({ src, alt, className = '' }) {
  if (!src) return null
  return (
    <div className={`card__image-wrapper${className ? ` ${className}` : ''}`}>
      <img
        src={src}
        alt={alt || ''}
        className="card__image"
        loading="lazy"
      />
    </div>
  )
}

export function CardContent({ children, className = '' }) {
  return (
    <div className={`card__content${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  )
}

export function CardHeader({ children, className = '' }) {
  return (
    <div className={`card__header${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  )
}

export function CardTitle({ children, className = '' }) {
  return (
    <h3 className={`card__title${className ? ` ${className}` : ''}`}>
      {children}
    </h3>
  )
}

export function CardText({ children, className = '' }) {
  return (
    <div className={`card__text${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  )
}

export function CardFooter({ children, className = '' }) {
  return (
    <div className={`card__footer${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  )
}
