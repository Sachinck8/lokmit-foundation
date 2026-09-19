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
    .get(`${API_ENDPOINTS.ADMIN_APPLICATIONS}/${applicationId}/interviews`, {
      params: { page, size },
    })
    .then(response => unwrapPage(response.data && response.data.data))
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
