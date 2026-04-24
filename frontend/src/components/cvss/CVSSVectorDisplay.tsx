// components/cvss/CVSSVectorDisplay.tsx
import React, { useState } from 'react';

interface CVSSVectorDisplayProps {
  vector: string;
}

const CVSSVectorDisplay: React.FC<CVSSVectorDisplayProps> = ({ vector }) => {
  const [copied, setCopied] = useState(false);

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(vector);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="space-y-2">
      <label className="block text-sm font-medium text-gray-700">CVSS Vector String</label>
      <div className="flex items-center gap-2">
        <code className="flex-1 p-2 bg-gray-100 rounded text-sm font-mono break-all">
          {vector}
        </code>
        <button
          onClick={copyToClipboard}
          className="px-3 py-1.5 text-sm bg-gray-200 hover:bg-gray-300 rounded-md transition-colors"
        >
          {copied ? '✓ Copied!' : 'Copy'}
        </button>
      </div>
    </div>
  );
};

export default CVSSVectorDisplay;