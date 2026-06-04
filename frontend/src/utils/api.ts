import axios, { type InternalAxiosRequestConfig } from 'axios';

type ApiRequestConfig = InternalAxiosRequestConfig & {
  skipAuth?: boolean;
  skipAuthRedirect?: boolean;
};

// Create an Axios instance configured to use the Vite proxy
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request Interceptor: Attach JWT Token
api.interceptors.request.use((requestConfig) => {
  const config = requestConfig as ApiRequestConfig;
  const token = localStorage.getItem('token');
  if (!config.skipAuth && token && config.headers) {
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
  const config = error.config as ApiRequestConfig | undefined;

  if (error.response && error.response.status === 401 && !config?.skipAuthRedirect) {
    localStorage.removeItem('token');
    // 如果当前已经在 login 页面，则不进行强制重定向（避免输错密码时疯狂刷新页面）
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }
  return Promise.reject(error);
});

export default api;
