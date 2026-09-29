import { Outlet } from 'react-router-dom'
import Navbar from '../../components/Navbar/Navbar.jsx'
import Footer from '../../components/Footer/Footer.jsx'
import './PublicLayout.css'

export default function PublicLayout() {
  return (
    <div className="public-layout">
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <Navbar />
      <main className="public-layout__main" id="main-content">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
