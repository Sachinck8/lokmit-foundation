import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { loginContent } from '../../../constants/loginContent.js'
import '../PortalLogin/PortalLogin.css'

export default function EmployerLogin() {
  const content = loginContent.employerLogin
  return (
    <div className="portal-login">
      <PageHero
        title={content.title}
        subtitle={content.subtitle}
        background="linear-gradient(135deg, #0d3d21 0%, #14532d 100%)"
      />
      <Container>
        <div className="portal-login__status">
          <span className="portal-login__status-label">{content.status.label}</span>
          <p className="portal-login__status-text">{content.status.description}</p>
        </div>
        <BulletList
          title="Planned Employer Portal Features"
          items={content.placeholderItems}
        />
        <div className="portal-login__actions">
          <Link to="/contact">
            <Button variant="primary" size="large">Contact Us</Button>
          </Link>
        </div>
      </Container>
    </div>
  )
}
