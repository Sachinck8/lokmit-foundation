import apiClient from '../api/client.js'
import { API_ENDPOINTS } from '../constants/apiEndpoints.js'

export function getHealth() {
  return apiClient.get(API_ENDPOINTS.HEALTH)
}