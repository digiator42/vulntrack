// src/components/public/CveResults.tsx
import React, { useState } from "react";
import {
  BugAntIcon,
  InformationCircleIcon,
  ArrowPathIcon,
  ShieldCheckIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";
import { CveResult } from "@/services/cveByCpe";
import { formatDistanceToNow } from "date-fns";

interface CveResultsProps {
  cves: CveResult[];
  cpeName: string;
  loading: boolean;
  onClose: () => void;
}

const CveResults: React.FC<CveResultsProps> = ({
  cves,
  cpeName,
  loading,
  onClose,
}) => {
  const [expandedCves, setExpandedCves] = useState<Record<string, boolean>>({});

  const getSeverityColor = (severity: string) => {
    const colors = {
      CRITICAL: "bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-300 border-red-200 dark:border-red-800",
      HIGH: "bg-orange-100 dark:bg-orange-900/40 text-orange-800 dark:text-orange-300 border-orange-200 dark:border-orange-800",
      MEDIUM: "bg-yellow-100 dark:bg-yellow-900/40 text-yellow-800 dark:text-yellow-300 border-yellow-200 dark:border-yellow-800",
      LOW: "bg-blue-100 dark:bg-blue-900/40 text-blue-800 dark:text-blue-300 border-blue-200 dark:border-blue-800",
    };
    return (
      colors[severity as keyof typeof colors] || "bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200"
    );
  };

  const getSeverityBadgeColor = (severity: string) => {
    const colors = {
      CRITICAL: "bg-red-600",
      HIGH: "bg-orange-600",
      MEDIUM: "bg-yellow-600",
      LOW: "bg-blue-600",
    };
    return colors[severity as keyof typeof colors] || "bg-gray-600";
  };

  const toggleCveExpand = (cveId: string) => {
    setExpandedCves((prev) => ({ ...prev, [cveId]: !prev[cveId] }));
  };

  if (loading) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
        <div className="bg-white dark:bg-gray-900 rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[80vh] overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheckIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Loading CVEs...
                </h3>
              </div>
            </div>
          </div>
          <div className="p-8 text-center">
            <ArrowPathIcon className="h-12 w-12 mx-auto text-blue-500 animate-spin mb-4" />
            <p className="text-gray-600 dark:text-gray-400">
              Fetching vulnerabilities for {cpeName}
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[80vh] overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-2">
                <ShieldCheckIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  CVEs for {cpeName}
                </h3>
                <span className="px-2 py-0.5 text-xs rounded-full bg-gray-200 text-gray-700 dark:text-gray-300">
                  {cves.length} {cves.length === 1 ? "CVE" : "CVEs"}
                </span>
              </div>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Known vulnerabilities associated with this CPE
              </p>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 dark:text-gray-400 transition-colors"
            >
              <svg
                className="h-6 w-6"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </button>
          </div>
        </div>

        {/* Results Summary */}
        <div className="px-6 py-3 bg-gray-50 dark:bg-gray-800 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-4 text-sm">
            {cves.length > 0 ? (
              <>
                <span className="text-red-600 dark:text-red-400 font-medium flex items-center gap-1">
                  <BugAntIcon className="h-5 w-5" />
                  {cves.length} vulnerability{cves.length !== 1 ? "ies" : ""}{" "}
                  found
                </span>
                <span className="text-gray-400">|</span>
                <span className="text-gray-600 dark:text-gray-400">
                  Latest:{" "}
                  {formatDistanceToNow(
                    new Date(cves[0]?.publishedDate || new Date()),
                    { addSuffix: true },
                  )}
                </span>
              </>
            ) : (
              <span className="text-green-600 dark:text-green-400 font-medium">
                No known vulnerabilities found
              </span>
            )}
          </div>
        </div>

        {/* CVEs List */}
        <div className="overflow-y-auto max-h-[60vh] p-4 space-y-4">
          {cves.length === 0 ? (
            <div className="text-center py-12">
              <ShieldCheckIcon className="h-16 w-16 mx-auto text-green-500 mb-4" />
              <p className="text-gray-500 dark:text-gray-400 text-lg">
                No CVEs found for this CPE
              </p>
              <p className="text-sm text-gray-400 mt-2">
                Check the NVD Site for more information
              </p>
            </div>
          ) : (
            cves
              .sort(
                (a, b) =>
                  new Date(b.publishedDate).getTime() -
                  new Date(a.publishedDate).getTime(),
              )
              .map((cve) => {
                const isExpanded = expandedCves[cve.id];
                const severityColor = getSeverityColor(cve.severity);

                return (
                  <div
                    key={cve.id}
                    className={`border rounded-lg overflow-hidden ${severityColor}`}
                  >
                    <div
                      className="px-4 py-3 cursor-pointer flex items-center justify-between hover:bg-opacity-50 transition-colors"
                      onClick={() => toggleCveExpand(cve.id)}
                    >
                      <div className="flex items-center gap-3 flex-1">
                        <BugAntIcon className="h-5 w-5" />
                        <span className="font-mono font-medium">{cve.id}</span>
                        <div className="flex items-center gap-2">
                          <span
                            className={`px-2 py-0.5 text-xs rounded-full text-white ${getSeverityBadgeColor(cve.severity)}`}
                          >
                            {cve.severity}
                          </span>
                          <span className="text-sm text-gray-600 dark:text-gray-400">
                            CVSS {cve.cvssScore}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs text-gray-400">
                          {formatDistanceToNow(new Date(cve.publishedDate), {
                            addSuffix: true,
                          })}
                        </span>
                        <InformationCircleIcon className="h-5 w-5 text-gray-400" />
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="px-4 py-3 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700">
                        <div className="mb-3">
                          <h5 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                            Description
                          </h5>
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {cve.description}
                          </p>
                        </div>
                        {cve.vectorString && (
                          <div className="mb-3">
                            <h5 className="text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                              CVSS Vector
                            </h5>
                            <code className="text-xs bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded block break-all">
                              {cve.vectorString}
                            </code>
                          </div>
                        )}
                        <div className="mt-3 pt-2 border-t border-gray-100">
                          <a
                            href={`https://nvd.nist.gov/vuln/detail/${cve.id}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:text-blue-300 dark:hover:text-blue-300"
                          >
                            View on NVD →
                          </a>
                        </div>
                      </div>
                    )}
                  </div>
                );
              })
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CveResults;
