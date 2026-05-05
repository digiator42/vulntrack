// src/services/cveByCpe.ts
import { CveVulnerability } from '@/types/dependency';

export interface CveResult {
  id: string;
  description: string;
  cvssScore: number;
  severity: string;
  publishedDate: string;
  vectorString: string;
}

export const fetchCvesByCpe = async (cpeName: string): Promise<CveResult[]> => {
  try {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/api/nvd/cves?cpeName=${encodeURIComponent(cpeName)}`,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch CVEs');
    }
    
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('Error fetching CVEs by CPE:', error);
    throw error;
  }
};