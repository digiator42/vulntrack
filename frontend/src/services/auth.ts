// services/auth.ts
import api from './api';

interface LoginResponse {
  token: string;
}

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export const login = async (username: string, password: string): Promise<LoginResponse> => {
  const response = await api.post('/auth/login', { username, password });
  return response.data;
};

export const logout = async (): Promise<string> => {
  const response = await api.post('/auth/logout');
  return response.data;
};

export const register = async (username: string, password: string, email: string): Promise<string> => {
  const response = await api.post('/auth/register', { username, password, email });
  return response.data;
};

export const getCurrentUser = async (): Promise<User> => {
  const response = await api.get('/auth/me');
  return response.data;
};