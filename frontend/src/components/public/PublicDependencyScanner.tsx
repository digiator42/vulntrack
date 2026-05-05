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

  const handleSearchCpe = async () => {
    if (!searchProduct.trim()) {
      toast.error("Please enter a product name");
      return;
    }

    setSearchingCpe(true);
    try {
      const response = await publicSearchCpe(searchProduct);
      setCpeResponse(response);

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
      CRITICAL: "bg-red-100 text-red-800 border-red-200",
      HIGH: "bg-orange-100 text-orange-800 border-orange-200",
      MEDIUM: "bg-yellow-100 text-yellow-800 border-yellow-200",
      LOW: "bg-blue-100 text-blue-800 border-blue-200",
    };
    return (
      colors[severity as keyof typeof colors] || "bg-gray-100 text-gray-800"
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
      <div className="bg-gradient-to-r from-blue-600 to-blue-800 rounded-xl shadow-lg p-8 text-white">
        <div className="flex items-center gap-3 mb-4">
          <ShieldCheckIcon className="h-10 w-10" />
          <h1 className="text-2xl font-bold">
            Free Dependency Vulnerability Scanner
          </h1>
        </div>
        <p className="text-blue-100 mb-6">
          Upload your manifest file to identify security vulnerabilities in your
          dependencies.
        </p>
        <div className="flex flex-wrap gap-4 text-sm">
          <div className="flex items-center gap-2">
            <CheckCircleIcon className="h-5 w-5 text-green-300" />
            <span>pom.xml (Maven)</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircleIcon className="h-5 w-5 text-green-300" />
            <span>package.json (NPM)</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircleIcon className="h-5 w-5 text-green-300" />
            <span>requirements.txt (Python)</span>
          </div>
        </div>
      </div>

      {/* CPE Search Section */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-purple-50 to-white">
          <div className="flex items-center gap-2">
            <EyeIcon className="h-5 w-5 text-purple-600" />
            <h3 className="text-lg font-semibold text-gray-900">
              CPE Lookup Tool
            </h3>
          </div>
          <p className="text-sm text-gray-600 mt-1">
            Search for Common Platform Enumeration (CPE) mappings by product
            name
          </p>
        </div>
        <div className="p-6">
          <div className="flex gap-3">
            <input
              type="text"
              value={searchProduct}
              onChange={(e) => setSearchProduct(e.target.value)}
              placeholder="Enter product name (e.g., snakeyaml, spring_framework, log4j)"
              className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-purple-500"
              onKeyPress={(e) => e.key === "Enter" && handleSearchCpe()}
            />
            <button
              onClick={handleSearchCpe}
              disabled={searchingCpe}
              className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 disabled:opacity-50 flex items-center gap-2"
            >
              {searchingCpe ? (
                <ArrowPathIcon className="h-4 w-4 animate-spin" />
              ) : (
                <EyeIcon className="h-4 w-4" />
              )}
              Search
            </button>
          </div>

          {/* Results Display */}
          {cpeResponse?.type === "results" &&
            cpeResponse.results &&
            cpeResponse.results.length > 0 && (
              <div className="mt-4 space-y-3">
                <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                  <ShieldCheckIcon className="h-4 w-4 text-green-600" />
                  CPE Mappings Found ({cpeResponse.results.length})
                </h4>

                <div
                  className={`space-y-3 ${cpeResponse.results.length > 5 ? "max-h-96 overflow-y-auto pr-2" : ""}`}
                >
                  {cpeResponse.results.map((cpe, index) => (
                    <div
                      key={cpe.id || index}
                      className="p-4 bg-green-50 border border-green-200 rounded-md hover:shadow-md transition-shadow"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-2 flex-wrap">
                            <span className="text-xs font-mono bg-green-100 px-2 py-0.5 rounded text-green-800">
                              #{index + 1}
                            </span>
                            <h4 className="font-semibold text-green-900">
                              {cpe.product}
                            </h4>
                            {cpe.enabled !== undefined && (
                              <span
                                className={`text-xs px-2 py-0.5 rounded-full ${cpe.enabled ? "bg-green-200 text-green-800" : "bg-gray-200 text-gray-600"}`}
                              >
                                {cpe.enabled ? "Active" : "Inactive"}
                              </span>
                            )}
                          </div>

                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
                            <div>
                              <p className="text-xs text-green-700 font-medium">
                                Vendor
                              </p>
                              <p className="text-sm text-green-800 mt-1">
                                {cpe.vendor || "N/A"}
                              </p>
                            </div>
                            {cpe.rawCpeName && (
                              <div className="md:col-span-2">
                                <p className="text-xs text-green-700 font-medium">
                                  CPE Name
                                </p>
                                <code className="text-sm bg-green-100 px-2 py-1 rounded block mt-1 break-all">
                                  {cpe.rawCpeName}
                                </code>
                              </div>
                            )}
                          </div>

                          {cpe.ecosystem && (
                            <div className="mt-2">
                              <p className="text-xs text-green-700 font-medium">
                                Ecosystem
                              </p>
                              <p className="text-sm text-green-800 mt-1">
                                {cpe.ecosystem}
                              </p>
                            </div>
                          )}

                          {cpe.notes && (
                            <div className="mt-3 pt-2 border-t border-green-200">
                              <p className="text-xs text-green-700 font-medium">
                                Notes
                              </p>
                              <p className="text-sm text-green-700 mt-1">
                                {cpe.notes}
                              </p>
                            </div>
                          )}

                          <div className="mt-2 flex gap-3 text-xs text-green-600">
                            {cpe.usageCount !== undefined && (
                              <span>Usage count: {cpe.usageCount}</span>
                            )}
                            {cpe.createdAt && (
                              <span>
                                Added:{" "}
                                {new Date(cpe.createdAt).toLocaleDateString()}
                              </span>
                            )}
                          </div>
                        </div>
                        <ShieldCheckIcon className="h-6 w-6 text-green-600 flex-shrink-0 ml-3" />
                      </div>
                    </div>
                  ))}
                </div>

                {cpeResponse.results.length > 5 && (
                  <div className="text-center text-xs text-gray-400 pt-2 border-t border-green-200">
                    <ArrowPathIcon className="h-3 w-3 inline mr-1" />
                    Scroll to see all {cpeResponse.results.length} mappings
                  </div>
                )}
              </div>
            )}

          {/* Suggestions Message */}
          {cpeResponse?.type === "suggestions" &&
            cpeResponse.suggestions &&
            cpeResponse.suggestions.length > 0 && (
              <div className="mt-4 space-y-3">
                <div className="p-4 bg-orange-100 border border-blue-200 rounded-md">
                  <div className="flex items-start gap-3">
                    <LightBulbIcon className="h-5 w-5 text-blue-600 mt-0.5" />
                    <div className="flex-1">
                      <p className="text-sm text-blue-800 font-medium">
                        No exact match for "{cpeResponse.query}"
                      </p>
                      <p className="text-xs text-blue-700 mt-1">
                        Found {cpeResponse.suggestions.length} similar product
                        {cpeResponse.suggestions.length > 1 ? "s" : ""}:
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-3">
                  <h4 className="text-sm font-semibold text-gray-700 flex items-center gap-2">
                    <LightBulbIcon className="h-4 w-4 text-blue-600" />
                    Suggestions ({cpeResponse.suggestions.length})
                  </h4>

                  <div
                    className={`space-y-3 ${cpeResponse.suggestions.length > 5 ? "max-h-96 overflow-y-auto pr-2" : ""}`}
                  >
                    {cpeResponse.suggestions.map((cpe, index) => (
                      <div
                        key={cpe.id || index}
                        className="p-4 bg-blue-50 border border-blue-200 rounded-md hover:shadow-md transition-all cursor-pointer hover:bg-blue-100"
                        onClick={() => {
                          setSearchProduct(cpe.product);
                          handleSearchCpe();
                        }}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-2 flex-wrap">
                              <span className="text-xs font-mono bg-blue-100 px-2 py-0.5 rounded text-blue-800">
                                #{index + 1}
                              </span>
                              <h4 className="font-semibold text-blue-900">
                                {cpe.product}
                              </h4>
                              <span className="text-xs text-blue-600 flex items-center gap-1">
                                Click to search{" "}
                                <ArrowRightIcon className="h-3 w-3" />
                              </span>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3">
                              <div>
                                <p className="text-xs text-blue-700 font-medium">
                                  Vendor
                                </p>
                                <p className="text-sm text-blue-800 mt-1">
                                  {cpe.vendor || "N/A"}
                                </p>
                              </div>
                              {cpe.rawCpeName && (
                                <div className="md:col-span-2">
                                  <p className="text-xs text-blue-700 font-medium">
                                    CPE Name
                                  </p>
                                  <code className="text-sm bg-blue-100 px-2 py-1 rounded block mt-1 break-all">
                                    {cpe.rawCpeName}
                                  </code>
                                </div>
                              )}
                            </div>
                          </div>
                          <ArrowRightIcon className="h-5 w-5 text-blue-500 flex-shrink-0 ml-3" />
                        </div>
                      </div>
                    ))}
                  </div>

                  {cpeResponse.suggestions.length > 5 && (
                    <div className="text-center text-xs text-gray-400 pt-2 border-t border-blue-200">
                      <ArrowPathIcon className="h-3 w-3 inline mr-1" />
                      Scroll to see all {cpeResponse.suggestions.length}{" "}
                      suggestions
                    </div>
                  )}
                </div>
              </div>
            )}

          {/* No Results Found */}
          {cpeResponse &&
            cpeResponse.type === "results" &&
            (!cpeResponse.results || cpeResponse.results.length === 0) && (
              <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-md">
                <div className="flex items-start gap-3">
                  <InformationCircleIcon className="h-5 w-5 text-yellow-600 mt-0.5" />
                  <div>
                    <p className="text-sm text-yellow-800 font-medium">
                      No CPE mappings found
                    </p>
                    <p className="text-xs text-yellow-700 mt-1">
                      No Common Platform Enumeration (CPE) mappings exist for "
                      {searchProduct}". This product may not be in the NVD
                      database or the name might be different.
                    </p>
                    <p className="text-xs text-yellow-700 mt-2">
                      Try alternative names or check the{" "}
                      <a
                        href="https://nvd.nist.gov/products/cpe"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-yellow-800 underline"
                      >
                        official CPE dictionary
                      </a>
                      .
                    </p>
                  </div>
                </div>
              </div>
            )}
        </div>
      </div>

      {/* Upload Area */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 bg-gradient-to-r from-blue-50 to-white">
          <div className="flex items-center gap-2">
            <DocumentMagnifyingGlassIcon className="h-5 w-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">
              Scan Your Dependencies
            </h3>
          </div>
          <p className="text-sm text-gray-600 mt-1">
            Upload your manifest file to check for known vulnerabilities (NVD
            database)
          </p>
        </div>

        <div className="p-6">
          <div
            {...getRootProps()}
            className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
              isDragActive
                ? "border-blue-500 bg-blue-50"
                : "border-gray-300 hover:border-blue-400 hover:bg-gray-50"
            }`}
          >
            <input {...getInputProps()} />
            <CloudArrowUpIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            {file ? (
              <div className="flex items-center justify-center gap-2">
                <CheckCircleIcon className="h-5 w-5 text-green-500" />
                <span className="text-sm text-gray-700">{file.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    setResult(null);
                  }}
                  className="text-red-500 hover:text-red-700"
                >
                  <XMarkIcon className="h-5 w-5" />
                </button>
              </div>
            ) : (
              <>
                <p className="text-gray-600">
                  Drag & drop a manifest file here, or click to select
                </p>
                <p className="text-sm text-gray-400 mt-1">
                  Supports: pom.xml, package.json, requirements.txt
                </p>
                <p className="text-xs text-gray-400 mt-2">
                  <LockClosedIcon className="h-3 w-3 inline mr-1" />
                  Your file is processed securely and not stored
                </p>
              </>
            )}
          </div>

          {file && (
            <div className="mt-4 flex justify-end">
              <button
                onClick={handleScan}
                disabled={scanning}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 flex items-center gap-2"
              >
                {scanning ? (
                  <>
                    <ArrowPathIcon className="h-4 w-4 animate-spin" />
                    Scanning...
                  </>
                ) : (
                  <>
                    <DocumentMagnifyingGlassIcon className="h-4 w-4" />
                    Scan Dependencies
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Results */}
      {result && (
        <div className="space-y-4 animate-fade-in">
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
              <p className="text-sm text-gray-500">Total Dependencies</p>
              <p className="text-2xl font-bold text-gray-900">
                {result.totalDependencies}
              </p>
            </div>
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
              <p className="text-sm text-gray-500">Vulnerable Dependencies</p>
              <p className="text-2xl font-bold text-red-600">
                {result.vulnerableDependencies}
              </p>
            </div>
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4">
              <p className="text-sm text-gray-500">Total Vulnerabilities</p>
              <p className="text-2xl font-bold text-orange-600">
                {result.vulnerabilities.reduce(
                  (sum, v) => sum + v.cves.length,
                  0,
                )}
              </p>
            </div>
          </div>

          {/* Vulnerability Disclaimer */}
          <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <InformationCircleIcon className="h-5 w-5 text-yellow-600 mt-0.5" />
              <div>
                <p className="text-sm text-yellow-800 font-medium">
                  Security Assessment Only
                </p>
                <p className="text-xs text-yellow-700 mt-1">
                  This scan uses public NVD data. Results are for informational
                  purposes only.
                </p>
              </div>
            </div>
          </div>

          {/* Vulnerable Dependencies List */}
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 bg-red-50">
              <div className="flex items-center gap-2">
                <ExclamationTriangleIcon className="h-5 w-5 text-red-600" />
                <h3 className="text-lg font-semibold text-gray-900">
                  Vulnerable Dependencies Found
                </h3>
              </div>
            </div>

            <div className="divide-y divide-gray-200">
              {result.vulnerabilities.length === 0 ? (
                <div className="p-8 text-center">
                  <CheckCircleIcon className="h-12 w-12 mx-auto text-green-500 mb-3" />
                  <p className="text-gray-500">
                    No vulnerabilities found! Your dependencies are secure.
                  </p>
                </div>
              ) : (
                result.vulnerabilities.map((vuln, idx) => (
                  <div
                    key={idx}
                    className="p-6 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <span className="text-2xl">
                            {getEcosystemIcon(vuln.dependency.ecosystem)}
                          </span>
                          <h4 className="text-lg font-semibold text-gray-900">
                            {vuln.dependency.name}
                          </h4>
                          <span className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-600">
                            v{vuln.dependency.version}
                          </span>
                          <span className="text-xs text-gray-400">
                            {vuln.dependency.ecosystem}
                          </span>
                        </div>
                        <p className="text-sm text-gray-500">
                          Manifest: {vuln.dependency.manifestFile}
                        </p>
                      </div>
                      <div className="text-right">
                        <span className="text-sm font-medium text-gray-600">
                          {vuln.cves.length}{" "}
                          {vuln.cves.length === 1 ? "CVE" : "CVEs"}
                        </span>
                      </div>
                    </div>

                    {/* CVEs List */}
                    <div className="space-y-3 pl-8">
                      {vuln.cves.map((cve, cveIdx) => {
                        const key = `${vuln.dependency.name}-${cve.id}`;
                        const isExpanded = expandedCves[key];

                        return (
                          <div
                            key={cveIdx}
                            className={`border rounded-lg overflow-hidden ${getSeverityColor(cve.severity)}`}
                          >
                            <div
                              className="px-4 py-3 cursor-pointer flex items-center justify-between hover:bg-opacity-50 transition-colors"
                              onClick={() => toggleCveExpand(key)}
                            >
                              <div className="flex items-center gap-3 flex-1">
                                <BugAntIcon className="h-5 w-5" />
                                <span className="font-mono font-medium">
                                  {cve.id}
                                </span>
                                <span
                                  className={`px-2 py-0.5 text-xs rounded-full bg-white bg-opacity-50`}
                                >
                                  CVSS {cve.cvssScore}
                                </span>
                                <span className="text-sm">{cve.severity}</span>
                              </div>
                              <div className="flex items-center gap-2">
                                <InformationCircleIcon className="h-5 w-5 text-gray-400" />
                              </div>
                            </div>

                            {isExpanded && (
                              <div className="px-4 py-3 bg-white border-t border-gray-200">
                                <div className="mb-3">
                                  <h5 className="text-sm font-semibold text-gray-700 mb-2">
                                    Description
                                  </h5>
                                  <p className="text-sm text-gray-600">
                                    {cve.description}
                                  </p>
                                </div>
                                {(cve.versionStartIncluding ||
                                  cve.versionEndExcluding) && (
                                  <div className="mb-3">
                                    <h5 className="text-sm font-semibold text-gray-700 mb-2">
                                      Affected Versions
                                    </h5>
                                    <p className="text-sm text-gray-600">
                                      {cve.versionStartIncluding &&
                                        `Fixed from: ${cve.versionStartIncluding}`}
                                      {cve.versionEndExcluding &&
                                        ` • Up to: ${cve.versionEndExcluding}`}
                                    </p>
                                  </div>
                                )}
                                <div>
                                  <h5 className="text-sm font-semibold text-gray-700 mb-2">
                                    Recommendation
                                  </h5>
                                  <p className="text-sm text-gray-600">
                                    Upgrade to a secure version as recommended
                                    by NVD.
                                    {cve.versionEndExcluding &&
                                      ` Fixed version: ${cve.versionEndExcluding}`}
                                  </p>
                                </div>
                                <div className="mt-3 pt-2 border-t border-gray-100">
                                  <a
                                    href={`https://nvd.nist.gov/vuln/detail/${cve.id}`}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="text-xs text-blue-600 hover:text-blue-800"
                                  >
                                    View on NVD →
                                  </a>
                                </div>
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
    </div>
  );
};

export default PublicDependencyScanner;
