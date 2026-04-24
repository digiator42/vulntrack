// hooks/useCVSS.ts
import { useState, useCallback } from 'react';

interface CVSSMetrics {
  AV: 'N' | 'A' | 'L' | 'P';
  AC: 'L' | 'H';
  PR: 'N' | 'L' | 'H';
  UI: 'N' | 'R';
  S: 'U' | 'C';
  C: 'N' | 'L' | 'H';
  I: 'N' | 'L' | 'H';
  A: 'N' | 'L' | 'H';
}

export const useCVSS = (initialVector?: string) => {
  const [metrics, setMetrics] = useState({
    AV: 'N',
    AC: 'L',
    PR: 'N',
    UI: 'N',
    S: 'U',
    C: 'N',
    I: 'N',
    A: 'N'
  });
  const [score, setScore] = useState(0);
  const [vector, setVector] = useState('');

  const calculateScore = useCallback((currentMetrics: CVSSMetrics) => {
    // CVSS v3.1 Metric Weights
    const AV = { N: 0.85, A: 0.62, L: 0.55, P: 0.2 }[currentMetrics.AV];
    const AC = { L: 0.77, H: 0.44 }[currentMetrics.AC];
    const PR = {
      N: 0.85,
      L: currentMetrics.S === 'U' ? 0.62 : 0.68,
      H: currentMetrics.S === 'U' ? 0.27 : 0.5
    }[currentMetrics.PR];
    const UI = { N: 0.85, R: 0.62 }[currentMetrics.UI];
    const C = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.C];
    const I = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.I];
    const A = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.A];

    // 1. Impact Sub-Score
    const ISS = 1 - ((1 - C) * (1 - I) * (1 - A));
    
    const Impact = currentMetrics.S === 'U' 
      ? 6.42 * ISS 
      : 7.52 * (ISS - 0.029) - 3.25 * Math.pow(ISS - 0.02, 15);

    const Exploitability = 8.22 * AV * AC * PR * UI;

    let baseScore = 0;
    if (Impact <= 0) {
      baseScore = 0;
    } else {
      if (currentMetrics.S === 'U') {
        baseScore = Math.min(Impact + Exploitability, 10);
      } else {
        baseScore = Math.min(1.08 * (Impact + Exploitability), 10);
      }
    }

    return Math.ceil(baseScore * 10) / 10;
  }, []);

  const updateMetric = useCallback((key: keyof CVSSMetrics, value: string) => {
    setMetrics(prev => {
      const ms: CVSSMetrics = { ...prev, [key]: value };
      const newScore = calculateScore(ms);
      setScore(newScore);
      
      const newVector = `CVSS:3.1/AV:${ms.AV}/AC:${ms.AC}/PR:${ms.PR}/UI:${ms.UI}/S:${ms.S}/C:${ms.C}/I:${ms.I}/A:${ms.A}`;
      setVector(newVector);
      
      return ms;
    });
  }, [calculateScore]);

  return { metrics, score, vector, updateMetric };
};