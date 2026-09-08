export default function EmptyState({ title, text, actionLabel, onAction, secondaryLabel, onSecondary }) {
  return (
    <div className="empty-state">
      <h2 className="empty-state__title">{title}</h2>
      {text && (
        <p className="empty-state__text">{text}</p>
      )}
      {(actionLabel || secondaryLabel) && (
        <div className="empty-state__actions">
          {actionLabel && (
            <button type="button" className="empty-state__action empty-state__action--primary" onClick={onAction}>
              {actionLabel}
            </button>
          )}
          {secondaryLabel && onSecondary && (
            <button type="button" className="empty-state__action empty-state__action--secondary" onClick={onSecondary}>
              {secondaryLabel}
            </button>
          )}
        </div>
      )}
    </div>
  )
}
