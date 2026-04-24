// hooks/useCVSS.ts
import { useState, useEffect } from 'react';

// Official CVSS v3.1 Metric Weights
const METRIC_WEIGHTS = {
  AV: { N: 0.85, A: 0.62, L: 0.55, P: 0.2 },
  AC: { L: 0.77, H: 0.44 },
  UI: { N: 0.85, R: 0.62 },
  CIA: { N: 0.0, L: 0.22, H: 0.56 },
  PR: {
    U: { N: 0.85, L: 0.62, H: 0.27 }, // Scope: Unchanged
    C: { N: 0.85, L: 0.68, H: 0.50 }, // Scope: Changed
  },
} as const;

export type CVSSMetrics = {
  AV: 'N' | 'A' | 'L' | 'P';
  AC: 'L' | 'H';
  PR: 'N' | 'L' | 'H';
  UI: 'N' | 'R';
  S:  'U' | 'C';
  C:  'N' | 'L' | 'H';
  I:  'N' | 'L' | 'H';
  A:  'N' | 'L' | 'H';
};

export function useCVSS(initialMetrics: Partial<CVSSMetrics> = {}) {
  const [metrics, setMetrics] = useState<CVSSMetrics>({
    AV: 'N', AC: 'L', PR: 'N', UI: 'N', S: 'U',
    C: 'N', I: 'N', A: 'N',
    ...initialMetrics,
  });

  const [score, setScore] = useState<number>(0);
  const [severity, setSeverity] = useState<string>('NONE');
  const [vectorString, setVectorString] = useState<string>('');

  // Recalculate on metric change
  useEffect(() => {
    const calculated = calculateBaseScore(metrics);
    setScore(calculated);
    setSeverity(getSeverity(calculated));
    setVectorString(buildVector(metrics));
  }, [metrics]);

  const updateMetric = <K extends keyof CVSSMetrics>(key: K, value: CVSSMetrics[K]) => {
    setMetrics((prev) => ({ ...prev, [key]: value }));
  };

  return { metrics, score, severity, vectorString, updateMetric };
}

// 🔢 Core CVSS v3.1 Calculation
function calculateBaseScore(m: CVSSMetrics): number {
  const av = METRIC_WEIGHTS.AV[m.AV];
  const ac = METRIC_WEIGHTS.AC[m.AC];
  const ui = METRIC_WEIGHTS.UI[m.UI];
  const c  = METRIC_WEIGHTS.CIA[m.C];
  const i  = METRIC_WEIGHTS.CIA[m.I];
  const a  = METRIC_WEIGHTS.CIA[m.A];
  
  // ⚠️ PR weights depend on Scope
  const pr = m.S === 'U' ? METRIC_WEIGHTS.PR.U[m.PR] : METRIC_WEIGHTS.PR.C[m.PR];

  // 1. Impact Sub-Score (ISS)
  const iss = 1 - (1 - c) * (1 - i) * (1 - a);

  // 2. Impact Score
  let impact: number;
  if (m.S === 'U') {
    impact = 6.42 * iss;
  } else {
    impact = 7.52 * (iss - 0.029) - 3.25 * Math.pow(iss - 0.02, 15);
  }

  // 3. Exploitability Score
  const exploitability = 8.22 * av * ac * pr * ui;

  // 4. Base Score
  let baseScore: number;
  if (impact <= 0) {
    baseScore = 0;
  } else if (m.S === 'U') {
    baseScore = roundUp(Math.min(impact + exploitability, 10));
  } else {
    baseScore = roundUp(Math.min(1.08 * (impact + exploitability), 10));
  }

  return baseScore;
}

// 🔍 CVSS v3.1 Severity Ratings
function getSeverity(score: number): string {
  if (score === 0) return 'NONE';
  if (score < 4.0) return 'LOW';
  if (score < 7.0) return 'MEDIUM';
  if (score < 9.0) return 'HIGH';
  return 'CRITICAL';
}

// 📝 Build Vector String for API/Backend
function buildVector(m: CVSSMetrics): string {
  return `CVSS:3.1/AV:${m.AV}/AC:${m.AC}/PR:${m.PR}/UI:${m.UI}/S:${m.S}/C:${m.C}/I:${m.I}/A:${m.A}`;
}

// ⬆️ CVSS requires rounding UP to 1 decimal place
function roundUp(score: number): number {
  return Math.ceil(score * 10) / 10;
}