// services/api.ts
import axios from 'axios';
import { Issue, PageableResponse } from '../types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'withCredentials': 'true',
    'Access-Control-Allow-Origin': '*'
  }
});

// Request interceptor to add auth token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const createIssue = async (data: any): Promise<Issue> => {
  const response = await api.post('/issues', data);
  return response.data;
};

export const getIssues = async (page: number = 0, size: number = 10, sort?: string): Promise<PageableResponse<Issue>> => {
  const response = await api.get('/issues', { params: { page, size, sort } });
  return response.data;
};

export const getIssueById = async (id: number): Promise<Issue> => {
  const response = await api.get(`/issues/${id}`);
  return response.data;
};

export const updateIssue = async (id: number, data: any): Promise<Issue> => {
  const response = await api.put(`/issues/${id}`, data);
  return response.data;
};

export const patchIssue = async (id: number, data: any): Promise<Issue> => {
  const response = await api.patch(`/issues/${id}`, data);
  return response.data;
};

export const deleteIssue = async (id: number): Promise<void> => {
  await api.delete(`/issues/${id}`);
};

export const searchIssues = async (criteria: any[], page: number, size: number): Promise<PageableResponse<Issue>> => {
  const response = await api.post(`/issues/search?page=${page}&size=${size}`, criteria);
  return response.data;
};

export const getDashboardMetrics = async (): Promise<any> => {
  const response = await api.get('/dashboard/metrics');
  return response.data;
};

export const getCWEList = async (): Promise<any[]> => {
  const response = await api.get('/cwe');
  return response.data;
};

export const getCWEDetails = async (id: string): Promise<any> => {
  const response = await api.get(`/cwe/${id}`);
  return response.data;
};

export default api;