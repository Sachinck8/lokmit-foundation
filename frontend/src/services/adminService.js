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
