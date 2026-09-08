import { Link } from 'react-router-dom'
import Button from '../../../components/Button/Button.jsx'
import './NotFound.css'

export default function NotFound() {
  return (
    <div className="not-found">
      <h1 className="not-found__code">404</h1>
      <h2 className="not-found__title">Page Not Found</h2>
      <p className="not-found__text">The page you are looking for does not exist or has been moved.</p>
      <Link to="/">
        <Button variant="primary" size="large">Go Home</Button>
      </Link>
      <Link to="/contact" className="not-found__contact-link">
        Contact Us
      </Link>
    </div>
  )
}
