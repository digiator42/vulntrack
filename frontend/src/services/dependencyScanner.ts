// src/services/dependencyScanner.ts
import api from './api';
import { ParsedDependency, DependencyScanResult, PackageCpeMapping } from '@/types/dependency';

export const parseManifest = async (file: File): Promise<ParsedDependency[]> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post('/admin/dependency/scan/parse', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const scanDependencies = async (file: File, autoCreateIssues: boolean = false): Promise<DependencyScanResult> => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('autoCreateIssues', String(autoCreateIssues));
  const response = await api.post('/admin/dependency/scan/scan', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return response.data;
};

export const getCpeMappings = async (): Promise<PackageCpeMapping[]> => {
  const response = await api.get('/admin/cpe-mappings');
  return response.data;
};

export const addCpeMapping = async (mapping: Partial<PackageCpeMapping>): Promise<PackageCpeMapping> => {
  const response = await api.post('/admin/cpe-mappings', mapping);
  return response.data;
};

export const deleteCpeMapping = async (id: number): Promise<void> => {
  await api.delete(`/admin/cpe-mappings/${id}`);
};

export const searchCpeMapping = async (product: string): Promise<PackageCpeMapping> => {
  const response = await api.get(`/admin/cpe-mappings/search?product=${product}`);
  return response.data;
};