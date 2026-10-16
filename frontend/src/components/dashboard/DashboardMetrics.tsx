// components/dashboard/DashboardMetrics.tsx
import React, { useEffect, useState } from "react";
import { getDashboardMetrics } from "@/services/api";
import { CardShimmer } from "@/components/common/Shimmer";

import {
  ClipboardDocumentListIcon,
  ExclamationCircleIcon,
  ExclamationTriangleIcon,
  ClockIcon,
} from "@heroicons/react/24/solid";

interface Metrics {
  totalIssues: number;
  openIssues: number;
  criticalIssues: number;
  avgResolutionDays: number;
  severityBreakdown: Record<string, number>;
}

const DashboardMetrics: React.FC = () => {
  const [metrics, setMetrics] = useState<Metrics | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadMetrics();
  }, []);

  const loadMetrics = async () => {
    try {
      const data = await getDashboardMetrics();
      setMetrics(data);
    } catch (error) {
      console.error("Failed to load metrics:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <CardShimmer />;
  }

  if (!metrics) {
    return <div>Failed to load metrics</div>;
  }

  const metricCards = [
    {
      title: "Total Issues",
      value: metrics.totalIssues,
      icon: ClipboardDocumentListIcon,
      color: "bg-gray-500",
    },
    {
      title: "Open Issues",
      value: metrics.openIssues,
      icon: ExclamationCircleIcon,
      color: "bg-red-500",
    },
    {
      title: "Critical Issues",
      value: metrics.criticalIssues,
      icon: ExclamationTriangleIcon,
      color: "bg-purple-500",
    },
    {
      title: "Avg Resolution Time",
      value: `${metrics.avgResolutionDays.toFixed(1)} days`,
      icon: ClockIcon,
      color: "bg-green-500",
    },
  ];

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {metricCards.map((card) => (
          <div key={card.title} className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center">
              <div className={`${card.color} text-white rounded-full p-3 mr-4`}>
                <card.icon className="h-8 w-8" aria-hidden="true" />
              </div>
              <div>
                <p className="text-sm text-gray-600">{card.title}</p>
                <p className="text-2xl font-bold text-gray-900">{card.value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="bg-white rounded-lg shadow p-6">
        <h3 className="text-lg font-medium text-gray-900 mb-4">
          Severity Breakdown
        </h3>
        <div className="space-y-3">
          {Object.entries(metrics.severityBreakdown).map(
            ([severity, count]) => (
              <div key={severity}>
                <div className="flex justify-between mb-1">
                  <span className="text-sm font-medium text-gray-700">
                    {severity}
                  </span>
                  <span className="text-sm text-gray-600">{count}</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full ${
                      severity === "CRITICAL"
                        ? "bg-red-600"
                        : severity === "HIGH"
                          ? "bg-orange-500"
                          : severity === "MEDIUM"
                            ? "bg-yellow-500"
                            : "bg-blue-500"
                    }`}
                    style={{ width: `${(count / metrics.totalIssues) * 100}%` }}
                  />
                </div>
              </div>
            ),
          )}
        </div>
      </div>
    </div>
  );
};

export default DashboardMetrics;
