// Central home for API endpoint paths - mirrors ApiPaths.java on the backend.
export const API_ENDPOINTS = {
  HEALTH: '/health',
  CONTACT_MESSAGES: '/contact-messages',
  // Public job browsing (A8) — mirrors ApiPaths.JOBS / ApiPaths.JOB.
  JOBS: '/jobs',
  // Authentication (A10).
  AUTH_LOGIN: '/auth/login',
  AUTH_REFRESH: '/auth/refresh',
  AUTH_LOGOUT: '/auth/logout',
  AUTH_ME: '/auth/me',
  // Candidate self-service (A10; resumes existed since A7.6.3).
  CANDIDATE_ME_PROFILE: '/candidates/me/profile',
  CANDIDATE_ME_RESUMES: '/candidates/me/resumes',
  CANDIDATE_ME_APPLICATIONS: '/candidates/me/applications',
  // Candidate self-service skills (A11) — existing V8 candidate_skills join.
  CANDIDATE_ME_SKILLS: '/candidates/me/skills',
  // Candidate self-service education + experience CRUD (A13) — V8 tables.
  CANDIDATE_ME_EDUCATIONS: '/candidates/me/educations',
  CANDIDATE_ME_EXPERIENCES: '/candidates/me/experiences',
}
