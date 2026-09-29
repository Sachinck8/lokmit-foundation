/**
 * Candidate portal content (A10). Labels and empty-state copy only —
 * no fake statistics, no invented company data; all displayed values come
 * from the real backend responses.
 */
export const candidateContent = {
  portal: {
    heroTitle: 'Candidate Portal',
    heroSubtitle: 'Your profile, resume, and applications in one place.',
  },

  applicationStatus: {
    SUBMITTED: 'Submitted',
    UNDER_REVIEW: 'Under review',
    SHORTLISTED: 'Shortlisted',
    HIRED: 'Hired',
    REJECTED: 'Not selected',
    WITHDRAWN: 'Withdrawn',
  },

  availability: {
    ACTIVELY_LOOKING: 'Actively looking',
    OPEN_TO_OFFERS: 'Open to offers',
    NOT_LOOKING: 'Not currently looking',
  },

  gender: {
    MALE: 'Male',
    FEMALE: 'Female',
    OTHER: 'Other',
  },

  empty: {
    applications: {
      title: 'No applications yet',
      text: 'When you apply to a published opening, it will appear here with its current status.',
      actionLabel: 'Browse jobs',
    },
    resumes: {
      title: 'No resume uploaded yet',
      text: 'Upload a PDF, DOC or DOCX resume (up to 5 MB) so your applications include your latest CV.',
      actionLabel: 'Upload your resume',
    },
    skills: {
      title: 'No skills added yet',
      text: 'Add skills from the catalogue so the hiring team can match you to suitable openings.',
    },
  },

  proficiency: {
    BEGINNER: 'Beginner',
    INTERMEDIATE: 'Intermediate',
    ADVANCED: 'Advanced',
    EXPERT: 'Expert',
  },
}
