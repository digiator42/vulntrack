// types/index.ts
export interface User {
  id: number;
  username: string;
  email: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export interface Issue {
  id: number;
  title: string;
  description: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  cvssScore: number;
  cvssVector?: string;
  cweId?: string;
  createdAt: string;
  updatedAt: string;
  owner: User;
}

export interface CVSSMetrics {
  attackVector: 'N' | 'A' | 'L' | 'P';
  attackComplexity: 'L' | 'H';
  privilegesRequired: 'N' | 'L' | 'H';
  userInteraction: 'N' | 'R';
  scope: 'U' | 'C';
  confidentiality: 'N' | 'L' | 'H';
  integrity: 'N' | 'L' | 'H';
  availability: 'N' | 'L' | 'H';
}

export interface CVEEntry {
  id: string;
  description: string;
  cvssScore?: number;
  cvssVector?: string;
  weaknesses: Array<{ cweId: string; name: string }>;
}

export interface PageableResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export enum IssueStatus {
    OPEN = 'OPEN',
    IN_PROGRESS = 'IN_PROGRESS',
    RESOLVED = 'RESOLVED',
    CLOSED = 'CLOSED',
}

export interface Vulnerability {
    id: number;
    title: string;
    description: string;
    severity: number; // 1-4 (will expand to CVSS later)
    status: IssueStatus;
    ownerId: number;
    createdAt?: string;
    updatedAt?: string;
    cveId?: string;
    cweId?: number;
    cvssScore?: number;
    cvssVector?: string;
    owaspCategory?: string;
}

export interface CreateVulnerabilityRequest {
    title: string;
    description: string;
    severity: number;
}

export interface UpdateVulnerabilityRequest {
    title: string;
    description: string;
}

export interface PatchVulnerabilityRequest {
    title?: string;
    description?: string;
    status?: IssueStatus;
}

export interface VulnerabilityPage {
    content: Vulnerability[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
}