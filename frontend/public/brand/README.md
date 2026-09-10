# Official LOKMIT FOUNDATION Brand Assets

This folder holds the **official supplied brand assets**. The frontend
references them at these exact public URLs (see
`frontend/src/constants/siteIdentity.js → brand`):

| File                | Purpose                                           | Used by            |
| ------------------- | ------------------------------------------------- | ------------------ |
| `logo-primary.png`  | Full primary logo (square mark + wordmark)        | Navbar             |
| `logo-horizontal.png` | Horizontal lockup (mark + wordmark in one line) | Reserved (navbar/brand strip) |
| `logo-symbol.png`   | Circular symbol/mark only                         | Reserved (compact brand areas) |
| `logo-compact.png`  | Compact logo for small placements                 | Reserved (mobile brand) |
| `logo-white.png`    | Reverse/white logo for dark backgrounds           | Footer (dark)      |
| `logo-black.png`    | Solid black logo for light print contexts         | Reserved           |
| `favicon.png`       | Browser tab icon                                  | `index.html`       |
| `social-logo.png`   | Square social share avatar                        | Reserved (og:image) |
| `brand-colors.png`  | Official palette reference                        | Reference only     |

## Rules

- Never redraw, recolor, or distort the artwork.
- Preserve original aspect ratio and transparency.
- `BrandImage` (`frontend/src/components/BrandImage/BrandImage.jsx`) probes
  these URLs and **falls back to `/assets/lokmit-logo.png`** while an official
  file is missing, so no broken image is ever rendered.
- Drop the official PNGs here with the exact names above and they are picked
  up automatically — no code change needed.
