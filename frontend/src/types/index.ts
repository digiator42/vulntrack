// types/index.ts
export interface User {
  id: number;
  username: string;
  email: string;
  role: 'ROLE_ADMIN' | 'ROLE_SECURITY_ADMIN' | 'ROLE_DEVELOPER';
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
  assignedTo?: User;
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
