// components/cvss/CVSSMetricGroup.tsx
import React from 'react';

interface Option {
  value: string;
  label: string;
  description?: string;
}

interface CVSSMetricGroupProps {
  label: string;
  description?: string;
  options: Option[];
  value: string;
  onChange: (value: string) => void;
}

const CVSSMetricGroup: React.FC<CVSSMetricGroupProps> = ({
  label,
  description,
  options,
  value,
  onChange
}) => {
  return (
    <div className="space-y-2">
      <div>
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
        {description && <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{description}</p>}
      </div>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            onClick={() => onChange(option.value)}
            className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
              value === option.value
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 hover:bg-gray-200'
            }`}
            title={option.description}
          >
            {option.label}
          </button>
        ))}
      </div>
    </div>
  );
};

export default CVSSMetricGroup;