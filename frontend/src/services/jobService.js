import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

/**
 * Public job browsing (A8). Anonymous — no auth headers, no tokens.
 * Backend returns only PUBLISHED jobs; drafts/closed/archived behave as
 * not-found and never appear in listings.
 */

/**
 * Lists published jobs with pagination and supported filters.
 *
 * @param {{ page?: number, size?: number, categoryId?: number|string, employmentType?: string, workMode?: string, search?: string }} params
 * @returns {Promise<{ items: Array, page: number, size: number, totalItems: number, totalPages: number }>} page payload
 */
export function listJobs(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.categoryId) query.categoryId = params.categoryId
  if (params.employmentType) query.employmentType = params.employmentType
  if (params.workMode) query.workMode = params.workMode
  if (params.search && String(params.search).trim() !== '') query.search = params.search.trim()
  return apiClient
    .get(API_ENDPOINTS.JOBS, { params: query })
    .then(response => {
      const data = response.data && response.data.data
      if (!data || !Array.isArray(data.items)) {
        throw new Error('Unexpected response shape from the jobs API.')
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

/**
 * Fetches one published job by id. Unknown and non-public ids both 404.
 *
 * @param {number|string} jobId
 * @returns {Promise<Object>} public job payload
 */
export function getJob(jobId) {
  return apiClient
    .get(`${API_ENDPOINTS.JOBS}/${jobId}`)
    .then(response => {
      const data = response.data && response.data.data
      if (!data || typeof data !== 'object' || data.id === undefined) {
        throw new Error('Unexpected response shape from the jobs API.')
      }
      return data
    })
}
