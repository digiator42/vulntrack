// components/common/ProtectedRoute.tsx
import React, { useEffect } from "react";
import { useRouter } from "next/router";
import { useAuth } from "@/hooks/useAuth";
import { PageShimmer } from "./Shimmer";

interface WithAuthOptions {
  allowedRoles?: string[];
  redirectTo?: string;
}

export function withAuth<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  options: WithAuthOptions = {},
) {
  return function WithAuthComponent(props: P) {
    const { isAuthenticated, loading, user } = useAuth();
    const router = useRouter();
    const { allowedRoles, redirectTo = "/scan" } = options;

    useEffect(() => {
      if (!loading) {
        if (!isAuthenticated) {
          router.push("/scan");
          return;
        }

        // Check role-based access
        if (allowedRoles && allowedRoles.length > 0 && user) {
          const userRole = user.role;
          if (!allowedRoles.includes(userRole)) {
            router.push(redirectTo);
            return;
          }
        }
      }
    }, [isAuthenticated, loading, user, router, allowedRoles, redirectTo]);

    if (loading) {
      return <PageShimmer />;
    }

    if (!isAuthenticated) {
      return null;
    }

    // Role check before rendering
    if (allowedRoles && allowedRoles.length > 0 && user) {
      if (!allowedRoles.includes(user.role)) {
        return null;
      }
    }

    return <WrappedComponent {...props} />;
  };
}
