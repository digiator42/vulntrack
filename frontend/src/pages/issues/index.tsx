// pages/issues/index.tsx
import React, { useState, useEffect } from "react";
import IssueList from "@/components/issues/IssueList";
import IssueSearch from "@/components/issues/IssueSearch";
import { getIssues, searchIssues, getMyIssues } from "../../services/api";
import { Issue } from "../../types";
import { withAuth } from "@/components/common/ProtectedRoute";
import { useAuth } from "@/hooks/useAuth";
import { TableShimmer } from "@/components/common/Shimmer";

const IssuesPage: React.FC = () => {
  const { user } = useAuth();
  const [issues, setIssues] = useState<Issue[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);
  const isDeveloper = user?.role === "ROLE_DEVELOPER";

  const loadIssues = async (page: number, searchCriteria?: any[]) => {
    setLoading(true);
    try {
      let response;

      // For developers, only fetch assigned issues
      if (isDeveloper) {
        if (searchCriteria && searchCriteria.length > 0) {
          // Add assigned to me filter
          const criteriaWithAssignee = [
            ...searchCriteria,
            { key: "assignedTo", operation: ":", value: user?.id.toString() },
          ];
          response = await searchIssues(criteriaWithAssignee, page, pageSize);
        } else {
          response = await getMyIssues(page, pageSize);
        }
      } else {
        // Admins and Security Admins see all issues
        if (searchCriteria && searchCriteria.length > 0) {
          response = await searchIssues(searchCriteria, page, pageSize);
        } else {
          response = await getIssues(page, pageSize, "createdAt,desc");
        }
      }

      setIssues(response.content);
      setTotalPages(response.totalPages);
      setCurrentPage(page);
    } catch (error) {
      console.error("Failed to load issues:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadIssues(0);
  }, []);

  const handleSearch = (criteria: any[]) => {
    loadIssues(0, criteria);
  };

  const handlePageChange = (newPage: number) => {
    loadIssues(newPage);
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
          {isDeveloper ? "My Assigned Issues" : "Vulnerability Issues"}
        </h1>
        <p className="text-gray-600 dark:text-gray-400 mt-1">
          {isDeveloper
            ? "Track and resolve vulnerabilities assigned to you"
            : "Track and manage security vulnerabilities"}
        </p>
      </div>

      {/* Hide search for developers if it shows all issues */}
      {!isDeveloper && <IssueSearch onSearch={handleSearch} />}

      {loading ? (
        <TableShimmer />
      ) : (
        <>
          {issues.length === 0 && isDeveloper && (
            <div className="text-center py-12 bg-white dark:bg-gray-900 rounded-lg shadow">
              <p className="text-gray-500 dark:text-gray-400">No issues assigned to you yet.</p>
            </div>
          )}

          <IssueList issues={issues} showAssignee={isDeveloper} />

          {totalPages > 1 && (
            <div className="mt-6 flex justify-center space-x-2">
              <button
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Previous
              </button>
              <span className="px-3 py-1">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage === totalPages - 1}
                className="px-3 py-1 border rounded disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

// Developers can view their assigned issues
// Admins and Security Admins can view all issues
export default withAuth(IssuesPage);
