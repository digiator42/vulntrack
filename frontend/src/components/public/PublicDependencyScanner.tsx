// src/components/public/PublicDependencyScanner.tsx
import React, { useState, useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { toast } from "sonner";
import {
  CloudArrowUpIcon,
  DocumentMagnifyingGlassIcon,
  XMarkIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  ArrowPathIcon,
  BugAntIcon,
  ShieldCheckIcon,
  LockClosedIcon,
  EyeIcon,
  LightBulbIcon,
  ArrowRightIcon,
} from "@heroicons/react/24/outline";
import {
  publicScanDependencies,
  publicSearchCpe,
  CpeSearchResponse,
} from "@/services/publicScanner";
import { PublicScanResult } from "@/services/publicScanner";
import { CveResult, fetchCvesByCpe } from "@/services/cveByCpe";
import CveResults from "./CveResults";

const PublicDependencyScanner: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<PublicScanResult | null>(null);
  const [expandedCves, setExpandedCves] = useState<Record<string, boolean>>({});
  const [searchProduct, setSearchProduct] = useState("");
  const [cpeResponse, setCpeResponse] = useState<CpeSearchResponse | null>(
    null,
  );
  const [searchingCpe, setSearchingCpe] = useState(false);
  const [showCveModal, setShowCveModal] = useState(false);
  const [selectedCpe, setSelectedCpe] = useState<string>("");
  const [cveResults, setCveResults] = useState<CveResult[]>([]);
  const [loadingCves, setLoadingCves] = useState(false);

  const viewCvesForCpe = async (cpeName: string) => {
    setSelectedCpe(cpeName);
    setShowCveModal(true);
    setLoadingCves(true);

    try {
      const results = await fetchCvesByCpe(cpeName);
      setCveResults(results);
    } catch (error) {
      console.error("Failed to fetch CVEs:", error);
      toast.error("Failed to fetch CVEs for this CPE");
      setCveResults([]);
    } finally {
      setLoadingCves(false);
    }
  };

  const onDrop = useCallback((acceptedFiles: File[]) => {
    const uploadedFile = acceptedFiles[0];
    const allowedExtensions = [".xml", ".json", ".txt"];
    const fileExtension = uploadedFile.name.split(".").pop()?.toLowerCase();

    if (allowedExtensions.includes(`.${fileExtension}`)) {
      setFile(uploadedFile);
      setResult(null);
    } else {
      toast.error(
        "Please upload a valid manifest file (pom.xml, package.json, requirements.txt)",
      );
    }
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/xml": [".xml"],
      "application/json": [".json"],
      "text/plain": [".txt"],
    },
    maxFiles: 1,
  });

  const handleScan = async () => {
    if (!file) {
      toast.error("Please select a file first");
      return;
    }

    setScanning(true);
    try {
      const scanResult = await publicScanDependencies(file);
      setResult(scanResult);
      toast.success(
        `Scan completed. Found ${scanResult.vulnerableDependencies} vulnerable dependencies`,
      );
    } catch (error) {
      console.error("Scan failed:", error);
      toast.error("Failed to scan dependencies");
    } finally {
      setScanning(false);
    }
  };

  const handleSearchCpe = async (searchTerm?: string) => {
      const productToSearch = searchTerm || searchProduct;

    if (!productToSearch?.trim()) {
      toast.error("Please enter a product name");
      return;
    }

    setSearchingCpe(true);
    try {
      const response = await publicSearchCpe(productToSearch);
      setCpeResponse(response);

      console.log("CPE Search Response:", productToSearch);

      if (
        response.type === "results" &&
        response.results &&
        response.results.length > 0
      ) {
        toast.success(
          `Found ${response.results.length} CPE mapping${response.results.length > 1 ? "s" : ""}`,
        );
      } else if (
        response.type === "suggestions" &&
        response.suggestions &&
        response.suggestions.length > 0
      ) {
        toast.info(
          `No exact match. Showing ${response.suggestions.length} suggestions`,
        );
      } else {
        toast.error("No CPE mappings found for this product");
      }
    } catch (error) {
      console.error("Search failed:", error);
      toast.error("Failed to search CPE");
      setCpeResponse(null);
    } finally {
      setSearchingCpe(false);
    }
  };

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

  const getEcosystemIcon = (ecosystem: string) => {
    const icons = {
      MAVEN: "☕",
      NPM: "📦",
      PYPI: "🐍",
      NUGET: "🔧",
      GRADLE: "📚",
    };
    return icons[ecosystem as keyof typeof icons] || "📦";
  };

  const toggleCveExpand = (key: string) => {
    setExpandedCves((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="space-y-6">
      {/* Hero Section */}
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 rounded-xl shadow-lg p-6 text-white">
        <div className="flex items-center gap-3 mb-3">
          <ShieldCheckIcon className="h-8 w-8" />
          <h1 className="text-xl font-bold">
            Dependency Vulnerability Scanner
          </h1>
        </div>
        <p className="text-blue-100 mb-4 text-sm">
          Upload your manifest file to identify security vulnerabilities in your
          dependencies.
        </p>
        <div className="flex flex-wrap gap-3 text-xs">
          <div className="flex items-center gap-1">
            <CheckCircleIcon className="h-4 w-4 text-green-300" />
            <span>pom.xml (Maven)</span>
          </div>
          <div className="flex items-center gap-1">
            <CheckCircleIcon className="h-4 w-4 text-green-300" />
            <span>package.json (NPM)</span>
          </div>
          <div className="flex items-center gap-1">
            <CheckCircleIcon className="h-4 w-4 text-green-300" />
            <span>requirements.txt (Python)</span>
          </div>
        </div>
      </div>

      {/* CPE Search Section */}
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-purple-50 to-white dark:from-purple-950 dark:to-gray-900">
          <div className="flex items-center gap-2">
            <EyeIcon className="h-4 w-4 text-purple-600 dark:text-purple-400" />
            <h3 className="text-md font-semibold text-gray-900 dark:text-white">
              CPE Lookup Tool
            </h3>
          </div>
        </div>
        <div className="p-4">
          <div className="flex gap-2">
            <input
              type="text"
              value={searchProduct}
              onChange={(e) => setSearchProduct(e.target.value)}
              placeholder="Enter product name (e.g., spring_framework, snakeyaml)"
              className="flex-1 px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
              onKeyPress={(e) => e.key === "Enter" && handleSearchCpe()}
            />
            <button
              onClick={() => handleSearchCpe()}
              disabled={searchingCpe}
              className="px-3 py-1.5 bg-purple-600 text-white text-sm rounded-md hover:bg-purple-700 disabled:opacity-50 flex items-center gap-1"
            >
              {searchingCpe ? (
                <ArrowPathIcon className="h-3 w-3 animate-spin" />
              ) : (
                <EyeIcon className="h-3 w-3" />
              )}
              Search
            </button>
          </div>

          {/* Results Display - Condensed */}
          {cpeResponse?.type === "results" &&
            cpeResponse.results &&
            cpeResponse.results.length > 0 && (
              <div className="mt-3 space-y-2">
                <h4 className="text-xs font-semibold text-gray-700 dark:text-gray-300 flex items-center gap-1">
                  <ShieldCheckIcon className="h-3 w-3 text-green-600 dark:text-green-400" />
                  CPE Mappings ({cpeResponse.results.length})
                </h4>

                <div
                  className={`space-y-2 ${cpeResponse.results.length > 5 ? "max-h-80 overflow-y-auto pr-1" : ""}`}
                >
                  {cpeResponse.results.map((cpe, index) => (
                    <div
                      key={cpe.id || index}
                      className="p-3 bg-green-50 border border-green-200 rounded-md dark:bg-green-900/25 dark:border-green-800"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 flex-wrap mb-1">
                            <span className="text-xs font-mono bg-green-100 px-1.5 py-0.5 rounded text-green-800 dark:bg-green-900/60 dark:text-green-300">
                              #{index + 1}
                            </span>
                            <h4 className="font-semibold text-sm text-green-900 dark:text-green-200">
                              {cpe.product}
                            </h4>
                            <span className="text-xs text-gray-500 dark:text-gray-400">
                              ({cpe.vendor || "N/A"})
                            </span>
                          </div>
                          {cpe.rawCpeName && (
                            <div className="flex items-center justify-between gap-2">
                              <a
                                href={`https://nvd.nist.gov/products/cpe/search/results?namingFormat=2.3&keyword=${encodeURIComponent(cpe.rawCpeName)}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-xs bg-green-100 dark:bg-green-900/60 dark:text-green-300 px-1.5 py-0.5 rounded break-all flex-1 hover:bg-green-200 dark:hover:bg-green-800 transition"
                              >
                                {cpe.rawCpeName}
                              </a>
                              <button
                                onClick={() => viewCvesForCpe(cpe.rawCpeName)}
                                className="text-xs bg-blue-600 text-white px-2 py-1 rounded flex items-center gap-1 whitespace-nowrap hover:bg-blue-700 cursor-pointer transition"
                              >
                                <BugAntIcon className="h-3 w-3" />
                                CVEs
                              </button>
                            </div>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

          {/* Suggestions - Condensed */}
          {cpeResponse?.type === "suggestions" &&
            cpeResponse.suggestions &&
            cpeResponse.suggestions.length > 0 && (
              <div className="mt-3 space-y-2">
                <div className="p-2 bg-orange-50 border border-orange-200 rounded-md dark:bg-orange-900/25 dark:border-orange-800">
                  <div className="flex items-start gap-2">
                    <LightBulbIcon className="h-4 w-4 text-orange-600 dark:text-orange-400 mt-0.5" />
                    <p className="text-xs text-orange-700 dark:text-orange-300">
                      No exact match for "{cpeResponse.query}". Showing{" "}
                      {cpeResponse.suggestions.length} suggestions.
                    </p>
                  </div>
                </div>

                <div
                  className={`space-y-2 ${cpeResponse.suggestions.length > 5 ? "max-h-80 overflow-y-auto pr-1" : ""}`}
                >
                  {cpeResponse.suggestions.map((cpe, index) => (
                    <div
                      key={cpe.id || index}
                      className="p-2 bg-blue-50 border border-blue-200 rounded-md cursor-pointer hover:bg-blue-100 dark:bg-blue-900/25 dark:border-blue-800 dark:hover:bg-blue-800"
                      onClick={() => {
                        setSearchProduct(cpe.product);
                        handleSearchCpe(cpe.product);
                      }}
                    >
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-xs font-medium text-blue-900 dark:text-blue-200">
                            {cpe.product}
                          </span>
                          <span className="text-xs text-gray-500 dark:text-gray-400 ml-2">
                            ({cpe.vendor})
                          </span>
                        </div>
                        <ArrowRightIcon className="h-3 w-3 text-blue-500 dark:text-blue-400" />
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

          {/* No Results */}
          {cpeResponse &&
            cpeResponse.type === "results" &&
            (!cpeResponse.results || cpeResponse.results.length === 0) && (
              <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-md dark:bg-yellow-900/25 dark:border-yellow-800">
                <p className="text-xs text-yellow-700 dark:text-yellow-300">
                  No CPE mappings found for "{searchProduct}"
                </p>
              </div>
            )}
        </div>
      </div>

      {/* Upload Area - Condensed */}
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white dark:from-blue-950 dark:to-gray-900">
          <div className="flex items-center gap-2">
            <DocumentMagnifyingGlassIcon className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            <h3 className="text-md font-semibold text-gray-900 dark:text-white">
              Scan Dependencies
            </h3>
          </div>
        </div>

        <div className="p-4">
          <div
            {...getRootProps()}
            className={`border-2 border-dashed rounded-lg p-6 text-center cursor-pointer transition-colors ${
              isDragActive
                ? "border-blue-500 bg-blue-50 dark:bg-blue-900/25"
                : "border-gray-300 dark:border-gray-600 hover:border-blue-400 hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800"
            }`}
          >
            <input {...getInputProps()} />
            <CloudArrowUpIcon className="h-8 w-8 mx-auto text-gray-400 mb-2" />
            {file ? (
              <div className="flex items-center justify-center gap-2">
                <CheckCircleIcon className="h-4 w-4 text-green-500" />
                <span className="text-sm text-gray-700 dark:text-gray-300">{file.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    setResult(null);
                  }}
                  className="text-red-500 hover:text-red-700 dark:text-red-300 dark:hover:text-red-300"
                >
                  <XMarkIcon className="h-4 w-4" />
                </button>
              </div>
            ) : (
              <>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Drag & drop or click to upload
                </p>
                <p className="text-xs text-gray-400 mt-1">
                  pom.xml | package.json | requirements.txt
                </p>
              </>
            )}
          </div>

          {file && (
            <div className="mt-3 flex justify-end">
              <button
                onClick={handleScan}
                disabled={scanning}
                className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1"
              >
                {scanning ? (
                  <ArrowPathIcon className="h-3 w-3 animate-spin" />
                ) : (
                  <DocumentMagnifyingGlassIcon className="h-3 w-3" />
                )}
                {scanning ? "Scanning..." : "Scan"}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Results - Condensed */}
      {result && (
        <div className="space-y-3">
          {/* Summary Cards */}
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-3 text-center">
              <p className="text-xs text-gray-500 dark:text-gray-400">Total Deps</p>
              <p className="text-xl font-bold text-gray-900 dark:text-white">
                {result.totalDependencies}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-3 text-center">
              <p className="text-xs text-gray-500 dark:text-gray-400">Vulnerable</p>
              <p className="text-xl font-bold text-red-600 dark:text-red-400">
                {result.vulnerableDependencies}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-3 text-center">
              <p className="text-xs text-gray-500 dark:text-gray-400">CVEs</p>
              <p className="text-xl font-bold text-orange-600 dark:text-orange-400">
                {result.vulnerabilities.reduce(
                  (sum, v) => sum + v.cves.length,
                  0,
                )}
              </p>
            </div>
          </div>

          {/* Vulnerable Dependencies List - Condensed */}
          <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="px-4 py-2 border-b border-gray-200 dark:border-gray-700 bg-red-50 dark:bg-red-900/30">
              <div className="flex items-center gap-1">
                <ExclamationTriangleIcon className="h-4 w-4 text-red-600 dark:text-red-400" />
                <h3 className="text-sm font-semibold text-gray-900 dark:text-white">
                  Vulnerable Dependencies
                </h3>
              </div>
            </div>

            <div className="divide-y divide-gray-200 dark:divide-gray-700 max-h-96 overflow-y-auto">
              {result.vulnerabilities.length === 0 ? (
                <div className="p-6 text-center">
                  <CheckCircleIcon className="h-8 w-8 mx-auto text-green-500 mb-2" />
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    No vulnerabilities found!
                  </p>
                </div>
              ) : (
                result.vulnerabilities.map((vuln, idx) => (
                  <div key={idx} className="p-3 hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-lg">
                          {getEcosystemIcon(vuln.dependency.ecosystem)}
                        </span>
                        <span className="font-medium text-sm">
                          {vuln.dependency.name}
                        </span>
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          v{vuln.dependency.version}
                        </span>
                      </div>
                      <span className="text-xs font-medium text-gray-600 dark:text-gray-400">
                        {vuln.cves.length} CVEs
                      </span>
                    </div>

                    <div className="space-y-2 pl-6">
                      {vuln.cves.map((cve, cveIdx) => {
                        const key = `${vuln.dependency.name}-${cve.id}`;
                        const isExpanded = expandedCves[key];

                        return (
                          <div
                            key={cveIdx}
                            className={`text-xs rounded-md overflow-hidden ${getSeverityColor(cve.severity)}`}
                          >
                            <div
                              className="px-2 py-1.5 cursor-pointer flex items-center justify-between"
                              onClick={() => toggleCveExpand(key)}
                            >
                              <div className="flex items-center gap-2">
                                <BugAntIcon className="h-3 w-3" />
                                <span className="font-mono">{cve.id}</span>
                                <span className="px-1 py-0.5 text-xs rounded-full bg-white dark:bg-gray-900 bg-opacity-50">
                                  CVSS {cve.cvssScore}
                                </span>
                              </div>
                              <InformationCircleIcon className="h-3 w-3" />
                            </div>

                            {isExpanded && (
                              <div className="px-2 py-2 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700 text-xs">
                                <p className="text-gray-600 dark:text-gray-400 mb-1">
                                  {cve.description.substring(0, 150)}...
                                </p>
                                <a
                                  href={`https://nvd.nist.gov/vuln/detail/${cve.id}`}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:text-blue-300 dark:hover:text-blue-300"
                                >
                                  View on NVD →
                                </a>
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {/* CVE Result modal */}
      {showCveModal && (
        <CveResults
          cves={cveResults}
          cpeName={selectedCpe}
          loading={loadingCves}
          onClose={() => {
            setShowCveModal(false);
            setSelectedCpe("");
            setCveResults([]);
          }}
        />
      )}
    </div>
  );
};

export default PublicDependencyScanner;
