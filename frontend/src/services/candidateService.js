import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

/**
 * Candidate self-service API layer (A10). All calls carry the bearer token
 * via the shared axios client; ownership is resolved server-side — no
 * candidateId is ever sent or trusted from the client.
 */

// ---------------------------------------------------------------------
// profile
// ---------------------------------------------------------------------

/** Fetches the authenticated candidate's own profile. */
export function getMyProfile() {
  return apiClient.get(API_ENDPOINTS.CANDIDATE_ME_PROFILE)
    .then(response => response.data && response.data.data)
}

/** Partially updates the authenticated candidate's own profile. */
export function updateMyProfile(patch) {
  return apiClient.patch(API_ENDPOINTS.CANDIDATE_ME_PROFILE, patch)
    .then(response => response.data && response.data.data)
}

// ---------------------------------------------------------------------
// resumes
// ---------------------------------------------------------------------

/** Lists the authenticated candidate's own resumes (metadata only). */
export function listMyResumes() {
  return apiClient.get(API_ENDPOINTS.CANDIDATE_ME_RESUMES)
    .then(response => {
      const data = response.data && response.data.data
      if (!Array.isArray(data)) {
        throw new Error('Unexpected response shape from the resumes API.')
      }
      return data
    })
}

/** Uploads (or replaces) the active resume. multipart field: file. */
export function uploadMyResume(file) {
  const formData = new FormData()
  formData.append('file', file)
  return apiClient.post(API_ENDPOINTS.CANDIDATE_ME_RESUMES, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(response => response.data && response.data.data)
}

/** Downloads the caller's own ACTIVE resume as a Blob. */
export function downloadMyResume(resumeId) {
  return apiClient.get(`/resumes/${resumeId}/download`, {
    responseType: 'blob',
  }).then(response => {
    const disposition = response.headers && response.headers['content-disposition']
    let filename = 'resume'
    if (disposition) {
      const starMatch = disposition.match(/filename\*=UTF-8''([^;]+)/i)
      if (starMatch && starMatch[1]) {
        try {
          filename = decodeURIComponent(starMatch[1])
        } catch {
          filename = 'resume'
        }
      } else {
        const plainMatch = disposition.match(/filename="?([^";]+)"?/i)
        if (plainMatch && plainMatch[1]) filename = plainMatch[1]
      }
    }
    return { blob: response.data, filename }
  })
}

/** Deletes one of the caller's own resumes (204 on success). */
export function deleteMyResume(resumeId) {
  return apiClient.delete(`/resumes/${resumeId}`)
    .then(() => true)
}

// ---------------------------------------------------------------------
// applications
// ---------------------------------------------------------------------

/**
 * Lists the authenticated candidate's own applications (paginated).
 *
 * @returns {Promise<{ items: Array, page: number, size: number, totalItems: number, totalPages: number }>}
 */
export function listMyApplications(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  return apiClient.get(API_ENDPOINTS.CANDIDATE_ME_APPLICATIONS, { params: query })
    .then(response => {
      const data = response.data && response.data.data
      if (!data || !Array.isArray(data.items)) {
        throw new Error('Unexpected response shape from the applications API.')
      }
      return {
        items: data.items,
        page: typeof data.page === 'number' ? data.page : 0,
        size: typeof data.size === 'number' ? data.size : 20,
        totalItems: typeof data.totalItems === 'number' ? data.totalItems : data.items.length,
        totalPages: typeof data.totalPages === 'number' ? data.totalPages : 1,
      }
    })
}

/** Fetches one of the caller's own applications by id. */
export function getMyApplication(applicationId) {
  return apiClient.get(`${API_ENDPOINTS.CANDIDATE_ME_APPLICATIONS}/${applicationId}`)
    .then(response => response.data && response.data.data)
}

/**
 * Submits an application for a published job.
 *
 * @param {{ jobId: number, resumeId?: number|null, coverNote?: string }} payload
 * @returns {Promise<Object>} the created application DTO
 */
export function applyToJob({ jobId, resumeId = null, coverNote = '' }) {
  const body = { jobId }
  if (resumeId) body.resumeId = resumeId
  if (coverNote && coverNote.trim() !== '') body.coverNote = coverNote.trim()
  return apiClient.post(API_ENDPOINTS.CANDIDATE_ME_APPLICATIONS, body)
    .then(response => response.data && response.data.data)
}
