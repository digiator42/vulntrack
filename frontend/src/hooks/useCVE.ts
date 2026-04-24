// hooks/useCVE.ts
import { useState, useCallback } from 'react';
import { searchCVE, getCVEDetails, CVEData } from '@/services/cve';

export const useCVE = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [suggestions, setSuggestions] = useState<CVEData[]>([]);
  const [selectedCVE, setSelectedCVE] = useState<CVEData | null>(null);

  const search = useCallback(async (query: string) => {
    if (!query || query.length < 4) {
      setSuggestions([]);
      return;
    }
    
    setLoading(true);
    setError(null);
    
    try {
      const results = await searchCVE(query);
      setSuggestions(results);
    } catch (err) {
      setError('Failed to search CVE');
      setSuggestions([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchDetails = useCallback(async (cveId: string) => {
    setLoading(true);
    setError(null);
    
    try {
      const details = await getCVEDetails(cveId);
      setSelectedCVE(details);
      return details;
    } catch (err) {
      setError('CVE not found');
      setSelectedCVE(null);
      return null;
    } finally {
      setLoading(false);
    }
  }, []);

  const clear = useCallback(() => {
    setSuggestions([]);
    setSelectedCVE(null);
    setError(null);
  }, []);

  return {
    loading,
    error,
    suggestions,
    selectedCVE,
    search,
    fetchDetails,
    clear
  };
};