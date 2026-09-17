// Official LOKMIT FOUNDATION homepage content.
// Sources: verified company profile and organization brief.
// Do not invent clients, partners, statistics, or testimonials.

export const homeContent = {
  hero: {
    badge: 'Incorporated under the Companies Act, 2013',
    title: 'Technical Consultancy for Skill Development & Livelihood Programs',
    subtitle:
      'LOKMIT FOUNDATION helps institutions and implementing organizations deliver quality skill development, livelihood promotion, and employment-linked programs — with structured processes, complete documentation, and continuous mentoring.',
    primaryCta: { label: 'Explore Our Services', to: '/services' },
    secondaryCta: { label: 'Contact Us', to: '/contact' },
    identity: [
      { label: 'Who we are', text: 'A professional technical consultancy & skill development organization' },
      { label: 'What we do', text: 'Technical consultancy, project advisory, documentation & compliance support' },
      { label: 'Who we help', text: 'Institutions running skill development and livelihood programs across India' },
    ],
  },

  whatWeDo: {
    title: 'What We Do',
    subtitle:
      'End-to-end technical support for organizations implementing skill development, livelihood, CSR, and employment-linked programs.',
    items: [
      {
        title: 'Technical Consultancy',
        text: 'Expert guidance for planning, structuring, and delivering quality skill development and livelihood projects.',
        link: '/services',
      },
      {
        title: 'Project Advisory',
        text: 'Approval-stage support — PRN applications, DPR preparation, proposals, and quality appraisal.',
        link: '/services',
      },
      {
        title: 'Documentation & Compliance',
        text: 'SOPs, manuals, MIS design, compliance reviews, internal and mock audits, and audit-ready documentation.',
        link: '/services',
      },
      {
        title: 'Capacity Building',
        text: 'Technical training and mentorship that strengthen institutions and their teams.',
        link: '/services',
      },
    ],
  },

  services: {
    title: 'Core Services',
    subtitle: 'Three focused practice areas, delivered with professional rigor.',
    categories: [
      {
        title: 'Project Approval Consultancy',
        description: 'Support from application through approval-stage guidance.',
        items: ['PRN Application', 'DPR Preparation', 'Proposal Preparation', 'Quality Appraisal', 'PAC Support'],
        link: '/services',
      },
      {
        title: 'Technical Mentorship',
        description: 'Structured support across the full project execution lifecycle.',
        items: ['Kaushal Rural Portal', 'AEBAS', 'Batch Planning', 'Assessment & Certification', 'Placement & OJT', 'Claims, Audit & Closure'],
        link: '/services',
      },
      {
        title: 'Professional Consultancy',
        description: 'Organizational documentation, systems, policy, and training support.',
        items: ['SOP Development', 'HR Manual & Policy', 'MIS Design', 'Company Profile & Presentations', 'Compliance Review', 'Internal & Mock Audit'],
        link: '/services',
      },
    ],
    cta: { label: 'View All Services', to: '/services' },
  },

  expertise: {
    title: 'Expertise',
    subtitle: 'Supported program areas and sectors across India.',
    items: [
      'DDU-GKY 2.0',
      'PMKVY',
      'NSDC',
      'CSR Skill Development Projects',
      'State Skill Development Missions',
      'Rural Livelihood Projects',
      'Employment & Placement Support',
    ],
    note: 'Listing these program areas does not imply official government affiliation, authorization, partnership, or endorsement.',
    cta: { label: 'Explore Our Expertise', to: '/expertise' },
  },

  workingModel: {
    eyebrow: 'Our Working Model',
    tagline: {
      lines: ['We Guide.', 'We Train.', 'We Mentor.', 'You Execute.'],
      visualLabel: 'Our Commitment',
    },
    subtitle: 'Clear roles. Shared responsibility. Measurable outcomes.',
    steps: [
      { label: 'We Guide.', description: 'Understand your requirements and objectives.' },
      { label: 'We Train.', description: 'Build the capability and structure to deliver.' },
      { label: 'We Mentor.', description: 'Provide ongoing technical mentorship.' },
      { label: 'You Execute.', description: 'Execute with confidence and compliance.' },
    ],
    cta: { label: 'Understand How We Work', to: '/about' },
  },

  whyUs: {
    title: 'Why LOKMIT FOUNDATION',
    subtitle: 'A professional, transparent, implementation-focused technical partner.',
    items: [
      'Professional Technical Experts',
      'End-to-End Technical Guidance',
      'Documentation Support',
      'Government Compliance',
      'Process Standardization',
      'Practical Implementation Guidance',
      'Transparent Consultancy',
      'Experienced Team',
      'Timely Support',
      'Long-Term Technical Mentorship',
    ],
  },

  process: {
    eyebrow: 'How We Work',
    title: 'A Structured, Repeatable Process',
    subtitle: 'Quality and compliance are built in at every stage.',
    steps: [
      { label: 'Understand', description: 'Understand your objectives, context, and requirements.' },
      { label: 'Plan', description: 'Plan approach, scope, documentation, and compliance.' },
      { label: 'Design', description: 'Design systems, processes, and project structures.' },
      { label: 'Implement', description: 'Implement guidance, mentorship, and support.' },
      { label: 'Monitor', description: 'Monitor progress, quality, and compliance.' },
      { label: 'Audit', description: 'Audit readiness, documentation, and outcomes.' },
      { label: 'Improve', description: 'Improve processes for sustainable results.' },
    ],
  },

  values: {
    title: 'Our Values',
    subtitle: 'The principles that shape every engagement.',
    items: [
      'Integrity',
      'Transparency',
      'Accountability',
      'Professionalism',
      'Innovation',
      'Quality',
      'Excellence',
      'Commitment',
    ],
    cta: { label: 'Read About Our Values', to: '/about/values' },
  },

  knowledge: {
    title: 'Knowledge & Updates',
    subtitle:
      'Announcements, insights, and program documentation resources will be published here as they become available.',
    cards: [
      { title: 'News & Updates', text: 'Official announcements and organizational updates.', link: '/news', linkLabel: 'Visit News' },
      { title: 'Downloads & Resources', text: 'Approved documents, templates, and publications.', link: '/downloads', linkLabel: 'Visit Downloads' },
      { title: 'FAQ', text: 'Answers to common questions about our services and engagements.', link: '/faq', linkLabel: 'Read FAQ' },
    ],
  },

  cta: {
    eyebrow: 'Start a Conversation',
    title: 'Ready to Strengthen Your Program Delivery?',
    subtitle:
      'Whether you need technical consultancy, documentation support, compliance guidance, or skill development assistance, we are ready to help.',
    primaryCta: { label: 'Get in Touch', to: '/contact' },
    secondaryCta: { label: 'View Company Profile', to: '/company-profile' },
  },
}
