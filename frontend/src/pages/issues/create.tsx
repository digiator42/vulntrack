// pages/issues/create.tsx
import React from "react";
import CreateIssueForm from "@/components/issues/CreateIssueForm";
import { withAuth } from "@/components/common/ProtectedRoute";

const CreateIssuePage: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          Create New Vulnerability Issue
        </h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">
          Enter vulnerability details and calculate CVSS score
        </p>
      </div>

      <div className="bg-white dark:bg-gray-900 rounded-lg shadow p-6">
        <CreateIssueForm />
      </div>
    </div>
  );
};

// Only ADMIN and SECURITY_ADMIN can create issues
export default withAuth(CreateIssuePage, {
  allowedRoles: ["ROLE_ADMIN", "ROLE_SECURITY_ADMIN"],
  redirectTo: "/issues",
});
