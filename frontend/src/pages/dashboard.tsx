// pages/dashboard.tsx
import React from "react";
import DashboardMetrics from "@/components/dashboard/DashboardMetrics";
import { withAuth } from "@/components/common/ProtectedRoute";

const DashboardPage: React.FC = () => {
  return (
    <div className="py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>
      <DashboardMetrics />
    </div>
  );
};

export default withAuth(DashboardPage);
