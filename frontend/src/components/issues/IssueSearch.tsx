// components/issues/IssueSearch.tsx
import React, { useState } from 'react';

interface SearchCriteria {
  key: string;
  operation: string;
  value: string;
}

interface IssueSearchProps {
  onSearch: (criteria: SearchCriteria[]) => void;
}

const IssueSearch: React.FC<IssueSearchProps> = ({ onSearch }) => {
  const [searchField, setSearchField] = useState('title');
  const [searchValue, setSearchValue] = useState('');
  const [severity, setSeverity] = useState('');
  const [status, setStatus] = useState('');

  const handleSearch = () => {
    const criteria: SearchCriteria[] = [];
    
    if (searchValue) {
      criteria.push({
        key: searchField,
        operation: ':',
        value: searchValue
      });
    }
    
    if (severity) {
      criteria.push({
        key: 'severity',
        operation: ':',
        value: severity
      });
    }
    
    if (status) {
      criteria.push({
        key: 'status',
        operation: ':',
        value: status
      });
    }
    
    onSearch(criteria);
  };

  const handleReset = () => {
    setSearchField('title');
    setSearchValue('');
    setSeverity('');
    setStatus('');
    onSearch([]);
  };

  return (
    <div className="bg-white dark:bg-gray-900 rounded-lg shadow p-6 mb-6">
      <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-4">Search Vulnerabilities</h3>
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Search Field
          </label>
          <select
            value={searchField}
            onChange={(e) => setSearchField(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="title">Title</option>
            <option value="description">Description</option>
            <option value="cweId">CWE ID</option>
          </select>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Search Value
          </label>
          <input
            type="text"
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            placeholder="Enter search term..."
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Severity
          </label>
          <select
            value={severity}
            onChange={(e) => setSeverity(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All</option>
            <option value="CRITICAL">Critical</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </select>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
            Status
          </label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="CLOSED">Closed</option>
          </select>
        </div>
      </div>
      
      <div className="mt-4 flex justify-end space-x-3">
        <button
          onClick={handleReset}
          className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800"
        >
          Reset
        </button>
        <button
          onClick={handleSearch}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
        >
          Search
        </button>
      </div>
    </div>
  );
};

export default IssueSearch;