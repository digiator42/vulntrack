// pages/issues/create.tsx
import React from "react";
import CreateIssueForm from "@/components/issues/CreateIssueForm";
import { withAuth } from "@/components/common/ProtectedRoute";

const CreateIssuePage: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900">
          Create New Vulnerability Issue
        </h1>
        <p className="text-gray-600 mt-1">
          Enter vulnerability details and calculate CVSS score
        </p>
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <CreateIssueForm />
      </div>
    </div>
  );
};

export default withAuth(CreateIssuePage);
