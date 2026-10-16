// src/services/dependencyScanner.ts
import api from './api';
import { ParsedDependency, DependencyScanResult, PackageCpeMapping } from '@/types/dependency';

export const parseManifest = async (file: File): Promise<ParsedDependency[]> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post('/api/admin/dependency/scan/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const scanDependencies = async (file: File, autoCreateIssues: boolean = false): Promise<DependencyScanResult> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('autoCreateIssues', String(autoCreateIssues));
  const response = await api.post('/api/admin/dependency/scan/scan', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const getCpeMappings = async (): Promise<PackageCpeMapping[]> => {
  const response = await api.get('/api/admin/cpe-mappings');
  return response.data;
};

export const addCpeMapping = async (mapping: Partial<PackageCpeMapping>): Promise<PackageCpeMapping> => {
  const response = await api.post('/api/admin/cpe-mappings', mapping);
  return response.data;
};

export const deleteCpeMapping = async (id: number): Promise<void> => {
  await api.delete(`/admin/cpe-mappings/${id}`);
};

export const searchCpeMapping = async (product: string): Promise<PackageCpeMapping> => {
  const response = await api.get(`/api/admin/cpe-mappings/search?product=${product}`);
  return response.data;
};

export interface NvdSyncState {
  status: "started" | "already_running";
  detail: string;
}

export interface NvdSyncStatus {
  running: boolean;
}

/** Trigger a full NVD cache sync (admin only). Returns 202 immediately. */
export const triggerNvdSync = async (): Promise<NvdSyncState> => {
  const response = await api.post("/api/admin/nvd/sync");
  return response.data;
};

/** Poll whether an NVD sync is currently running. */
export const getNvdSyncStatus = async (): Promise<NvdSyncStatus> => {
  const response = await api.get("/api/admin/nvd/sync/status");
  return response.data;
};