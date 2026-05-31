import axios from 'axios';

// Create an Axios instance configured to use the Vite proxy
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request Interceptor: Attach JWT Token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Response Interceptor: Handle Global Errors like 401 Unauthorized
api.interceptors.response.use((response) => {
  return response.data; // we assume backend returns { status, data, message }
}, (error) => {
  if (error.response && error.response.status === 401) {
    localStorage.removeItem('token');
    // 如果当前已经在 login 页面，则不进行强制重定向（避免输错密码时疯狂刷新页面）
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }
  return Promise.reject(error);
});

export default api;
