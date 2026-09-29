import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

/**
 * Public CMS content API layer (A26). Anonymous read-only access over the
 * shared axios client: published services/expertise areas and active
 * categories, mirroring the backend ApiPaths.PUBLIC_* constants.
 *
 * Contract notes (mirroring candidateService conventions):
 * - every call unwraps the standard { success, data } envelope;
 * - list endpoints validate the response shape and throw on surprises;
 * - callers implement static-content fallback — a failure here must degrade
 *   to the built-in page copy, never to an error page.
 */

function unwrap(response) {
  return response.data && response.data.data
}

function unwrapPage(response) {
  const data = response.data && response.data.data
  if (!data || !Array.isArray(data.items)) {
    throw new Error('Unexpected response shape from the public content API.')
  }
  return {
    items: data.items,
    page: typeof data.page === 'number' ? data.page : 0,
    size: typeof data.size === 'number' ? data.size : 20,
    totalItems: typeof data.totalItems === 'number' ? data.totalItems : data.items.length,
    totalPages: typeof data.totalPages === 'number' ? data.totalPages : 1,
  }
}

// ---------------------------------------------------------------------
// services
// ---------------------------------------------------------------------

/** Lists published services (paginated, display order). */
export function listPublishedServices(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  return apiClient.get(API_ENDPOINTS.PUBLIC_SERVICES, { params: query })
    .then(unwrapPage)
}

/** Fetches one published service by slug (404-masked). */
export function getPublishedService(slug) {
  return apiClient.get(API_ENDPOINTS.PUBLIC_SERVICE(slug)).then(unwrap)
}

/** Lists active service categories in display order. */
export function listServiceCategories() {
  return apiClient.get(API_ENDPOINTS.PUBLIC_SERVICE_CATEGORIES)
    .then(response => {
      const data = unwrap(response)
      if (!Array.isArray(data)) {
        throw new Error('Unexpected response shape from the service categories API.')
      }
      return data
    })
}

// ---------------------------------------------------------------------
// expertise areas
// ---------------------------------------------------------------------

/** Lists published expertise areas in display order. */
export function listExpertiseAreas() {
  return apiClient.get(API_ENDPOINTS.PUBLIC_EXPERTISE_AREAS)
    .then(response => {
      const data = unwrap(response)
      if (!Array.isArray(data)) {
        throw new Error('Unexpected response shape from the expertise areas API.')
      }
      return data
    })
}

// ---------------------------------------------------------------------
// projects
// ---------------------------------------------------------------------

/**
 * Lists published projects (paginated, newest first).
 *
 * @param {{ page?: number, size?: number, category?: string }} params
 *        category is an ACTIVE project-category slug filter.
 */
export function listPublishedProjects(params = {}) {
  const query = {}
  if (params.page !== undefined && params.page !== null) query.page = params.page
  if (params.size !== undefined && params.size !== null) query.size = params.size
  if (params.category) query.category = params.category
  return apiClient.get(API_ENDPOINTS.PUBLIC_PROJECTS, { params: query })
    .then(unwrapPage)
}

/** Fetches one published project by slug (404-masked). */
export function getPublishedProject(slug) {
  return apiClient.get(API_ENDPOINTS.PUBLIC_PROJECT(slug)).then(unwrap)
}

/** Lists active project categories in display order. */
export function listProjectCategories() {
  return apiClient.get(API_ENDPOINTS.PUBLIC_PROJECT_CATEGORIES)
    .then(response => {
      const data = unwrap(response)
      if (!Array.isArray(data)) {
        throw new Error('Unexpected response shape from the project categories API.')
      }
      return data
    })
}
