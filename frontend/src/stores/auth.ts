import { defineStore } from 'pinia';
import api from '../utils/api';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null as string | null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token
  },

  actions: {
    async login(credentials: any) {
      this.loading = true;
      this.error = null;
      try {
        const response: any = await api.post('/v1/auth/login', credentials);
        this.token = response.data.accessToken;
        this.user = { username: credentials.username }; // We just mock user from payload for now since backend doesn't return full user object in MVP
        
        localStorage.setItem('token', this.token || '');
        localStorage.setItem('user', JSON.stringify(this.user));
        
        return true;
      } catch (err: any) {
        this.error = err.response?.data?.message || '登录失败 / Login failed';
        return false;
      } finally {
        this.loading = false;
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
});
