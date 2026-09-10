import { useState, useEffect } from 'react'

/**
 * Renders an official LOKMIT FOUNDATION brand asset.
 *
 * The official supplied assets live in `frontend/public/brand/` and are served
 * at `/brand/`. While an official asset file is not yet present on disk, the
 * component probes the official URL first and gracefully falls back to the
 * verified master logo (`/assets/lokmit-logo.png`) so no broken image is ever
 * rendered. Once the official file is deployed, it is picked up automatically
 * — no code change required.
 *
 * Props:
 *  - src:   official asset path (e.g. brand.primary, brand.white)
 *  - alt:   required alt text (never omit for meaningful brand marks)
 *  - className / width / height / loading: passed through to <img>
 */
export default function BrandImage({
  src,
  alt,
  className = '',
  width,
  height,
  loading,
}) {
  const fallback = '/assets/lokmit-logo.png'
  const [resolvedSrc, setResolvedSrc] = useState(src)

  useEffect(() => {
    setResolvedSrc(src)
    let cancelled = false
    const probe = new Image()
    probe.onload = () => {
      if (!cancelled) setResolvedSrc(src)
    }
    probe.onerror = () => {
      if (!cancelled) setResolvedSrc(fallback)
    }
    probe.src = src
    return () => {
      cancelled = true
    }
  }, [src])

  return (
    <img
      src={resolvedSrc}
      alt={alt}
      className={className}
      width={width}
      height={height}
      loading={loading}
    />
  )
}
