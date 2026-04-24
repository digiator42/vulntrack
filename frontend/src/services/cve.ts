// services/cve.ts
import axios from 'axios';

// Using NVD API (National Vulnerability Database)
const NVD_API_URL = 'https://services.nvd.nist.gov/rest/json/cves/2.0';

export interface CVEData {
  id: string;
  description: string;
  cvssScore?: number;
  cvssVector?: string;
  weaknesses: Array<{ cweId: string; name: string }>;
}

export const searchCVE = async (cveId: string): Promise<CVEData[]> => {
  try {
    const response = await axios.get(NVD_API_URL, {
      params: {
        cveId: cveId,
        resultsPerPage: 5
      }
    });
    
    return response.data.vulnerabilities?.map((vuln: any) => ({
      id: vuln.cve.id,
      description: vuln.cve.descriptions.find((d: any) => d.lang === 'en')?.value || '',
      cvssScore: vuln.cve.metrics?.cvssMetricV31?.[0]?.cvssData?.baseScore,
      cvssVector: vuln.cve.metrics?.cvssMetricV31?.[0]?.cvssData?.vectorString,
      weaknesses: vuln.cve.weaknesses?.map((w: any) => ({
        cweId: w.description?.[0]?.value?.split(':')[0] || '',
        name: w.description?.[0]?.value || ''
      })) || []
    })) || [];
  } catch (error) {
    console.error('Error fetching CVE data:', error);
    return [];
  }
};

export const getCVEDetails = async (cveId: string): Promise<CVEData> => {
  const results = await searchCVE(cveId);
  if (results.length === 0) {
    throw new Error('CVE not found');
  }
  return results[0];
};