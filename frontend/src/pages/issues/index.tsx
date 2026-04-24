// pages/issues/index.tsx
import React, { useState, useEffect } from 'react';
import IssueList from '@/components/issues/IssueList';
import IssueSearch from '@/components/issues/IssueSearch';
import { getIssues, searchIssues } from '../../services/api';
import { Issue } from '../../types';
import { withAuth } from '@/components/common/ProtectedRoute';
import LoadingSpinner from '@/components/common/LoadingSpinner';

const IssuesPage: React.FC = () => {
  const [issues, setIssues] = useState<Issue[]>([]);
  const [loading, setLoading] = useState(true);
  const [totalPages, setTotalPages] = useState(0);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(10);

  const loadIssues = async (page: number, searchCriteria?: any[]) => {
    setLoading(true);
    try {
      let response;
      if (searchCriteria && searchCriteria.length > 0) {
        response = await searchIssues(searchCriteria, page, pageSize);
      } else {
        response = await getIssues(page, pageSize, 'createdAt,desc');
      }
      setIssues(response.content);
      setTotalPages(response.totalPages);
      setCurrentPage(page);
    } catch (error) {
      console.error('Failed to load issues:', error);
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
          <h1 className="text-2xl font-bold text-gray-900">Vulnerability Issues</h1>
          <p className="text-gray-600 mt-1">Track and manage security vulnerabilities</p>
        </div>

        <IssueSearch onSearch={handleSearch} />

        {loading ? (
          <div className="flex justify-center py-12">
            <LoadingSpinner />
          </div>
        ) : (
          <>
            <IssueList issues={issues} />
            
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

export default withAuth(IssuesPage);