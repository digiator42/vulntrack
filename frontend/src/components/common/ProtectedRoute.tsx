// components/common/ProtectedRoute.tsx
import React, { useEffect } from 'react';
import { useRouter } from 'next/router';
import { useAuth } from '@/hooks/useAuth';
import LoadingSpinner from './LoadingSpinner';

export function withAuth<P extends object>(
  WrappedComponent: React.ComponentType<P>
) {
  return function WithAuthComponent(props: P) {
    const { isAuthenticated, loading } = useAuth();
    const router = useRouter();

    useEffect(() => {
      if (!loading && !isAuthenticated) {
        router.push('/login');
      }
    }, [isAuthenticated, loading, router]);

    if (loading) {
      return (
        <div className="flex justify-center items-center min-h-screen">
          <LoadingSpinner />
        </div>
      );
    }

    if (!isAuthenticated) {
      return null;
    }

    return <WrappedComponent {...props} />;
  };
}