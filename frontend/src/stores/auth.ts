import { defineStore } from 'pinia';
import api from '../utils/api';

export type UserRole = 'HR' | 'CANDIDATE' | 'ADMIN';

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  createdAt: string;
}

interface LoginCredentials {
  username: string;
  password: string;
}

interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  role: UserRole;
}

const parseStoredUser = (): AuthUser | null => {
  const raw = localStorage.getItem('user');
  if (!raw) return null;

  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    localStorage.removeItem('user');
    return null;
  }
};

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: parseStoredUser() as AuthUser | null,
    token: localStorage.getItem('token') || null,
    loading: false,
    error: null as string | null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    homeRouteName: (state) => {
      if (state.user?.role === 'CANDIDATE') return 'candidateDashboard'
      if (state.user?.role === 'ADMIN') return 'adminConsole'
      return 'dashboard'
    }
  },

  actions: {
    async login(credentials: LoginCredentials) {
      this.loading = true;
      this.error = null;
      try {
        const response: any = await api.post('/v1/auth/login', credentials);
        this.token = response.data.accessToken;
        this.user = response.data.user;
        
        localStorage.setItem('token', this.token || '');
        localStorage.setItem('user', JSON.stringify(this.user));
        
        return this.user;
      } catch (err: any) {
        this.error = err.response?.data?.message || '登录失败 / Login failed';
        return null;
      } finally {
        this.loading = false;
      }
    },

    async register(payload: RegisterPayload) {
      this.loading = true;
      this.error = null;
      try {
        const response: any = await api.post('/v1/auth/register', payload);
        return response.data;
      } catch (err: any) {
        this.error = err.response?.data?.message || '注册失败 / Register failed';
        throw err;
      } finally {
        this.loading = false;
      }
    },

    async fetchCurrentUser() {
      if (!this.token) {
        return null;
      }

      this.loading = true;
      try {
        const response: any = await api.get('/v1/users/me');
        this.user = response.data;
        localStorage.setItem('user', JSON.stringify(this.user));
        return this.user;
      } catch (err: any) {
        this.error = err.response?.data?.message || '获取用户信息失败';
        this.logout();
        return null;
      } finally {
        this.loading = false;
      }
    },

    logout() {
      this.user = null;
      this.token = null;
      this.error = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
    }
  }
});
