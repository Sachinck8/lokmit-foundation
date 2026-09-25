import { forwardRef } from 'react'
import './Button.css'

/**
 * Design-system button.
 * variant: primary | secondary | outline | ghost | dark-outline
 * size:    small | medium | large
 * Renders an <a> when `href` is provided, otherwise a <button>.
 */
const Button = forwardRef(function Button({
  children,
  variant = 'primary',
  size = 'medium',
  onClick,
  type = 'button',
  href,
  disabled,
  ariaLabel,
  className = '',
  ...props
}, ref) {
  const classNames = [
    'btn',
    `btn--${variant}`,
    `btn--${size}`,
    className,
  ].filter(Boolean).join(' ')

  if (href) {
    return (
      <a href={href} className={classNames} aria-label={ariaLabel} onClick={onClick} {...props}>
        {children}
      </a>
    )
  }

  return (
    <button
      ref={ref}
      type={type}
      className={classNames}
      onClick={onClick}
      disabled={disabled || false}
      aria-label={ariaLabel}
      {...props}
    >
      {children}
    </button>
  )
})

export default Button
