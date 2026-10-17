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
      CRITICAL: "bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300",
      HIGH: "bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-300",
      MEDIUM: "bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-300",
      LOW: "bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300",
    };
    return (
      badges[severity as keyof typeof badges] || "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200"
    );
  };

  const getStatusBadge = (status: string) => {
    const badges = {
      OPEN: "bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300",
      IN_PROGRESS: "bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-300",
      RESOLVED: "bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-300",
      CLOSED: "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200",
    };
    return badges[status as keyof typeof badges] || "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200";
  };

  if (issues.length === 0) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 dark:text-gray-400">No issues found</p>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-900 shadow overflow-hidden sm:rounded-md">
      <ul className="divide-y divide-gray-200 dark:divide-gray-700">
        {issues.map((issue) => (
          <li key={issue.id}>
            <Link href={`/issues/${issue.id}`}>
              <div className="block hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800 cursor-pointer">
                <div className="px-4 py-4 sm:px-6">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                      <p className="text-sm font-medium text-blue-600 dark:text-blue-400 truncate">
                        #{issue.id}
                      </p>
                      <p className="text-lg font-medium text-gray-900 dark:text-white">
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
                        <p className="flex items-center text-sm text-gray-500 dark:text-gray-400">
                          <span className="mr-1">📊</span>
                          CVSS: {issue.cvssScore.toFixed(1)}
                        </p>
                      )}
                      {issue.cweId && (
                        <p className="mt-2 flex items-center text-sm text-gray-500 dark:text-gray-400 sm:mt-0 sm:ml-6">
                          <span className="mr-1">🔒</span>
                          {issue.cweId}
                        </p>
                      )}
                    </div>
                    <div className="mt-2 flex items-center text-sm text-gray-500 dark:text-gray-400 sm:mt-0">
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
