import { Link, Outlet } from 'react-router-dom'
import './PublicLayout.css'

// Base chrome for all public pages: header, routed main content, footer.
export default function PublicLayout() {
  return (
    <div className="public-layout">
      <header className="public-layout__header">
        <Link to="/" className="public-layout__brand">
          LOKMIT FOUNDATION
        </Link>
      </header>

      <main className="public-layout__main">
        <Outlet />
      </main>

      <footer className="public-layout__footer">
        &copy; LOKMIT FOUNDATION. All rights reserved.
      </footer>
    </div>
  )
}