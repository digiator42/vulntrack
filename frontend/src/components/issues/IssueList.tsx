// components/issues/IssueList.tsx
import React from "react";
import Link from "next/link";
import { Issue } from "@/types";
import { formatDistanceToNow } from "date-fns";

interface IssueListProps {
  issues: Issue[];
  showAssignee?: boolean;
}

const IssueList: React.FC<IssueListProps> = ({
  issues,
  showAssignee = false,
}) => {
  console.log("Rendering IssueList with issues:", issues);
  const getSeverityBadge = (severity: string) => {
    const badges = {
      CRITICAL: "bg-red-100 text-red-800",
      HIGH: "bg-orange-100 text-orange-800",
      MEDIUM: "bg-yellow-100 text-yellow-800",
      LOW: "bg-blue-100 text-blue-800",
    };
    return (
      badges[severity as keyof typeof badges] || "bg-gray-100 text-gray-800"
    );
  };

  const getStatusBadge = (status: string) => {
    const badges = {
      OPEN: "bg-red-100 text-red-800",
      IN_PROGRESS: "bg-yellow-100 text-yellow-800",
      RESOLVED: "bg-green-100 text-green-800",
      CLOSED: "bg-gray-100 text-gray-800",
    };
    return badges[status as keyof typeof badges] || "bg-gray-100 text-gray-800";
  };

  if (issues.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">No issues found</p>
      </div>
    );
  }

  return (
    <div className="bg-white shadow overflow-hidden sm:rounded-md">
      <ul className="divide-y divide-gray-200">
        {issues.map((issue) => (
          <li key={issue.id}>
            <Link href={`/issues/${issue.id}`}>
              <div className="block hover:bg-gray-50 cursor-pointer">
                <div className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <p className="text-sm font-medium text-blue-600 truncate">
                        #{issue.id}
                      </p>
                      <p className="text-lg font-medium text-gray-900">
                        {issue.title}
                      </p>
                    </div>
                    <div className="flex space-x-2">
                      <span
                        className={`px-2 py-1 text-xs rounded-full ${getSeverityBadge(issue.severity)}`}
                      >
                        {issue.severity}
                      </span>
                      <span
                        className={`px-2 py-1 text-xs rounded-full ${getStatusBadge(issue.status)}`}
                      >
                        {issue.status.replace("_", " ")}
                      </span>
                    </div>
                  </div>
                  <div className="mt-2 sm:flex sm:justify-between">
                    <div className="sm:flex">
                      {issue.cvssScore && (
                        <p className="flex items-center text-sm text-gray-500">
                          <span className="mr-1">📊</span>
                          CVSS: {issue.cvssScore.toFixed(1)}
                        </p>
                      )}
                      {issue.cweId && (
                        <p className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6">
                          <span className="mr-1">🔒</span>
                          {issue.cweId}
                        </p>
                      )}
                    </div>
                    <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                      {showAssignee && issue.assignedTo && (
                        <span className="mr-3">
                          👤 Assigned to: {issue.assignedTo.username}
                        </span>
                      )}
                      <span className="mr-1">🕒</span>
                      {issue.createdAt
                        ? `Created ${formatDistanceToNow(new Date(issue.createdAt))} ago`
                        : "Created date unavailable"}
                    </div>
                  </div>
                </div>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default IssueList;
