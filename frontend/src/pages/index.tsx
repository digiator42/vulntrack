// pages/index.tsx
import React, { useEffect } from "react";
import { useRouter } from "next/router";
import { useAuth } from "@/hooks/useAuth";
import { PageShimmer } from "@/components/common/Shimmer";

export default function Home() {
  const { isAuthenticated, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading) {
      if (isAuthenticated) {
        router.push("/dashboard");
      } else {
        router.push("/scan");
      }
    }
  }, [isAuthenticated, loading, router]);

  return <PageShimmer />;
}
