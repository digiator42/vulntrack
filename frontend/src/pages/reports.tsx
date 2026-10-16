// src/pages/reports.tsx
import React, { useState, useEffect } from "react";
import { withAuth } from "@/components/common/ProtectedRoute";
import { PageShimmer } from "@/components/common/Shimmer";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import {
  ChartBarIcon,
  DocumentArrowDownIcon,
  ShieldCheckIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
} from "@heroicons/react/24/outline";

interface ReportSummary {
  totalIssues: number;
  openIssues: number;
  resolvedIssues: number;
  averageCvss: number;
  severityBreakdown: {
    CRITICAL: number;
    HIGH: number;
    MEDIUM: number;
    LOW: number;
  };
  topCwes: Record<string, number>;
}

const ReportsPage: React.FC = () => {
  const { user } = useAuth();
  const [data, setData] = useState<ReportSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    fetchSummary();
  }, []);

  const fetchSummary = async () => {
    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/report/summary`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });
      const result = await response.json();
      setData(result);
    } catch (error) {
      console.error("Failed to fetch report summary:", error);
      toast.error("Failed to load report data");
    } finally {
      setLoading(false);
    }
  };

  const downloadPDF = async () => {
    setDownloading(true);
    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/report/pdf`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
        },
      });

      if (!response.ok) throw new Error("Download failed");

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `vulntrack-report-${new Date().toISOString().split("T")[0]}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success("Report downloaded successfully");
    } catch (error) {
      console.error("Download failed:", error);
      toast.error("Failed to download report");
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return <PageShimmer />;
  }

  if (!data) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">No report data available</p>
      </div>
    );
  }

  const getSeverityColor = (severity: string) => {
    const colors = {
      CRITICAL: "bg-red-500",
      HIGH: "bg-orange-500",
      MEDIUM: "bg-yellow-500",
      LOW: "bg-blue-500",
    };
    return colors[severity as keyof typeof colors] || "bg-gray-500";
  };

  const getSeverityBgLight = (severity: string) => {
    const colors = {
      CRITICAL: "bg-red-50 border-red-200",
      HIGH: "bg-orange-50 border-orange-200",
      MEDIUM: "bg-yellow-50 border-yellow-200",
      LOW: "bg-blue-50 border-blue-200",
    };
    return colors[severity as keyof typeof colors] || "bg-gray-50";
  };

  const getCVSSRating = (score: number) => {
    if (score >= 9)
      return { text: "Critical", color: "text-red-700", bg: "bg-red-100" };
    if (score >= 7)
      return { text: "High", color: "text-orange-700", bg: "bg-orange-100" };
    if (score >= 4)
      return { text: "Medium", color: "text-yellow-700", bg: "bg-yellow-100" };
    return { text: "Low", color: "text-blue-700", bg: "bg-blue-100" };
  };

  const severityRatings = [
    {
      name: "CRITICAL",
      count: data.severityBreakdown?.CRITICAL || 0,
      color: "bg-red-500",
    },
    {
      name: "HIGH",
      count: data.severityBreakdown?.HIGH || 0,
      color: "bg-orange-500",
    },
    {
      name: "MEDIUM",
      count: data.severityBreakdown?.MEDIUM || 0,
      color: "bg-yellow-500",
    },
    {
      name: "LOW",
      count: data.severityBreakdown?.LOW || 0,
      color: "bg-blue-500",
    },
  ];

  const totalIssues = data.totalIssues;
  const resolvedPercentage =
    totalIssues > 0 ? (data.resolvedIssues / totalIssues) * 100 : 0;
  const openPercentage =
    totalIssues > 0 ? (data.openIssues / totalIssues) * 100 : 0;
  const cvssRating = getCVSSRating(data.averageCvss);

  const topCWEs = Object.entries(data.topCwes || {})
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Security Posture Report
            </h1>
            <p className="text-gray-600 mt-1">
              Comprehensive vulnerability overview for{" "}
              {user?.role?.replace("ROLE_", "")}
            </p>
          </div>
          <button
            onClick={downloadPDF}
            disabled={downloading}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
          >
            <DocumentArrowDownIcon className="h-5 w-5" />
            {downloading ? "Generating..." : "Download PDF Report"}
          </button>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 font-medium">Total Issues</p>
              <p className="text-3xl font-bold text-gray-900 mt-1">
                {data.totalIssues}
              </p>
            </div>
            <div className="bg-blue-100 rounded-full p-3">
              <ShieldCheckIcon className="h-6 w-6 text-blue-600" />
            </div>
          </div>
          <div className="mt-4 flex items-center gap-2">
            <span className="text-xs text-gray-500">All vulnerabilities</span>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 font-medium">
                Average CVSS Score
              </p>
              <div className="flex items-baseline gap-1 mt-1">
                <p className="text-3xl font-bold text-gray-900">
                  {data.averageCvss.toFixed(1)}
                </p>
                <p className="text-sm text-gray-500">/ 10</p>
              </div>
            </div>
            <div className={`${cvssRating.bg} rounded-full p-3`}>
              <ExclamationTriangleIcon
                className={`h-6 w-6 ${cvssRating.color}`}
              />
            </div>
          </div>
          <div className="mt-4">
            <span
              className={`text-xs font-medium px-2 py-1 rounded ${cvssRating.bg} ${cvssRating.color}`}
            >
              {cvssRating.text} Severity
            </span>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 font-medium">Open Issues</p>
              <p className="text-3xl font-bold text-orange-600 mt-1">
                {data.openIssues}
              </p>
            </div>
            <div className="bg-orange-100 rounded-full p-3">
              <ClockIcon className="h-6 w-6 text-orange-600" />
            </div>
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-orange-500 rounded-full h-2 transition-all"
                style={{ width: `${openPercentage}%` }}
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 font-medium">
                Resolved Issues
              </p>
              <p className="text-3xl font-bold text-green-600 mt-1">
                {data.resolvedIssues}
              </p>
            </div>
            <div className="bg-green-100 rounded-full p-3">
              <CheckCircleIcon className="h-6 w-6 text-green-600" />
            </div>
          </div>
          <div className="mt-4">
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-green-500 rounded-full h-2 transition-all"
                style={{ width: `${resolvedPercentage}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Charts Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Severity Distribution */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-gray-900">
              Severity Distribution
            </h3>
            <ChartBarIcon className="h-5 w-5 text-gray-400" />
          </div>
          <div className="space-y-4">
            {severityRatings.map((severity) => {
              const count = severity.count;
              const percentage =
                totalIssues > 0 ? (count / totalIssues) * 100 : 0;
              return (
                <div key={severity.name}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="font-medium text-gray-700">
                      {severity.name}
                    </span>
                    <span className="text-gray-600">
                      {count} ({percentage.toFixed(1)}%)
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                    <div
                      className={`${severity.color} rounded-full h-3 transition-all duration-500`}
                      style={{ width: `${percentage}%` }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          {totalIssues > 0 && (
            <div className="mt-6 pt-4 border-t border-gray-200">
              <div className="flex justify-around">
                {severityRatings.map((severity) => (
                  <div key={severity.name} className="text-center">
                    <div
                      className={`w-3 h-3 ${severity.color} rounded-full mx-auto mb-1`}
                    />
                    <p className="text-xs text-gray-600">{severity.name}</p>
                    <p className="text-sm font-bold text-gray-900">
                      {severity.count}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Top CWEs */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Top CWEs</h3>
            <span className="text-xs text-gray-500">
              Most Common Weaknesses
            </span>
          </div>
          {topCWEs.length > 0 ? (
            <div className="space-y-4">
              {topCWEs.map(([cweId, count], index) => (
                <div
                  key={cweId}
                  className="group hover:bg-gray-50 p-2 rounded-lg transition-colors"
                >
                  <div className="flex justify-between items-center mb-1">
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-bold text-gray-400 w-6">
                        #{index + 1}
                      </span>
                      <span className="font-mono text-sm text-gray-700">
                        {cweId}
                      </span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900">
                      {count}
                    </span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2">
                    <div
                      className="bg-purple-500 rounded-full h-2 transition-all"
                      style={{ width: `${(count / totalIssues) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <p className="text-gray-400">No CWE data available</p>
            </div>
          )}
        </div>
      </div>

      {/* Risk Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {severityRatings
          .filter((s) => s.count > 0)
          .map((severity) => (
            <div
              key={severity.name}
              className={`rounded-lg shadow-sm border p-6 ${getSeverityBgLight(severity.name)}`}
            >
              <div className="flex justify-between items-start">
                <div>
                  <h4 className="text-sm font-medium text-gray-700">
                    Critical Risk Alert
                  </h4>
                  <p className="text-2xl font-bold mt-2">{severity.count}</p>
                  <p className="text-sm text-gray-600 mt-1">
                    {severity.name.toLowerCase()} severity vulnerabilities
                  </p>
                </div>
                <div
                  className={`w-12 h-12 ${getSeverityColor(severity.name)} rounded-full opacity-20`}
                />
              </div>
              <button
                onClick={() =>
                  (window.location.href = `/issues?severity=${severity.name}`)
                }
                className="mt-4 text-sm font-medium text-blue-600 hover:text-blue-700"
              >
                View Details →
              </button>
            </div>
          ))}
      </div>

      {/* Footer Info */}
      <div className="bg-gray-50 rounded-lg p-4 text-center text-sm text-gray-500 border border-gray-200">
        <p className="text-xs mt-1">
          {user?.role === "ROLE_ADMIN" || user?.role === "ROLE_SECURITY_ADMIN"
            ? "Global security metrics"
            : "Showing metrics for issues assigned to you"}
        </p>
      </div>
    </div>
  );
};

// Only ADMIN and SECURITY_ADMIN can access reports
export default withAuth(ReportsPage, {
  allowedRoles: ["ROLE_ADMIN", "ROLE_SECURITY_ADMIN"],
  redirectTo: "/dashboard",
});
