import { defineStore } from 'pinia';
import api from '../utils/api';
import { resolveApiError } from '../utils/apiError';

export type UserRole = 'HR' | 'CANDIDATE' | 'ADMIN';

export interface OrganizationRef {
  id: string;
  name: string;
}

export interface AuthUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  organization: OrganizationRef | null;
  createdAt: string;
}

interface LoginCredentials {
  username: string;
  password: string;
  rememberMe?: boolean;
}

interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  organizationId?: string | null;
  organizationToken?: string | null;
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
        
        // If rememberMe wasn't passed, default behavior remains
        localStorage.setItem('token', this.token || '');
        localStorage.setItem('user', JSON.stringify(this.user));
        
        return this.user;
      } catch (err: any) {
        this.error = resolveApiError(err, '登录失败 / Login failed').summary;
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
        this.error = resolveApiError(err, '注册失败 / Register failed').summary;
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
        this.error = resolveApiError(err, '获取用户信息失败').summary;
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
      localStorage.removeItem('savedCreds');
    }
  }
});
