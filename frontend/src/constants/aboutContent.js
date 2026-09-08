import { company } from './siteIdentity.js'

export const aboutOverview = {
  hero: {
    title: 'About Us',
    subtitle:
      'A professional organization providing technical consultancy, skill development, livelihood and employment support across India.',
    badge: company.legalStatus,
    background: 'linear-gradient(135deg, #f6f8f4 0%, #eef2ea 100%)',
  },
  intro: company.shortDescription,
  objectivesItems: [
    'Technical Consultancy',
    'Project Advisory',
    'Capacity Building',
    'Documentation Support',
    'Compliance Guidance',
    'Skill Development Promotion',
    'Employment Generation',
    'Rural Livelihood Promotion',
    'Digital Transformation',
    'Organizational Development',
  ],
  cta: { label: 'View Company Profile', to: '/company-profile' },
  statHints: {
    title: 'Organizational Focus',
    items: [
      { label: 'Established', value: company.incorporationDate },
      { label: 'Legal Status', value: 'Company under the Companies Act, 2013' },
      { label: 'Registered Office', value: 'Benipur, Darbhanga, Bihar' },
    ],
  },
}

export const visionMission = {
  hero: {
    title: 'Vision & Mission',
    subtitle:
      'Guided by a clear purpose: to support quality skill development and livelihood outcomes through professional, transparent, and sustainable solutions.',
    background: 'linear-gradient(135deg, #0d3d21 0%, #14532d 100%)',
  },
  vision: {
    label: 'Our Vision',
    text: 'To become India\'s most trusted technical consultancy organization in the field of Skill Development, Livelihood Promotion, and Employment Generation by delivering innovative, transparent, and sustainable solutions.',
  },
  mission: {
    label: 'Our Mission',
    intro: 'LOKMIT FOUNDATION works to:',
    items: [
      'Strengthen Skill Development Institutions.',
      'Improve project quality and compliance.',
      'Provide expert technical mentorship.',
      'Build sustainable employment opportunities.',
      'Support organizations through knowledge-driven consultancy.',
    ],
  },
}

export const objectivesContent = {
  hero: {
    title: 'Our Objectives',
    subtitle:
      'Clear, practical objectives that guide how we support organizations.',
    background: 'linear-gradient(135deg, #f6f8f4 0%, #eef2ea 100%)',
  },
  items: [
    { title: 'Technical Consultancy' },
    { title: 'Project Advisory' },
    { title: 'Capacity Building' },
    { title: 'Documentation Support' },
    { title: 'Compliance Guidance' },
    { title: 'Skill Development Promotion' },
    { title: 'Employment Generation' },
    { title: 'Rural Livelihood Promotion' },
    { title: 'Digital Transformation' },
    { title: 'Organizational Development' },
  ],
  note: 'These objectives define the scope of our work and the areas where we provide structured support.',
}

export const valuesContent = {
  hero: {
    title: 'Our Values',
    subtitle: 'The principles that shape how we work and partner with organizations.',
    background: 'linear-gradient(135deg, #0d3d21 0%, #14532d 100%)',
  },
  items: [
    { name: 'Integrity', description: 'Honest, ethical, and responsible in every engagement.' },
    { name: 'Transparency', description: 'Clear communication, visible processes, and accountable delivery.' },
    { name: 'Accountability', description: 'Ownership of commitments, outcomes, and compliance.' },
    { name: 'Professionalism', description: 'Disciplined, respectful, and quality-focused conduct.' },
    { name: 'Innovation', description: 'Practical improvement in approach, process, and reach.' },
    { name: 'Quality', description: 'Reliable, well-documented, and purpose-built support.' },
    { name: 'Excellence', description: 'A consistent pursuit of better, not just complete.' },
    { name: 'Commitment', description: 'Long-term support beyond the first deliverable.' },
  ],
}

export const directorsMessage = {
  hero: {
    title: "Director's Message",
    subtitle: 'A note from the Director of LOKMIT FOUNDATION.',
    background: 'linear-gradient(135deg, #f6f8f4 0%, #eef2ea 100%)',
  },
  message:
    'At LOKMIT FOUNDATION, our objective is to empower organizations through technical knowledge, structured processes, and professional guidance. We believe that quality implementation begins with strong systems, continuous mentoring, and complete compliance. Our commitment is to help organizations achieve excellence in skill development and livelihood projects.',
  signature: {
    name: company.director.name,
    title: company.director.title,
  },
  legalFooter: {
    label: 'Legal Status',
    text: `${company.legalStatus}. CIN: ${company.cin}. ROC: ${company.roc}. Date of Incorporation: ${company.incorporationDate}.`,
  },
}

export const teamContent = {
  hero: {
    title: 'Leadership & Team',
    subtitle:
      'Learn more about the leadership behind LOKMIT FOUNDATION.',
    background: 'linear-gradient(135deg, #0d3d21 0%, #14532d 100%)',
  },
  note:
    'Team profiles are being prepared. Currently this section represents the Director of the organization.',
  members: [
    {
      name: company.director.name,
      role: company.director.title,
      bio: 'Sanjay Kumar Yadav is the Director of LOKMIT FOUNDATION, a company incorporated under the Companies Act, 2013 and registered with the Registrar of Companies, Patna.',
    },
  ],
  placeholderNote: 'Additional team profiles will be added as approved organizational information is provided.',
}
