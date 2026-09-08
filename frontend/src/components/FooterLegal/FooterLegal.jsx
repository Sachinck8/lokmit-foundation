import Button from '../Button/Button.jsx'
import { Link } from 'react-router-dom'
import { company } from '../../constants/siteIdentity.js'
import './FooterLegal.css'

export default function FooterLegal() {
  return (
    <div className="footer-legal">
      <p className="footer-legal__text">
        This website is operated by {company.name}, {company.legalStatus}.
      </p>
      <div className="footer-legal__actions">
        <Link to="/contact">
          <Button variant="outline" size="medium">Contact Us</Button>
        </Link>
      </div>
    </div>
  )
}
