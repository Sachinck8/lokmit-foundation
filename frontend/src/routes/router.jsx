import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from '../layouts/PublicLayout/PublicLayout.jsx'
import CandidateLayout from '../layouts/CandidateLayout/CandidateLayout.jsx'
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
import JobDetail from '../pages/public/Jobs/JobDetail.jsx'
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
import VisionMission from '../pages/public/About/VisionMission/VisionMission.jsx'
import Objectives from '../pages/public/About/Objectives/Objectives.jsx'
import Values from '../pages/public/About/Values/Values.jsx'
import DirectorsMessage from '../pages/public/About/DirectorsMessage/DirectorsMessage.jsx'
import Team from '../pages/public/About/Team/Team.jsx'
import CandidateDashboard from '../pages/candidate/CandidateDashboard.jsx'
import CandidateProfile from '../pages/candidate/CandidateProfile.jsx'
import CandidateResumes from '../pages/candidate/CandidateResumes.jsx'
import CandidateApplications from '../pages/candidate/CandidateApplications.jsx'
import CandidateApplicationDetail from '../pages/candidate/CandidateApplicationDetail.jsx'
import CandidateNotifications from '../pages/candidate/CandidateNotifications.jsx'
import CandidateInterviews from '../pages/candidate/CandidateInterviews.jsx'
import RequireCandidate from '../auth/RequireCandidate.jsx'
import RequireAdmin from '../auth/RequireAdmin.jsx'
import AdminLayout from '../layouts/AdminLayout/AdminLayout.jsx'
import AdminApplications from '../pages/admin/AdminApplications.jsx'
import AdminApplicationDetail from '../pages/admin/AdminApplicationDetail.jsx'
import AdminJobs from '../pages/admin/AdminJobs.jsx'
import AdminJobDetail from '../pages/admin/AdminJobDetail.jsx'

const candidateRoutes = [
  { path: '/candidate', element: <CandidateDashboard /> },
  { path: '/candidate/profile', element: <CandidateProfile /> },
  { path: '/candidate/resumes', element: <CandidateResumes /> },
  { path: '/candidate/applications', element: <CandidateApplications /> },
  { path: '/candidate/applications/:applicationId', element: <CandidateApplicationDetail /> },
  { path: '/candidate/interviews', element: <CandidateInterviews /> },
  { path: '/candidate/notifications', element: <CandidateNotifications /> },
]

const adminRoutes = [
  { path: '/admin-panel/applications', element: <AdminApplications /> },
  { path: '/admin-panel/applications/:applicationId', element: <AdminApplicationDetail /> },
  { path: '/admin-panel/jobs', element: <AdminJobs /> },
  { path: '/admin-panel/jobs/:jobId', element: <AdminJobDetail /> },
]

const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: '/about', element: <About /> },
      { path: '/about/vision-mission', element: <VisionMission /> },
      { path: '/about/objectives', element: <Objectives /> },
      { path: '/about/values', element: <Values /> },
      { path: '/about/directors-message', element: <DirectorsMessage /> },
      { path: '/about/team', element: <Team /> },
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
      { path: '/jobs/:jobId', element: <JobDetail /> },
      { path: '/downloads', element: <Downloads /> },
      { path: '/gallery', element: <Gallery /> },
      { path: '/news', element: <News /> },
      { path: '/events', element: <Events /> },
      { path: '/contact', element: <Contact /> },
      { path: '/faq', element: <FAQ /> },
      { path: '/privacy-policy', element: <PrivacyPolicy /> },
      { path: '/terms-and-conditions', element: <TermsConditions /> },
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
  {
    path: '/',
    element: (
      <RequireCandidate>
        <CandidateLayout />
      </RequireCandidate>
    ),
    children: candidateRoutes,
  },
  {
    path: '/',
    element: (
      <RequireAdmin>
        <AdminLayout />
      </RequireAdmin>
    ),
    children: adminRoutes,
  },
])

export default router
