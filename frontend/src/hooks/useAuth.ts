// hooks/useAuth.ts
import { useState, useEffect } from 'react';
import { useRouter } from 'next/router';
import { login as apiLogin, register as apiRegister, getCurrentUser, logout as apiLogout} from '@/services/auth';

interface User {
  id: number;
  username: string;
  email: string;
  role: string;
}

export const useAuth = () => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      loadUser();
    } else {
      setLoading(false);
    }
  }, []);

  const loadUser = async () => {
    try {
      const userData = await getCurrentUser();
      setUser(userData);
    } catch (error) {
      localStorage.removeItem('token');
      setUser(null);
    } finally {
      setLoading(false);
    }
  };

  const login = async (username: string, password: string) => {
    const response = await apiLogin(username, password);
    setToken(response.token);
    localStorage.setItem('token', response.token);
    await loadUser();
    router.push('/dashboard');
    return response;
  };

  const register = async (username: string, password: string, email: string) => {
    const response = await apiRegister(username, password, email);
    return response;
  };

  const logout = async () => {
    await apiLogout();
    localStorage.removeItem('token');
    setUser(null);
    router.push('/login');
  };

  return {
    user,
    token,
    loading,
    isAuthenticated: !!user,
    login,
    register,
    logout
  };
};