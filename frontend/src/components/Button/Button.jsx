import { forwardRef } from 'react'
import './Button.css'

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

  const buttonProps = {
    ref,
    className: classNames,
    type,
    disabled: disabled || false,
    onClick,
    'aria-label': ariaLabel,
    ...props,
  }

  if (href) {
    return (
      <a href={href} className={classNames} {...props}>
        {children}
      </a>
    )
  }

  return (
    <button {...buttonProps}>
      {children}
    </button>
  )
})

export default Button
