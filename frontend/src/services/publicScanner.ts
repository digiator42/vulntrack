// src/services/publicScanner.ts
import { ParsedDependency, CveVulnerability } from '@/types/dependency';

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

export const publicSearchCpe = async (product: string): Promise<any> => {
  const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/public/cpe/search?product=${encodeURIComponent(product)}`);
  if (!response.ok) {
    if (response.status === 404) return null;
    throw new Error('Failed to search CPE');
  }
  return response.json();
};