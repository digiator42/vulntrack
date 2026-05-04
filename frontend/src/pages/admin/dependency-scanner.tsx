// src/pages/admin/dependency-scanner.tsx
import React from "react";
import { withAuth } from "@/components/common/ProtectedRoute";
import DependencyScanner from "@/components/dependency/DependencyScanner";
import { useRouter } from "next/router";

const AdminDependencyScannerPage: React.FC = () => {
  const router = useRouter();

  const handleIssueCreated = () => {
    // Refresh or show notification
  };

  return (
    <div className="max-w-6xl mx-auto py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">
          Dependency Vulnerability Scanner
        </h1>
        <p className="text-gray-600 mt-1">
          Upload manifest files to automatically detect vulnerable dependencies
          and create security issues
        </p>
      </div>
      <DependencyScanner onIssueCreated={handleIssueCreated} />
    </div>
  );
};

// Only ADMIN and SECURITY_ADMIN can access
export default withAuth(AdminDependencyScannerPage, {
  allowedRoles: ["ROLE_ADMIN", "ROLE_SECURITY_ADMIN"],
  redirectTo: "/dashboard",
});
