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
  // Candidate self-service notifications + interview visibility (A14).
  CANDIDATE_ME_NOTIFICATIONS: '/candidates/me/notifications',
  CANDIDATE_ME_INTERVIEWS: '/candidates/me/interviews',
  // Admin employment console (A18) — applications review (Stage 1).
  ADMIN_APPLICATIONS: '/admin/applications',
  // Admin dashboard (A21) — existing A2 read-only endpoints (dashboard:view).
  ADMIN_DASHBOARD_SUMMARY: '/admin/dashboard/summary',
  ADMIN_DASHBOARD_RECENT_ENQUIRIES: '/admin/dashboard/recent-enquiries',
  ADMIN_DASHBOARD_RECENT_USERS: '/admin/dashboard/recent-users',
  ADMIN_DASHBOARD_RECENT_APPLICATIONS: '/admin/dashboard/recent-applications',
  // Admin user management (A21) — existing A3 APIs (users:manage).
  ADMIN_USERS: '/admin/users',
  // Admin content platform (A22) — existing A4/A5/A6 APIs over the
  // content:manage, services:manage and projects:manage permissions.
  ADMIN_CMS_WEBSITE_CONTENT: '/admin/cms/website-content',
  ADMIN_SERVICE_CATEGORIES: '/admin/service-categories',
  ADMIN_SERVICES: '/admin/services',
  ADMIN_EXPERTISE_AREAS: '/admin/expertise-areas',
  ADMIN_PROJECT_CATEGORIES: '/admin/project-categories',
  ADMIN_PROJECTS: '/admin/projects',
  // Admin interview management (A20) — existing interview APIs under the
  // application resource (list/create/get/patch/delete; A7.4 backend).
  ADMIN_APPLICATION_INTERVIEWS: (applicationId) =>
    `/admin/applications/${applicationId}/interviews`,
  // Admin employment console (A19) — jobs management over the existing
  // admin job lifecycle APIs (no new backend surface).
  ADMIN_JOBS: '/admin/jobs',
  ADMIN_JOB_CATEGORIES: '/admin/job-categories',
  ADMIN_EMPLOYERS: '/admin/employers',
  ADMIN_SKILLS: '/admin/skills',
  ADMIN_CANDIDATE_RESUMES: (candidateId) => `/admin/candidates/${candidateId}/resumes`,
  ADMIN_CANDIDATE_SKILLS: (candidateId) => `/admin/candidates/${candidateId}/skills`,
  ADMIN_CANDIDATE_EDUCATIONS: (candidateId) => `/admin/candidates/${candidateId}/educations`,
  ADMIN_CANDIDATE_EXPERIENCES: (candidateId) => `/admin/candidates/${candidateId}/experiences`,
  RESUME_DOWNLOAD: (resumeId) => `/resumes/${resumeId}/download`,
}
