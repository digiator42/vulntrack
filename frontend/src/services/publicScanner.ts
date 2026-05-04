// src/services/publicScanner.ts
import { ParsedDependency, CveVulnerability, PackageCpeMapping } from '@/types/dependency';

export interface PublicScanResult {
  manifestFileName: string;
  totalDependencies: number;
  vulnerableDependencies: number;
  vulnerabilities: Array<{
    dependency: ParsedDependency;
    cves: CveVulnerability[];
  }>;
}

export const publicParseManifest = async (file: File): Promise<ParsedDependency[]> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/public/scan/parse`, {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) throw new Error('Failed to parse manifest');
  return response.json();
};

export const publicScanDependencies = async (file: File): Promise<PublicScanResult> => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/public/scan/scan`, {
    method: 'POST',
    body: formData,
  });
  if (!response.ok) throw new Error('Failed to scan dependencies');
  return response.json();
};


// src/services/publicScanner.ts

export interface CpeSearchResponse {
  type: 'results' | 'suggestions';
  query?: string;
  results?: PackageCpeMapping[];
  suggestions?: PackageCpeMapping[];
}

export const publicSearchCpe = async (product: string): Promise<CpeSearchResponse> => {
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/public/cpe/search?product=${encodeURIComponent(product)}`);
  if (!response.ok) {
    if (response.status === 404) {
      return { type: 'results', results: [] };
    }
    throw new Error('Failed to search CPE');
  }
  const data = await response.json();
  
  // Handle the new response format with type: "results"
  if (data.type === 'results') {
    return {
      type: 'results',
      results: data.results || []
    };
  }
  
  // Handle suggestions response
  if (data.type === 'suggestions') {
    return {
      type: 'suggestions',
      query: data.query,
      suggestions: data.suggestions || []
    };
  }
  
  // Fallback for array response
  const results = Array.isArray(data) ? data : [data];
  return {
    type: 'results',
    results: results
  };
};