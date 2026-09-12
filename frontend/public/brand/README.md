# Official LOKMIT FOUNDATION Brand Assets

This folder holds the **official supplied brand assets**. All display goes
through the single reusable component
`frontend/src/components/BrandLogo/BrandLogo.jsx`, which maps each context to
the correct asset. Logo paths are never duplicated across the app.

| File                  | Purpose                                        | Used by (`BrandLogo` variant) |
| --------------------- | ---------------------------------------------- | ----------------------------- |
| `logo-horizontal.png` | Horizontal lockup (mark + wordmark, one line)  | `desktop-navbar`              |
| `logo-compact.png`    | Compact square logo for small placements       | `mobile-navbar`               |
| `logo-white.png`      | Reverse/white logo for dark backgrounds        | `footer`                      |
| `logo-symbol.png`     | Circular symbol/mark only (supporting element) | `symbol` (small, subtle)      |
| `logo-primary.png`    | Full primary logo (square mark + wordmark)     | `primary` (light backgrounds) |
| `logo-black.png`      | Solid black logo for light print contexts      | Reserved                      |
| `REVERSE LOGO.png`    | Alternative reverse logo                       | Reserved                      |
| `favicon.png`         | Browser tab icon                               | `index.html` `<link rel="icon">` |
| `social-logo.png`     | Square social share avatar                     | `og:image` / `twitter:image`  |
| `brand-colors.png`    | Official palette reference                     | Reference only                |

## Asset facts (verified from the PNG data)

- `logo-horizontal`, `logo-white`, `logo-symbol`, `logo-black` have **true
  transparency** — they render directly on light or dark backgrounds with no
  backing plate.
- `logo-compact`, `logo-primary` are **opaque with a white background** — they
  belong on light surfaces only (never the dark footer).

## Rules

- Never redraw, recolor, crop, filter, or distort the artwork; preserve the
  original background exactly as supplied.
- Preserve original aspect ratio (height-driven sizing, `object-fit: contain`).
- The symbol is a small supporting element only — never a huge decorative
  circle and never a replacement for the main logo.
- Dark backgrounds use `logo-white` / `REVERSE LOGO`; light backgrounds use
  `logo-primary` / `logo-black`. Never mix the two contexts.
