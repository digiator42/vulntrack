// components/issues/IssueDetail.tsx
import React, { useState } from 'react';
import { Issue } from '@/types';
import { format } from 'date-fns';
import Link from 'next/link';

interface IssueDetailProps {
  issue: Issue;
  onUpdate?: (id: number, data: any) => void;
  onDelete?: (id: number) => void;
}

const IssueDetail: React.FC<IssueDetailProps> = ({ issue, onUpdate, onDelete }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [status, setStatus] = useState(issue.status);

  const getSeverityColor = (severity: string) => {
    const colors = {
      CRITICAL: 'text-red-700 dark:text-red-300 bg-red-100 dark:bg-red-900/40',
      HIGH: 'text-orange-700 dark:text-orange-300 bg-orange-100 dark:bg-orange-900/40',
      MEDIUM: 'text-yellow-700 dark:text-yellow-300 bg-yellow-100 dark:bg-yellow-900/40',
      LOW: 'text-blue-700 dark:text-blue-300 bg-blue-100 dark:bg-blue-900/40'
    };
    return colors[severity as keyof typeof colors] || 'text-gray-700 bg-gray-100';
  };

  const handleStatusUpdate = () => {
    if (onUpdate) {
      onUpdate(issue.id, { status });
      setIsEditing(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg">
      <div className="p-6 border-b border-gray-200">
        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">{issue.title}</h1>
            <div className="flex items-center space-x-4 mt-2">
              <span className={`px-2 py-1 text-xs rounded-full ${getSeverityColor(issue.severity)}`}>
                {issue.severity}
              </span>
              {issue.cvssScore && (
                <span className="text-sm text-gray-600">
                  CVSS Score: <strong>{issue.cvssScore.toFixed(1)}</strong>
                </span>
              )}
              {issue.cweId && (
                <Link href={`/cwe/${issue.cweId}`} className="text-sm text-blue-600 dark:text-blue-400 hover:underline">
                  {issue.cweId}
                </Link>
              )}
            </div>
          </div>
          <div className="flex space-x-2">
            <button
              onClick={() => setIsEditing(!isEditing)}
              className="px-3 py-1 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
            >
              Edit
            </button>
            {onDelete && (
              <button
                onClick={() => onDelete(issue.id)}
                className="px-3 py-1 text-sm text-red-600 dark:text-red-400 border border-red-300 dark:border-red-700 rounded-md hover:bg-red-50 dark:bg-red-900/25 dark:hover:bg-red-900/40"
              >
                Delete
              </button>
            )}
          </div>
        </div>
      </div>
      
      <div className="p-6">
        <div className="grid grid-cols-2 gap-6 mb-6">
          <div>
            <h3 className="text-sm font-medium text-gray-500">Status</h3>
            {isEditing ? (
              <div className="mt-1 flex space-x-2">
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as any)}
                  className="flex-1 px-3 py-1 border border-gray-300 rounded-md"
                >
                  <option value="OPEN">Open</option>
                  <option value="IN_PROGRESS">In Progress</option>
                  <option value="RESOLVED">Resolved</option>
                  <option value="CLOSED">Closed</option>
                </select>
                <button
                  onClick={handleStatusUpdate}
                  className="px-3 py-1 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  Save
                </button>
              </div>
            ) : (
              <p className="mt-1 text-gray-900">{issue.status.replace('_', ' ')}</p>
            )}
          </div>
          
          <div>
            <h3 className="text-sm font-medium text-gray-500">Created By</h3>
            <p className="mt-1 text-gray-900">{issue.owner?.username || 'Unknown'}</p>
          </div>
          
          <div>
            <h3 className="text-sm font-medium text-gray-500">Created At</h3>
            <p className="mt-1 text-gray-900">{format(new Date(issue.createdAt), 'PPP')}</p>
          </div>
          
          <div>
            <h3 className="text-sm font-medium text-gray-500">Last Updated</h3>
            <p className="mt-1 text-gray-900">{format(new Date(issue.updatedAt), 'PPP')}</p>
          </div>
        </div>
        
        <div className="mb-6">
          <h3 className="text-sm font-medium text-gray-500 mb-2">Description</h3>
          <div className="prose max-w-none">
            <p className="text-gray-700 whitespace-pre-wrap">{issue.description}</p>
          </div>
        </div>
        
        {issue.cvssVector && (
          <div className="mt-4 p-3 bg-gray-50 rounded-md">
            <h3 className="text-sm font-medium text-gray-500 mb-1">CVSS Vector</h3>
            <code className="text-xs font-mono text-gray-700 break-all">
              {issue.cvssVector}
            </code>
          </div>
        )}
      </div>
    </div>
  );
};

export default IssueDetail;