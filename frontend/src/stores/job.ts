import { defineStore } from 'pinia';
import api from '../utils/api';
import { resolveApiError } from '../utils/apiError';

export const useJobStore = defineStore('job', {
  state: () => ({
    jobs: [] as any[],
    loading: false,
    error: null as string | null,
    totalElements: 0,
    totalPages: 0,
  }),
  
  actions: {
    async fetchJobs(page = 0, size = 10) {
      this.loading = true;
      this.error = null;
      try {
        const response: any = await api.get('/jobs', { params: { page, size } });
        // Assume structure ApiResponse<PageResponse<JobResponse>>
        this.jobs = response.data.content;
        this.totalElements = response.data.totalElements;
        this.totalPages = response.data.totalPages;
      } catch (err: any) {
        this.error = resolveApiError(err, 'Failed to fetch jobs').summary;
      } finally {
        this.loading = false;
      }
    },

    async createJob(payload: { title: string, description: string, requirements: any }) {
      this.loading = true;
      try {
        const response: any = await api.post('/jobs', payload);
        this.jobs.unshift(response.data);
        return true;
      } catch (err: any) {
        this.error = resolveApiError(err, 'Failed to create job').summary;
        return false;
      } finally {
        this.loading = false;
      }
    }
  }
});
