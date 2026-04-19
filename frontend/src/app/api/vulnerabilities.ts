// frontend/src/lib/api/vulnerabilities.ts
import { apiClient } from './client';
import type {
    Vulnerability,
    CreateVulnerabilityRequest,
    UpdateVulnerabilityRequest,
    PatchVulnerabilityRequest,
    VulnerabilityPage
} from '@/types/vulnerability';

export const vulnerabilityApi = {
    // Get all vulnerabilities for current user
    getAll: async (params?: {
        page?: number;
        size?: number;
        sort?: string;
    }): Promise<VulnerabilityPage> => {
        const { data } = await apiClient.get('/api/issues', { params });
        return data;
    },

    // Get vulnerability by ID
    getById: async (id: number): Promise<Vulnerability> => {
        const { data } = await apiClient.get(`/api/issues/${id}`);
        return data;
    },

    // Create new vulnerability
    create: async (payload: CreateVulnerabilityRequest): Promise<Vulnerability> => {
        const { data } = await apiClient.post('/api/issues', payload);
        return data;
    },

    // Update vulnerability (PUT)
    update: async (id: number, payload: UpdateVulnerabilityRequest): Promise<Vulnerability> => {
        const { data } = await apiClient.put(`/api/issues/${id}`, payload);
        return data;
    },

    // Patch vulnerability (PATCH)
    patch: async (id: number, payload: PatchVulnerabilityRequest): Promise<Vulnerability> => {
        const { data } = await apiClient.patch(`/api/issues/${id}`, payload);
        return data;
    },

    // Delete vulnerability
    delete: async (id: number): Promise<void> => {
        await apiClient.delete(`/api/issues/${id}`);
    },
};