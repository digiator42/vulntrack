// src/types/dependency.ts
export interface ParsedDependency {
  name: string;
  version: string;
  ecosystem: 'MAVEN' | 'NPM' | 'PYPI' | 'NUGET' | 'GRADLE';
  manifestFile: string;
}

export interface CveVulnerability {
  id: string;
  description: string;
  cvssScore: number;
  severity: string;
  publishedDate: string;
  vectorString: string;
  versionStartIncluding?: string;
  versionStartExcluding?: string;
  versionEndIncluding?: string;
  versionEndExcluding?: string;
}

export interface VulnerableDependency {
  dependency: ParsedDependency;
  cves: CveVulnerability[];
  createdIssueIds: number[];
}

export interface DependencyScanResult {
  manifestFileName: string;
  totalDependencies: number;
  vulnerableDependencies: number;
  vulnerabilities: VulnerableDependency[];
}

export interface PackageCpeMapping {
  id: number;
  product: string;
  cpeName: string;
  vendor: string;
  notes?: string;
  createdAt: string;
  usageCount: number;
  enabled: boolean;
  ecosystem: 'MAVEN' | 'NPM' | 'PYPI' | 'NUGET' | 'GRADLE';
  rawCpeName: string; // The original CPE name from NVD
}