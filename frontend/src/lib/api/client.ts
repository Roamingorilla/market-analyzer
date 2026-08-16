import axios from 'axios';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: `${API_BASE}/api/v1`,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use((config) => {
  return config;
});

// Response interceptor — unwrap ApiResponse wrapper
apiClient.interceptors.response.use(
  (response) => {
    if (response.data?.success !== undefined) {
      return { ...response, data: response.data.data };
    }
    return response;
  },
  (error) => {
    const message = error.response?.data?.error || error.message || 'Network error';
    return Promise.reject(new Error(message));
  }
);

export default apiClient;
