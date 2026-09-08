import Navbar from '../../components/Navbar/Navbar.jsx'
import Footer from '../../components/Footer/Footer.jsx'
import './PublicLayout.css'

export default function PublicLayout() {
  return (
    <div className="public-layout">
      <Navbar />
      <main className="public-layout__main">
        <div className="public-layout__content" />
      </main>
      <Footer />
    </div>
  )
}
