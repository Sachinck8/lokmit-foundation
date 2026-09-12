import { Link } from 'react-router-dom'
import Button from '../Button/Button.jsx'
import './EmptyState.css'

/**
 * Consistent empty / coming-soon state.
 * `to` (internal route) or `href` renders a real action link;
 * `onAction` still works for programmatic actions.
 */
export default function EmptyState({
  title,
  text,
  actionLabel,
  onAction,
  to,
  href,
  secondaryLabel,
  onSecondary,
  secondaryTo,
  secondaryHref,
  icon,
}) {
  const renderPrimary = () => {
    if (actionLabel && to) {
      return (
        <Link to={to} className="empty-state__action-link">
          <Button variant="primary" size="medium">{actionLabel}</Button>
        </Link>
      )
    }
    if (actionLabel && href) {
      return (
        <a href={href} className="empty-state__action-link">
          <Button variant="primary" size="medium">{actionLabel}</Button>
        </a>
      )
    }
    if (actionLabel && onAction) {
      return (
        <Button variant="primary" size="medium" onClick={onAction}>{actionLabel}</Button>
      )
    }
    return null
  }

  const renderSecondary = () => {
    if (secondaryLabel && secondaryTo) {
      return (
        <Link to={secondaryTo} className="empty-state__action-link">
          <Button variant="outline" size="medium">{secondaryLabel}</Button>
        </Link>
      )
    }
    if (secondaryLabel && secondaryHref) {
      return (
        <a href={secondaryHref} className="empty-state__action-link">
          <Button variant="outline" size="medium">{secondaryLabel}</Button>
        </a>
      )
    }
    if (secondaryLabel && onSecondary) {
      return (
        <Button variant="outline" size="medium" onClick={onSecondary}>{secondaryLabel}</Button>
      )
    }
    return null
  }

  const hasActions = Boolean(renderPrimary() || renderSecondary())

  return (
    <div className="empty-state">
      {icon && <div className="empty-state__icon" aria-hidden="true">{icon}</div>}
      <h2 className="empty-state__title">{title}</h2>
      {text && (
        <p className="empty-state__text">{text}</p>
      )}
      {hasActions && (
        <div className="empty-state__actions">
          {renderPrimary()}
          {renderSecondary()}
        </div>
      )}
    </div>
  )
}
