import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

/**
 * Submits a public website enquiry to the backend.
 *
 * @param {{ name: string, email: string, phone?: string, category: string, subject: string, message: string }} payload
 * @returns {Promise<{ id: number, status: string, receivedAt: string }>} confirmation data
 */
export function submitEnquiry(payload) {
  return apiClient.post(API_ENDPOINTS.CONTACT_MESSAGES, payload).then(response => response.data.data)
}
