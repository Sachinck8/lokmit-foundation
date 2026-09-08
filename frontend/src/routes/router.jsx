import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from '../layouts/PublicLayout/PublicLayout.jsx'
import Home from '../pages/public/Home/Home.jsx'
import About from '../pages/public/About/About.jsx'
import CompanyProfile from '../pages/public/CompanyProfile/CompanyProfile.jsx'
import LegalInformation from '../pages/public/LegalInformation/LegalInformation.jsx'
import Services from '../pages/public/Services/Services.jsx'
import Expertise from '../pages/public/Expertise/Expertise.jsx'
import Projects from '../pages/public/Projects/Projects.jsx'
import CaseStudies from '../pages/public/CaseStudies/CaseStudies.jsx'
import Impact from '../pages/public/Impact/Impact.jsx'
import Clients from '../pages/public/Clients/Clients.jsx'
import Careers from '../pages/public/Careers/Careers.jsx'
import Jobs from '../pages/public/Jobs/Jobs.jsx'
import Downloads from '../pages/public/Downloads/Downloads.jsx'
import Gallery from '../pages/public/Gallery/Gallery.jsx'
import News from '../pages/public/News/News.jsx'
import Events from '../pages/public/Events/Events.jsx'
import Contact from '../pages/public/Contact/Contact.jsx'
import FAQ from '../pages/public/FAQ/FAQ.jsx'
import PrivacyPolicy from '../pages/public/PrivacyPolicy/PrivacyPolicy.jsx'
import TermsConditions from '../pages/public/TermsConditions/TermsConditions.jsx'
import RefundPolicy from '../pages/public/RefundPolicy/RefundPolicy.jsx'
import Disclaimer from '../pages/public/Disclaimer/Disclaimer.jsx'
import CookiePolicy from '../pages/public/CookiePolicy/CookiePolicy.jsx'
import ClientLogin from '../pages/public/ClientLogin/ClientLogin.jsx'
import EmployerLogin from '../pages/public/EmployerLogin/EmployerLogin.jsx'
import CandidateLogin from '../pages/public/CandidateLogin/CandidateLogin.jsx'
import AdminPanel from '../pages/public/AdminPanel/AdminPanel.jsx'
import NotFound from '../pages/public/NotFound/NotFound.jsx'

const aboutRoutes = [
  { path: 'vision-mission', element: require('../pages/public/About/VisionMission/VisionMission.jsx').default },
  { path: 'objectives', element: require('../pages/public/About/Objectives/Objectives.jsx').default },
  { path: 'values', element: require('../pages/public/About/Values/Values.jsx').default },
  { path: 'directors-message', element: require('../pages/public/About/DirectorsMessage/DirectorsMessage.jsx').default },
  { path: 'team', element: require('../pages/public/About/Team/Team.jsx').default },
]

const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: '/about', element: <About />, children: aboutRoutes },
      { path: '/company-profile', element: <CompanyProfile /> },
      { path: '/legal-information', element: <LegalInformation /> },
      { path: '/services', element: <Services /> },
      { path: '/expertise', element: <Expertise /> },
      { path: '/projects', element: <Projects /> },
      { path: '/case-studies', element: <CaseStudies /> },
      { path: '/impact', element: <Impact /> },
      { path: '/clients', element: <Clients /> },
      { path: '/careers', element: <Careers /> },
      { path: '/jobs', element: <Jobs /> },
      { path: '/downloads', element: <Downloads /> },
      { path: '/gallery', element: <Gallery /> },
      { path: '/news', element: <News /> },
      { path: '/events', element: <Events /> },
      { path: '/contact', element: <Contact /> },
      { path: '/faq', element: <FAQ /> },
      { path: '/privacy-policy', element: <PrivacyPolicy /> },
      { path: '/terms-conditions', element: <TermsConditions /> },
      { path: '/refund-policy', element: <RefundPolicy /> },
      { path: '/disclaimer', element: <Disclaimer /> },
      { path: '/cookie-policy', element: <CookiePolicy /> },
      { path: '/client-login', element: <ClientLogin /> },
      { path: '/employer-login', element: <EmployerLogin /> },
      { path: '/candidate-login', element: <CandidateLogin /> },
      { path: '/admin-panel', element: <AdminPanel /> },
      { path: '*', element: <NotFound /> },
    ],
  },
])

export default router
