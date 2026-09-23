import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

/**
 * A18 Stage 1 — Admin applications review console service.
 *
 * Thin typed wrappers over the EXISTING backend admin APIs (no new HTTP
 * client, no new auth handling — the shared axios client keeps the bearer
 * token and the refresh-once-401 interceptor). Every endpoint used here is
 * already permission-protected server-side:
 *   /admin/applications family              = employment:manage
 *   /admin/candidates/id/resumes            = candidates:manage
 *   /admin/candidates/id/skills-educations-experiences = candidates:manage
 *   /resumes/id/download                    = ownership or candidates:manage
 */

function unwrapPage(data) {
  if (!data || !Array.isArray(data.items)) {
    throw new Error('Unexpected response shape from the admin API.')
  }
  return {
    items: data.items,
    page: typeof data.page === 'number' ? data.page : 0,
    size: typeof data.size === 'number' ? data.size : 20,
    totalItems: typeof data.totalItems === 'number' ? data.totalItems : data.items.length,
    totalPages: typeof data.totalPages === 'number' ? data.totalPages : 1,
  }
}

function unwrapOne(response) {
  const data = response.data && response.data.data
  if (!data) {
    throw new Error('Unexpected response shape from the admin API.')
  }
  return data
}

/**
 * Lists applications with the existing backend filters.
 * @param {{ page?: number, size?: number, jobId?: number|string, candidateId?: number|string,
 *            status?: string, search?: string }} params
 */
export function listApplications(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.jobId) query.jobId = params.jobId
  if (params.candidateId) query.candidateId = params.candidateId
  if (params.status) query.status = params.status
  if (params.search && String(params.search).trim() !== '') query.search = params.search.trim()
  return apiClient
    .get(API_ENDPOINTS.ADMIN_APPLICATIONS, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/**
 * Fetches one application (job/candidate/employer summaries + review data).
 * Unknown ids return the backend's 404.
 */
export function getApplication(applicationId) {
  return apiClient
    .get(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}`)
    .then(unwrapOne)
}

/** Records a review status transition. Backend rules are authoritative. */
export function startReview(applicationId) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/start-review`)
    .then(unwrapOne)
}

export function shortlist(applicationId) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/shortlist`)
    .then(unwrapOne)
}

/** decision: 'HIRED' | 'REJECTED'; note optional. */
export function decide(applicationId, decision, note) {
  const body = { decision }
  if (note && String(note).trim() !== '') body.note = String(note).trim()
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/decide`, body)
    .then(unwrapOne)
}

export function withdraw(applicationId) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/withdraw`)
    .then(unwrapOne)
}

/** Paginated status history for one application (newest first). */
export function getApplicationHistory(applicationId, { page = 0, size = 50 } = {}) {
  return apiClient
    .get(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/history`, {
      params: { page, size },
    })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Paginated interview list for one application. */
export function getApplicationInterviews(applicationId, { page = 0, size = 50 } = {}) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_APPLICATION_INTERVIEWS(applicationId), {
      params: { page, size },
    })
    .then(response => unwrapPage(response.data && response.data.data))
}

/**
 * Schedules a new interview for an application (A20 — reuses the existing
 * A7.4 interview API; no new backend surface). The backend requires a
 * non-terminal application (400 otherwise) and always starts the interview
 * as SCHEDULED — status is never client-writable. The candidate is
 * notified by the existing outbox chain; the UI only shows the resulting
 * state.
 * @param {number|string} applicationId
 * @param {{ scheduledAt: string, mode: 'ONSITE'|'REMOTE'|'PHONE',
 *           location?: string, notes?: string }} payload
 */
export function scheduleInterview(applicationId, payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_APPLICATION_INTERVIEWS(applicationId), payload)
    .then(unwrapOne)
}

/**
 * Partially updates an interview (reschedule, mode/location/notes changes,
 * or a conservative status transition — backend allows SCHEDULED →
 * COMPLETED | NO_SHOW | CANCELLED only; 409 otherwise). Omitted fields
 * stay unchanged. `undefined`/omitted optional fields are dropped so the
 * backend's explicit-null clears are always deliberate.
 */
export function updateInterview(applicationId, interviewId, payload) {
  const body = {}
  if (payload.scheduledAt !== undefined) body.scheduledAt = payload.scheduledAt
  if (payload.mode !== undefined) body.mode = payload.mode
  if (payload.location !== undefined) body.location = payload.location
  if (payload.notes !== undefined) body.notes = payload.notes
  if (payload.status !== undefined) body.status = payload.status
  return apiClient
    .patch(
      `${API_ENDPOINTS.ADMIN_APPLICATION_INTERVIEWS(applicationId)}/${interviewId}`,
      body,
    )
    .then(unwrapOne)
}

/**
 * Changes only an interview's status (same conservative backend matrix as
 * updateInterview — a convenience wrapper that sends just the status).
 */
export function setInterviewStatus(applicationId, interviewId, status) {
  return updateInterview(applicationId, interviewId, { status })
}

/**
 * Deletes an interview. The backend allows deleting CANCELLED interviews
 * only (409 otherwise) and never touches the owning application.
 */
export function deleteInterview(applicationId, interviewId) {
  return apiClient
    .delete(
      `${API_ENDPOINTS.ADMIN_APPLICATION_INTERVIEWS(applicationId)}/${interviewId}`,
    )
    .then(response => undefined)
}

/**
 * Lists a candidate's resumes (metadata only). The caller then downloads a
 * chosen resume through the existing secure download endpoint.
 */
export function getCandidateResumes(candidateId, { page = 0, size = 50 } = {}) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_CANDIDATE_RESUMES(candidateId), { params: { page, size } })
    .then(response => unwrapPage(response.data && response.data.data))
}

export function getCandidateSkills(candidateId) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_CANDIDATE_SKILLS(candidateId))
    .then(response => (response.data && response.data.data) || [])
}

export function getCandidateEducations(candidateId) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_CANDIDATE_EDUCATIONS(candidateId))
    .then(response => (response.data && response.data.data) || [])
}

export function getCandidateExperiences(candidateId) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_CANDIDATE_EXPERIENCES(candidateId))
    .then(response => (response.data && response.data.data) || [])
}

/**
 * Secure resume download through the existing authenticated endpoint
 * (backend resolves candidate ownership or candidates:manage). Returns the
 * axios response with a Blob body for the caller to trigger a download.
 */
export function downloadResume(resumeId) {
  return apiClient.get(API_ENDPOINTS.RESUME_DOWNLOAD(resumeId), {
    responseType: 'blob',
  })
}

/**
 * A19 — Admin jobs management over the EXISTING admin job lifecycle APIs
 * (all employment:manage server-side; no new backend surface).
 */

/**
 * Lists jobs with the existing backend filters.
 * @param {{ page?: number, size?: number, status?: string, employmentType?: string,
 *            workMode?: string, search?: string }} params
 */
export function listJobs(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.status) query.status = params.status
  if (params.employmentType) query.employmentType = params.employmentType
  if (params.workMode) query.workMode = params.workMode
  if (params.search && String(params.search).trim() !== '') query.search = params.search.trim()
  return apiClient
    .get(API_ENDPOINTS.ADMIN_JOBS, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Fetches one job (employer/category safe summaries included). Unknown ids → backend 404. */
export function getJob(jobId) {
  return apiClient.get(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}`).then(unwrapOne)
}

/** Creates a job. Backend always starts it DRAFT; slug/employer/category validated server-side. */
export function createJob(payload) {
  return apiClient.post(API_ENDPOINTS.ADMIN_JOBS, payload).then(unwrapOne)
}

/**
 * Partially updates a job. Only changed fields are sent so explicit-null
 * clears (requirements/location/salary/deadline/category) stay intentional.
 * Slug and employer are immutable server-side; status is lifecycle-only.
 */
export function updateJob(jobId, payload) {
  return apiClient.patch(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}`, payload).then(unwrapOne)
}

/** Lifecycle transitions — backend rules are authoritative (DRAFT→PUBLISHED→CLOSED→ARCHIVED). */
export function publishJob(jobId) {
  return apiClient.post(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/publish`).then(unwrapOne)
}

export function closeJob(jobId) {
  return apiClient.post(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/close`).then(unwrapOne)
}

export function archiveJob(jobId) {
  return apiClient.post(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/archive`).then(unwrapOne)
}

/** Permanent delete (DRAFT only server-side). Confirmation is the caller's responsibility. */
export function deleteJob(jobId) {
  return apiClient.delete(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}`)
}

/** Lists a job's skill requirements. Unknown job → backend 404. */
export function getJobSkills(jobId) {
  return apiClient
    .get(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/skills`)
    .then(response => (response.data && response.data.data) || [])
}

/** Assigns a skill requirement (duplicate → backend 409). */
export function assignJobSkill(jobId, skillId) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/skills`, { skillId })
    .then(unwrapOne)
}

/** Removes a skill requirement. Unknown pair → backend 404. */
export function removeJobSkill(jobId, skillId) {
  return apiClient.delete(`${API_ENDPOINTS.ADMIN_JOBS}/${jobId}/skills/${skillId}`)
}

// ------------------------------------------------------------------ A21
// Admin dashboard (A2 backend) — read-only aggregates and recent activity.
// Every value comes from the API; the UI never fabricates statistics.

/** High-level platform counts (users/jobs/applications/enquiries). */
export function getDashboardSummary() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_DASHBOARD_SUMMARY)
    .then(unwrapOne)
}

/** Newest contact enquiries (default 5, backend-capped at 10). */
export function getRecentEnquiries(limit = 5) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_DASHBOARD_RECENT_ENQUIRIES, { params: { limit } })
    .then(response => response.data && response.data.data)
}

/** Newest user accounts (safe fields only; default 5, backend-capped at 10). */
export function getRecentUsers(limit = 5) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_DASHBOARD_RECENT_USERS, { params: { limit } })
    .then(response => response.data && response.data.data)
}

/** Newest job applications with candidate/job references (default 5, backend-capped at 10). */
export function getRecentApplications(limit = 5) {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_DASHBOARD_RECENT_APPLICATIONS, { params: { limit } })
    .then(response => response.data && response.data.data)
}

// ------------------------------------------------------------------ A21
// Admin users (A3 backend) — safe administrative views only. The DTO
// deliberately excludes password hashes, refresh tokens and I-2
// brute-force bookkeeping; those never reach this layer.

/**
 * Lists user accounts with the existing backend filters.
 * @param {{ page?: number, size?: number, search?: string, status?: string, role?: string }} params
 */
export function listUsers(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.search && String(params.search).trim() !== '') query.search = params.search.trim()
  if (params.status) query.status = params.status
  if (params.role) query.role = params.role
  return apiClient
    .get(API_ENDPOINTS.ADMIN_USERS, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Safe detail view of one user. Unknown ids return the backend's 404. */
export function getUser(userId) {
  return apiClient
    .get(`${API_ENDPOINTS.ADMIN_USERS}/${userId}`)
    .then(unwrapOne)
}

/**
 * Changes a user's account status (PATCH /admin/users/{id}/status).
 * Values: ACTIVE | LOCKED | SUSPENDED | DELETED. The backend rejects
 * self-deactivation and disabling the last active SUPER_ADMIN (400).
 */
export function updateUserStatus(userId, status) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_USERS}/${userId}/status`, { status })
    .then(unwrapOne)
}

/**
 * Replaces a user's role assignments (PUT /admin/users/{id}/roles).
 * Full replacement — the resulting roles are exactly the supplied set
 * (an empty set removes all roles, subject to backend protections).
 */
export function updateUserRoles(userId, roles) {
  return apiClient
    .put(`${API_ENDPOINTS.ADMIN_USERS}/${userId}/roles`, { roles })
    .then(unwrapOne)
}

// ------------------------------------------------------------------ A22
// Admin content platform (A4/A5/A6 backends) — website content, services,
// expertise areas and projects. Thin wrappers over the existing endpoints
// only; field names mirror the backend DTOs exactly.

const optional = value => {
  const trimmed = typeof value === 'string' ? value.trim() : value
  return trimmed === '' || trimmed == null ? undefined : trimmed
}

const toIdOrNull = value => {
  const trimmed = typeof value === 'string' ? value.trim() : value
  return trimmed === '' || trimmed == null ? null : Number(trimmed)
}

/**
 * Lists website content sections (content:manage).
 * @param {{ page?: number, size?: number, pageKey?: string, sectionKey?: string, status?: string }} params
 */
export function listWebsiteContent(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  const pageKey = optional(params.pageKey)
  if (pageKey) query.pageKey = pageKey
  const sectionKey = optional(params.sectionKey)
  if (sectionKey) query.sectionKey = sectionKey
  if (params.status) query.status = params.status
  return apiClient
    .get(API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Creates a content section (always starts DRAFT; duplicate pair → 409). */
export function createWebsiteContent(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT, {
      pageKey: payload.pageKey,
      sectionKey: payload.sectionKey,
      title: optional(payload.title),
      contentJson: optional(payload.contentJson),
    })
    .then(unwrapOne)
}

/**
 * Partially updates a content section. Empty strings are sent as explicit
 * null (the backend clears the field); the identity keys are immutable.
 */
export function updateWebsiteContent(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT}/${id}`, {
      title: optional(payload.title) ?? null,
      contentJson: optional(payload.contentJson) ?? null,
    })
    .then(unwrapOne)
}

export function publishWebsiteContent(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT}/${id}/publish`)
    .then(unwrapOne)
}

export function archiveWebsiteContent(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT}/${id}/archive`)
    .then(unwrapOne)
}

/** Deletes a content section (content:publish; permanent). */
export function deleteWebsiteContent(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_CMS_WEBSITE_CONTENT}/${id}`)
    .then(response => undefined)
}

/** Lists service categories (services:manage). */
export function listServiceCategories() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_SERVICE_CATEGORIES, { params: { size: 100 } })
    .then(response => unwrapPage(response.data && response.data.data).items)
}

export function createServiceCategory(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_SERVICE_CATEGORIES, {
      name: optional(payload.name),
      slug: optional(payload.slug),
      description: optional(payload.description),
      displayOrder: toIdOrNull(payload.displayOrder) ?? undefined,
    })
    .then(unwrapOne)
}

export function updateServiceCategory(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_SERVICE_CATEGORIES}/${id}`, {
      name: optional(payload.name) ?? null,
      description: optional(payload.description) ?? null,
      displayOrder: toIdOrNull(payload.displayOrder) ?? null,
    })
    .then(unwrapOne)
}

/** Deletes a category; referencing services are detached, never deleted. */
export function deleteServiceCategory(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_SERVICE_CATEGORIES}/${id}`)
    .then(response => undefined)
}

/**
 * Lists services (services:manage).
 * @param {{ page?: number, size?: number, categoryId?: number|string, status?: string, search?: string }} params
 */
export function listServices(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  const categoryId = toIdOrNull(params.categoryId)
  if (categoryId) query.categoryId = categoryId
  if (params.status) query.status = params.status
  const search = optional(params.search)
  if (search) query.search = search
  return apiClient
    .get(API_ENDPOINTS.ADMIN_SERVICES, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Creates a service (always starts DRAFT; duplicate slug → 409, unknown category → 404). */
export function createService(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_SERVICES, {
      slug: optional(payload.slug),
      title: optional(payload.title),
      summary: optional(payload.summary),
      description: optional(payload.description),
      categoryId: toIdOrNull(payload.categoryId) ?? undefined,
      displayOrder: toIdOrNull(payload.displayOrder) ?? undefined,
    })
    .then(unwrapOne)
}

/**
 * Partially updates a service. Slug is immutable; an explicitly empty
 * category detaches the service (backend null); status uses lifecycle
 * endpoints.
 */
export function updateService(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_SERVICES}/${id}`, {
      title: optional(payload.title) ?? null,
      summary: optional(payload.summary) ?? null,
      description: optional(payload.description) ?? null,
      categoryId: toIdOrNull(payload.categoryId) ?? null,
      displayOrder: toIdOrNull(payload.displayOrder) ?? null,
    })
    .then(unwrapOne)
}

export function publishService(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_SERVICES}/${id}/publish`)
    .then(unwrapOne)
}

export function archiveService(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_SERVICES}/${id}/archive`)
    .then(unwrapOne)
}

export function deleteService(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_SERVICES}/${id}`)
    .then(response => undefined)
}

/**
 * Lists expertise areas (services:manage).
 * @param {{ page?: number, size?: number, status?: string, search?: string }} params
 */
export function listExpertiseAreas(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.status) query.status = params.status
  const search = optional(params.search)
  if (search) query.search = search
  return apiClient
    .get(API_ENDPOINTS.ADMIN_EXPERTISE_AREAS, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Creates an expertise area (always starts DRAFT; duplicate slug → 409). */
export function createExpertiseArea(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_EXPERTISE_AREAS, {
      slug: optional(payload.slug),
      name: optional(payload.name),
      description: optional(payload.description),
      displayOrder: toIdOrNull(payload.displayOrder) ?? undefined,
    })
    .then(unwrapOne)
}

/** Partially updates an expertise area; slug is immutable. */
export function updateExpertiseArea(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_EXPERTISE_AREAS}/${id}`, {
      name: optional(payload.name) ?? null,
      description: optional(payload.description) ?? null,
      displayOrder: toIdOrNull(payload.displayOrder) ?? null,
    })
    .then(unwrapOne)
}

export function publishExpertiseArea(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_EXPERTISE_AREAS}/${id}/publish`)
    .then(unwrapOne)
}

export function archiveExpertiseArea(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_EXPERTISE_AREAS}/${id}/archive`)
    .then(unwrapOne)
}

export function deleteExpertiseArea(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_EXPERTISE_AREAS}/${id}`)
    .then(response => undefined)
}

/** Lists project categories (projects:manage). */
export function listProjectCategories() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_PROJECT_CATEGORIES, { params: { size: 100 } })
    .then(response => unwrapPage(response.data && response.data.data).items)
}

export function createProjectCategory(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_PROJECT_CATEGORIES, {
      name: optional(payload.name),
      slug: optional(payload.slug),
      description: optional(payload.description),
      displayOrder: toIdOrNull(payload.displayOrder) ?? undefined,
    })
    .then(unwrapOne)
}

export function updateProjectCategory(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_PROJECT_CATEGORIES}/${id}`, {
      name: optional(payload.name) ?? null,
      description: optional(payload.description) ?? null,
      displayOrder: toIdOrNull(payload.displayOrder) ?? null,
    })
    .then(unwrapOne)
}

/** Deletes a category; referencing projects are detached, never deleted. */
export function deleteProjectCategory(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_PROJECT_CATEGORIES}/${id}`)
    .then(response => undefined)
}

/**
 * Lists projects (projects:manage).
 * @param {{ page?: number, size?: number, categoryId?: number|string, status?: string,
 *           projectStatus?: string, search?: string }} params
 */
export function listProjects(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  const categoryId = toIdOrNull(params.categoryId)
  if (categoryId) query.categoryId = categoryId
  if (params.status) query.status = params.status
  if (params.projectStatus) query.projectStatus = params.projectStatus
  const search = optional(params.search)
  if (search) query.search = search
  return apiClient
    .get(API_ENDPOINTS.ADMIN_PROJECTS, { params: query })
    .then(response => unwrapPage(response.data && response.data.data))
}

/** Creates a project (always starts DRAFT; duplicate slug → 409, bad date pair → 400). */
export function createProject(payload) {
  return apiClient
    .post(API_ENDPOINTS.ADMIN_PROJECTS, {
      slug: optional(payload.slug),
      title: optional(payload.title),
      summary: optional(payload.summary),
      description: optional(payload.description),
      categoryId: toIdOrNull(payload.categoryId) ?? undefined,
      projectStatus: optional(payload.projectStatus),
      location: optional(payload.location),
      startDate: optional(payload.startDate),
      endDate: optional(payload.endDate),
      objectives: optional(payload.objectives),
      impactSummary: optional(payload.impactSummary),
    })
    .then(unwrapOne)
}

/**
 * Partially updates a project. Slug is immutable; lifecycle status uses
 * the dedicated endpoints; empty optionals are sent as explicit null.
 */
export function updateProject(id, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_PROJECTS}/${id}`, {
      title: optional(payload.title) ?? null,
      summary: optional(payload.summary) ?? null,
      description: optional(payload.description) ?? null,
      categoryId: toIdOrNull(payload.categoryId) ?? null,
      projectStatus: optional(payload.projectStatus) ?? null,
      location: optional(payload.location) ?? null,
      startDate: optional(payload.startDate) ?? null,
      endDate: optional(payload.endDate) ?? null,
      objectives: optional(payload.objectives) ?? null,
      impactSummary: optional(payload.impactSummary) ?? null,
    })
    .then(unwrapOne)
}

export function publishProject(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_PROJECTS}/${id}/publish`)
    .then(unwrapOne)
}

export function archiveProject(id) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_PROJECTS}/${id}/archive`)
    .then(unwrapOne)
}

export function deleteProject(id) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_PROJECTS}/${id}`)
    .then(response => undefined)
}

/** Lists a project's image metadata (projects:manage). */
export function listProjectImages(projectId) {
  return apiClient
    .get(`${API_ENDPOINTS.ADMIN_PROJECTS}/${projectId}/images`)
    .then(response => unwrapPage(response.data && response.data.data).items)
}

export function createProjectImage(projectId, payload) {
  return apiClient
    .post(`${API_ENDPOINTS.ADMIN_PROJECTS}/${projectId}/images`, {
      imageUrl: optional(payload.imageUrl),
      altText: optional(payload.altText),
      displayOrder: toIdOrNull(payload.displayOrder) ?? undefined,
    })
    .then(unwrapOne)
}

export function updateProjectImage(projectId, imageId, payload) {
  return apiClient
    .patch(`${API_ENDPOINTS.ADMIN_PROJECTS}/${projectId}/images/${imageId}`, {
      imageUrl: optional(payload.imageUrl) ?? null,
      altText: optional(payload.altText) ?? null,
      displayOrder: toIdOrNull(payload.displayOrder) ?? null,
    })
    .then(unwrapOne)
}

export function deleteProjectImage(projectId, imageId) {
  return apiClient
    .delete(`${API_ENDPOINTS.ADMIN_PROJECTS}/${projectId}/images/${imageId}`)
    .then(response => undefined)
}

/** Reference lists for the job form (size 100 = backend PageParams.MAX_SIZE, one request). */
export function listJobCategories() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_JOB_CATEGORIES, { params: { size: 100 } })
    .then(response => unwrapPage(response.data && response.data.data).items)
}

export function listEmployers() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_EMPLOYERS, { params: { size: 100 } })
    .then(response => unwrapPage(response.data && response.data.data).items)
}

export function listSkillsCatalog() {
  return apiClient
    .get(API_ENDPOINTS.ADMIN_SKILLS, { params: { size: 100 } })
    .then(response => unwrapPage(response.data && response.data.data).items)
}
