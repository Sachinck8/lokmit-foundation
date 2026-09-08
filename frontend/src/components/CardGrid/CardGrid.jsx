import { useMemo } from 'react'
import './CardGrid.css'

export default function CardGrid({ children, cols = 3, gap = 'md', className = '' }) {
  const computedGap = useMemo(() => {
    const gapMap = {
      sm: 'gap-2',
      md: 'gap-4',
      lg: 'gap-6',
      xl: 'gap-8',
    }
    return gapMap[gap] || gapMap.md
  }, [gap])

  return (
    <div className={`card-grid ${computedGap}${className ? ` ${className}` : ''}`}>
      {children}
    </div>
  )
}
