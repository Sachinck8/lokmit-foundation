// Official company identity and legal information for LOKMIT FOUNDATION.
// Authoritative source: client-provided company profile.
// Do not invent additional legal details.

export const company = {
  name: 'LOKMIT FOUNDATION',
  legalStatus: 'A Company incorporated under the Companies Act, 2013',
  cin: 'U88900BR2026NPL086900',
  roc: 'Registrar of Companies, Patna',
  incorporationDate: '29 July 2026',
  pan: 'AAHCL0128L',
  tan: 'PTNL02318B',
  registeredOffice: {
    lines: [
      'PT. No. 568, Ward No. 10,',
      'Opp. Baba Pokhar,',
      'D. Pauri,',
      'Benipur,',
      'Darbhanga,',
      'Bihar – 847201, India.',
    ],
  },
  officialEmail: 'lokmitfoundation@gmail.com',
  director: {
    name: 'Mr. Sanjay Kumar Yadav',
    title: 'Director, LOKMIT FOUNDATION',
  },
  shortDescription:
    'LOKMIT FOUNDATION is a professional organization established to provide high-quality technical consultancy, project advisory, documentation support, compliance assistance, capacity building, and skill development solutions across India.',
  tagline: 'Technical Consultancy. Skill Development. Livelihood & Employment Support.',
}

export const quickFacts = [
  { label: 'Legal Status', value: 'Company under the Companies Act, 2013' },
  { label: 'CIN', value: 'U88900BR2026NPL086900' },
  { label: 'ROC', value: 'Registrar of Companies, Patna' },
  { label: 'Date of Incorporation', value: '29 July 2026' },
  { label: 'PAN', value: 'AAHCL0128L' },
  { label: 'TAN', value: 'PTNL02318B' },
  { label: 'Registered Office', value: 'Benipur, Darbhanga, Bihar – 847201' },
  { label: 'Director', value: 'Mr. Sanjay Kumar Yadav' },
]

export const site = {
  name: 'LOKMIT FOUNDATION',
  tagline: company.tagline,
  description: company.shortDescription,
  aboutHubTitle: 'About Us',
  primaryNav: [
    { to: '/', label: 'Home' },
    { to: '/about', label: 'About' },
    { to: '/company-profile', label: 'Company Profile' },
    { to: '/legal-information', label: 'Legal Information' },
    { to: '/services', label: 'Services' },
    { to: '/expertise', label: 'Expertise' },
    { to: '/projects', label: 'Projects' },
    { to: '/news', label: 'News' },
    { to: '/gallery', label: 'Gallery' },
    { to: '/contact', label: 'Contact' },
  ],
  secondaryNav: [
    { to: '/clients', label: 'Clients & Partners' },
    { to: '/careers', label: 'Careers' },
    { to: '/downloads', label: 'Downloads' },
    { to: '/faq', label: 'FAQ' },
  ],
  loginNav: [
    { to: '/client-login', label: 'Client Login' },
    { to: '/employer-login', label: 'Employer Login' },
    { to: '/candidate-login', label: 'Candidate Login' },
    { to: '/admin-panel', label: 'Admin Panel' },
  ],
  footerNav: {
    pages: [
      { to: '/', label: 'Home' },
      { to: '/about', label: 'About Us' },
      { to: '/company-profile', label: 'Company Profile' },
      { to: '/legal-information', label: 'Legal Information' },
      { to: '/services', label: 'Services' },
      { to: '/expertise', label: 'Expertise' },
      { to: '/projects', label: 'Projects' },
      { to: '/case-studies', label: 'Case Studies' },
      { to: '/impact', label: 'Impact' },
      { to: '/clients', label: 'Clients & Partners' },
      { to: '/careers', label: 'Careers' },
      { to: '/jobs', label: 'Job Portal' },
      { to: '/downloads', label: 'Downloads' },
      { to: '/gallery', label: 'Gallery' },
      { to: '/news', label: 'News' },
      { to: '/events', label: 'Events' },
      { to: '/faq', label: 'FAQ' },
    ],
    legal: [
      { to: '/privacy-policy', label: 'Privacy Policy' },
      { to: '/terms-conditions', label: 'Terms & Conditions' },
      { to: '/refund-policy', label: 'Refund / Cancellation Policy' },
      { to: '/disclaimer', label: 'Disclaimer' },
      { to: '/cookie-policy', label: 'Cookie Policy' },
    ],
    contact: [
      { href: `mailto:${company.officialEmail}`, label: company.officialEmail },
      { to: '/contact', label: 'Contact Us' },
    ],
  },
}
