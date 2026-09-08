// Temporary mock data for the public website.
// This data can be replaced with real API data when backend endpoints are available.
// NOTE: Official company identity is maintained in siteIdentity.js.
// Do not duplicate or contradict official legal information here.
import { company } from './siteIdentity.js'

export const pageContent = {
  site: {
    name: company.name,
    tagline: company.tagline,
    description: company.shortDescription,
    legalStatus: company.legalStatus,
    cin: company.cin,
    roc: company.roc,
    incorporationDate: company.incorporationDate,
    pan: company.pan,
    tan: company.tan,
    registeredOffice: company.registeredOffice.lines.join(' '),
    officialEmail: company.officialEmail,
    director: company.director.name,
  },

  // Placeholder sections for future API-backed content.
  // These are intentionally minimal until approved organizational content is available.

  projects: {
    empty: {
      title: 'Projects',
      text: 'Project details are being prepared for publication.',
    },
  },

  caseStudies: {
    empty: {
      title: 'Case Studies',
      text: 'Approved case-study content is being prepared.',
    },
  },

  impact: {
    empty: {
      title: 'Impact',
      text: 'Verified organizational statistics will be presented here as approved figures are provided.',
    },
  },

  clients: {
    empty: {
      title: 'Clients & Partners',
      text: 'Client and partner representation will be added as approved logos and names are provided.',
    },
  },

  testimonials: {
    empty: {
      title: 'Testimonials',
      text: 'Testimonials will be added when approved statements are provided.',
    },
  },

  stats: {
    empty: {
      title: 'Organization Statistics',
      text: 'Verified statistics will be presented here as approved figures are provided.',
    },
  },

  partners: {
    empty: {
      title: 'Partners',
      text: 'Partner representation will be added when approved information is provided.',
    },
  },

  team: {
    empty: {
      title: 'Team',
      text: 'Team profiles are being prepared.',
    },
  },

  jobs: {
    empty: {
      title: 'Job Portal',
      text: 'The job portal is under development.',
    },
  },

  downloads: {
    empty: {
      title: 'Downloads & Resources',
      text: 'Approved downloadable resources will be listed here when available.',
    },
  },
}
