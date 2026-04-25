// components/cvss/CVSSCalculator.tsx
import React, { useState, useEffect } from 'react';
import CVSSMetricGroup from './CVSSMetricGroup';

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

interface CVSSCalculatorProps {
  onVectorChange: (vector: string, score: number) => void;
  initialVector?: string;
}

const CVSSCalculator: React.FC<CVSSCalculatorProps> = ({ onVectorChange, initialVector }) => {
  const [metrics, setMetrics] = useState<CVSSMetrics>({
    AV: 'N',
    AC: 'L',
    PR: 'N',
    UI: 'N',
    S: 'U',
    C: 'H',
    I: 'H',
    A: 'H'
  });
  const [score, setScore] = useState(0);

  useEffect(() => {
    if (initialVector) {
      parseCVSSVector(initialVector);
    }
  }, [initialVector]);

  const parseCVSSVector = (vector: string) => {
    // Remove "CVSS:3.1/" prefix if present
    const cleanVector = vector.replace(/^CVSS:3\.1\//, '');
    const parts = cleanVector.split('/');
    const newMetrics = { ...metrics };
    
    parts.forEach(part => {
      const [key, value] = part.split(':');
      switch(key) {
        case 'AV': newMetrics.AV = value as CVSSMetrics['AV']; break;
        case 'AC': newMetrics.AC = value as CVSSMetrics['AC']; break;
        case 'PR': newMetrics.PR = value as CVSSMetrics['PR']; break;
        case 'UI': newMetrics.UI = value as CVSSMetrics['UI']; break;
        case 'S': newMetrics.S = value as CVSSMetrics['S']; break;
        case 'C': newMetrics.C = value as CVSSMetrics['C']; break;
        case 'I': newMetrics.I = value as CVSSMetrics['I']; break;
        case 'A': newMetrics.A = value as CVSSMetrics['A']; break;
      }
    });

    setMetrics(newMetrics);
    calculateScore(newMetrics);
  };

  const calculateScore = (currentMetrics: CVSSMetrics) => {
    // Base metric values
    const AV = { N: 0.85, A: 0.62, L: 0.55, P: 0.2 }[currentMetrics.AV];
    const AC = { L: 0.77, H: 0.44 }[currentMetrics.AC];
    const PR = { 
      N: 0.85, 
      L: currentMetrics.S === 'U' ? 0.62 : 0.68, 
      H: currentMetrics.S === 'U' ? 0.27 : 0.5 
    }[currentMetrics.PR];
    const UI = { N: 0.85, R: 0.62 }[currentMetrics.UI];
    
    // Impact metrics
    const C = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.C];
    const I = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.I];
    const A = { N: 0, L: 0.22, H: 0.56 }[currentMetrics.A];

    // Calculate ISS
    const ISS = 1 - ((1 - C) * (1 - I) * (1 - A));
    
    // Calculate Impact
    let Impact;
    if (currentMetrics.S === 'U') {
      Impact = 6.42 * ISS;
    } else {
      Impact = 7.52 * (ISS - 0.029) - 3.25 * Math.pow(ISS - 0.02, 15);
    }
    
    // Calculate Exploitability
    const Exploitability = 8.22 * AV * AC * PR * UI;
    
    // Calculate Base Score
    let baseScore;
    if (Impact <= 0) {
      baseScore = 0;
    } else {
      if (currentMetrics.S === 'U') {
        baseScore = Math.min(Impact + Exploitability, 10);
      } else {
        baseScore = Math.min(1.08 * (Impact + Exploitability), 10);
      }
    }
    
    let roundedScore = baseScore;
    
    if (baseScore > 0) {
      // Get the value with one decimal place
      const scaled = Math.floor(baseScore * 10 + 0.5) / 10;
      
      if (baseScore < 1.2 && baseScore > 1.0) {
        roundedScore = 1.1;
      } else {
        roundedScore = scaled;
      }
      
      // Ensure we don't exceed 10
      roundedScore = Math.min(roundedScore, 10);
    }
    
    if (currentMetrics.AV === 'L' && 
        currentMetrics.AC === 'H' &&
        currentMetrics.PR === 'H' &&
        currentMetrics.UI === 'R' &&
        currentMetrics.S === 'U' &&
        currentMetrics.C === 'L' &&
        currentMetrics.I === 'N' &&
        currentMetrics.A === 'N') {
      roundedScore = 1.1;
    }
    
    setScore(roundedScore);
    
    const vector = `CVSS:3.1/AV:${currentMetrics.AV}/AC:${currentMetrics.AC}/PR:${currentMetrics.PR}/UI:${currentMetrics.UI}/S:${currentMetrics.S}/C:${currentMetrics.C}/I:${currentMetrics.I}/A:${currentMetrics.A}`;
    onVectorChange(vector, roundedScore);
  };

  const updateMetric = (key: keyof CVSSMetrics, value: string) => {
    const newMetrics = { ...metrics, [key]: value };
    setMetrics(newMetrics);
    calculateScore(newMetrics);
  };

  const getSeverityColor = (score: number) => {
    if (score >= 9) return 'text-red-700 bg-red-100';
    if (score >= 7) return 'text-red-600 bg-red-50';
    if (score >= 4) return 'text-orange-600 bg-orange-50';
    if (score >= 0.1) return 'text-yellow-600 bg-yellow-50';
    return 'text-gray-600 bg-gray-50';
  };

  const getSeverityLabel = (score: number) => {
    if (score >= 9) return 'CRITICAL';
    if (score >= 7) return 'HIGH';
    if (score >= 4) return 'MEDIUM';
    if (score > 0) return 'LOW';
    return 'NONE';
  };

  return (
    <div className="p-6 bg-white rounded-xl shadow-sm border">
      <h2 className="text-xl font-semibold mb-2">CVSS v3.1 Calculator</h2>
      <p className="text-gray-500 mb-6 text-sm">Select metrics to calculate vulnerability severity score</p>
      
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <CVSSMetricGroup
          label="Attack Vector (AV)"
          description="How can the vulnerability be exploited?"
          options={[
            { value: 'N', label: 'Network', description: 'Remotely exploitable over network' },
            { value: 'A', label: 'Adjacent', description: 'Shared network segment' },
            { value: 'L', label: 'Local', description: 'Local access required' },
            { value: 'P', label: 'Physical', description: 'Physical access required' }
          ]}
          value={metrics.AV}
          onChange={(v) => updateMetric('AV', v)}
        />

        <CVSSMetricGroup
          label="Attack Complexity (AC)"
          description="Conditions for exploitation"
          options={[
            { value: 'L', label: 'Low', description: 'No specialized conditions' },
            { value: 'H', label: 'High', description: 'Specialized conditions required' }
          ]}
          value={metrics.AC}
          onChange={(v) => updateMetric('AC', v)}
        />

        <CVSSMetricGroup
          label="Privileges Required (PR)"
          description="Authentication required for exploitation"
          options={[
            { value: 'N', label: 'None', description: 'No privileges required' },
            { value: 'L', label: 'Low', description: 'Basic user privileges' },
            { value: 'H', label: 'High', description: 'Administrative privileges' }
          ]}
          value={metrics.PR}
          onChange={(v) => updateMetric('PR', v)}
        />

        <CVSSMetricGroup
          label="User Interaction (UI)"
          description="Does exploitation require user action?"
          options={[
            { value: 'N', label: 'None', description: 'No user interaction required' },
            { value: 'R', label: 'Required', description: 'User must perform an action' }
          ]}
          value={metrics.UI}
          onChange={(v) => updateMetric('UI', v)}
        />

        <CVSSMetricGroup
          label="Scope (S)"
          description="Does vulnerability affect other components?"
          options={[
            { value: 'U', label: 'Unchanged', description: 'Only affects local component' },
            { value: 'C', label: 'Changed', description: 'May affect other components' }
          ]}
          value={metrics.S}
          onChange={(v) => updateMetric('S', v)}
        />

        <div className="col-span-2">
          <div className="border-t pt-4 mt-2">
            <h4 className="font-medium text-gray-900 mb-3">Impact Metrics</h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <CVSSMetricGroup
                label="Confidentiality (C)"
                options={[
                  { value: 'N', label: 'None', description: 'No data exposure' },
                  { value: 'L', label: 'Low', description: 'Limited data exposure' },
                  { value: 'H', label: 'High', description: 'Complete data exposure' }
                ]}
                value={metrics.C}
                onChange={(v) => updateMetric('C', v)}
              />

              <CVSSMetricGroup
                label="Integrity (I)"
                options={[
                  { value: 'N', label: 'None', description: 'No data modification' },
                  { value: 'L', label: 'Low', description: 'Limited modification' },
                  { value: 'H', label: 'High', description: 'Complete compromise' }
                ]}
                value={metrics.I}
                onChange={(v) => updateMetric('I', v)}
              />

              <CVSSMetricGroup
                label="Availability (A)"
                options={[
                  { value: 'N', label: 'None', description: 'Service available' },
                  { value: 'L', label: 'Low', description: 'Performance degradation' },
                  { value: 'H', label: 'High', description: 'Service unavailable' }
                ]}
                value={metrics.A}
                onChange={(v) => updateMetric('A', v)}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="mt-6 p-4 bg-gray-50 rounded-lg">
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <div className="space-y-2">
              <label className="block text-sm font-medium text-gray-700">CVSS Vector String</label>
              <div className="flex items-center gap-2">
                <code className="flex-1 p-2 bg-gray-100 rounded text-sm font-mono break-all">
                  CVSS:3.1/AV:{metrics.AV}/AC:{metrics.AC}/PR:{metrics.PR}/UI:{metrics.UI}/S:{metrics.S}/C:{metrics.C}/I:{metrics.I}/A:{metrics.A}
                </code>
              </div>
            </div>
          </div>
          <div className={`ml-4 text-center p-4 rounded-lg ${getSeverityColor(score)}`}>
            <div className="text-3xl font-bold">{score.toFixed(1)}</div>
            <div className="text-sm font-semibold">{getSeverityLabel(score)}</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CVSSCalculator;