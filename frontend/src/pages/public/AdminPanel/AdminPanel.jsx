import PageHero from '../../../components/PageHero/PageHero.jsx'
import Container from '../../../components/Container/Container.jsx'
import BulletList from '../../../components/BulletList/BulletList.jsx'
import Button from '../../../components/Button/Button.jsx'
import { Link } from 'react-router-dom'
import { loginContent } from '../../../constants/loginContent.js'
import '../PortalLogin/PortalLogin.css'

export default function AdminPanel() {
  const content = loginContent.adminPanel
  return (
    <div className="portal-login">
      <PageHero
        title={content.title}
        subtitle={content.subtitle}
      />
      <Container>
        <div className="portal-login__status">
          <span className="portal-login__status-label">{content.status.label}</span>
          <p className="portal-login__status-text">{content.status.description}</p>
        </div>
        <BulletList
          title="Planned Admin CMS Modules"
          items={content.placeholderItems}
        />
        <div className="portal-login__actions">
          {/* A18: the applications review console is live for staff. */}
          <Link to="/admin-panel/applications">
            <Button variant="primary" size="large">Open Applications Review</Button>
          </Link>
          <Link to="/contact">
            <Button variant="ghost" size="large">Contact Us</Button>
          </Link>
        </div>
      </Container>
    </div>
  )
}
