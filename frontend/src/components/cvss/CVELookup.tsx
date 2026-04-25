// components/issues/CVELookup.tsx
import React, { useState, useEffect } from 'react';
import { searchCVE, getCVEDetails } from '../../services/cve';
import { CVEEntry } from '../../types';
import LoadingSpinner from '@/components/common/LoadingSpinner';

interface CVELookupProps {
  onCVESelect: (cveData: CVEEntry) => void;
}

const CVELookup: React.FC<CVELookupProps> = ({ onCVESelect }) => {
  const [cveId, setCveId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [suggestions, setSuggestions] = useState<CVEEntry[]>([]);
  const [selectedCVE, setSelectedCVE] = useState<CVEEntry | null>(null);

  // Debounced search for CVE suggestions
  useEffect(() => {
    const timer = setTimeout(() => {
      if (cveId.length >= 4 && cveId.toUpperCase().startsWith('CVE-')) {
        fetchSuggestions();
      } else {
        setSuggestions([]);
      }
    }, 500);

    return () => clearTimeout(timer);
  }, [cveId]);

  const fetchSuggestions = async () => {
    try {
      setLoading(true);
      // This would call your backend or a CVE API service
      const results = await searchCVE(cveId);
      setSuggestions(results);
    } catch (err) {
      console.error('Failed to fetch CVE suggestions:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchCVEDetails = async () => {
    if (!cveId.trim()) return;
    
    setLoading(true);
    setError(null);
    
    try {
      const cveData = await getCVEDetails(cveId.toUpperCase());
      setSelectedCVE(cveData);
      console.log('Fetched CVE details:', cveData);
      onCVESelect(cveData);
    } catch (err) {
      setError('CVE not found. Please check the ID and try again.');
      setSelectedCVE(null);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      fetchCVEDetails();
    }
  };

  const selectSuggestion = (cve: CVEEntry) => {
    setCveId(cve.id);
    setSuggestions([]);
    setSelectedCVE(cve);
    onCVESelect(cve);
  };

  return (
    <div className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          CVE ID (Optional)
        </label>
        <div className="relative">
          <input
            type="text"
            value={cveId}
            onChange={(e) => setCveId(e.target.value.toUpperCase())}
            onKeyPress={handleKeyPress}
            placeholder="e.g., CVE-2024-12345"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={fetchCVEDetails}
            disabled={loading}
            className="absolute right-2 top-1/2 transform -translate-y-1/2 px-3 py-1 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? <LoadingSpinner size="small" /> : 'Lookup'}
          </button>
          
          {/* Suggestions dropdown */}
          {suggestions.length > 0 && (
            <div className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-60 overflow-auto">
              {suggestions.map((cve) => (
                <button
                  key={cve.id}
                  onClick={() => selectSuggestion(cve)}
                  className="w-full text-left px-4 py-2 hover:bg-gray-50 focus:outline-none focus:bg-gray-50"
                >
                  <div className="font-medium text-sm">{cve.id}</div>
                  <div className="text-xs text-gray-500 truncate">{cve.description}</div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Display fetched CVE details */}
      {selectedCVE && (
        <div className="p-4 bg-green-50 border border-green-200 rounded-md">
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <h4 className="font-medium text-green-900">{selectedCVE.id}</h4>
              <p className="text-sm text-green-700 mt-1">{selectedCVE.description}</p>
              {selectedCVE.cvssScore && (
                <div className="mt-2 flex items-center gap-4">
                  <span className="text-sm font-medium">CVSS Score:</span>
                  <span className={`px-2 py-1 rounded text-xs font-semibold ${
                    selectedCVE.cvssScore >= 7 ? 'bg-red-100 text-red-700' :
                    selectedCVE.cvssScore >= 4 ? 'bg-yellow-100 text-yellow-700' :
                    'bg-green-100 text-green-700'
                  }`}>
                    {selectedCVE.cvssScore}
                  </span>
                </div>
              )}
              {selectedCVE.weaknesses && selectedCVE.weaknesses.length > 0 && (
                <div className="mt-2">
                  <span className="text-sm font-medium">CWEs:</span>
                  <div className="flex gap-2 mt-1">
                    {selectedCVE.weaknesses.map((cwe) => (
                      <span key={cwe.cweId} className="px-2 py-0.5 bg-gray-100 text-gray-700 text-xs rounded">
                        {cwe.cweId}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
            <button
              onClick={() => setSelectedCVE(null)}
              className="text-green-600 hover:text-green-800"
            >
              ×
            </button>
          </div>
        </div>
      )}

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-md text-red-700 text-sm">
          {error}
        </div>
      )}
    </div>
  );
};

export default CVELookup;