// src/components/dependency/DependencyScanner.tsx
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
  PlusCircleIcon,
} from "@heroicons/react/24/outline";
import { scanDependencies } from "@/services/dependencyScanner";
import {
  DependencyScanResult,
  VulnerableDependency,
  CveVulnerability,
} from "@/types/dependency";
import { createIssue } from "@/services/api";
import MarkdownRenderer from "@/components/common/MarkdownRenderer";

interface DependencyScannerProps {
  onIssueCreated?: () => void;
}

const DependencyScanner: React.FC<DependencyScannerProps> = ({
  onIssueCreated,
}) => {
  const [file, setFile] = useState<File | null>(null);
  const [scanning, setScanning] = useState(false);
  const [result, setResult] = useState<DependencyScanResult | null>(null);
  const [creatingIssues, setCreatingIssues] = useState<Record<string, boolean>>(
    {},
  );
  const [expandedCves, setExpandedCves] = useState<Record<string, boolean>>({});

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
      const scanResult = await scanDependencies(file, false);
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

  const handleCreateIssue = async (
    vuln: VulnerableDependency,
    cve: CveVulnerability,
  ) => {
    const key = `${vuln.dependency.name}-${cve.id}`;
    setCreatingIssues((prev) => ({ ...prev, [key]: true }));

    try {
      // Generate markdown description for the issue
      const description = `## 🔒 Vulnerable Dependency Detected

| Field | Value |
|-------|-------|
| **CVE ID** | ${cve.id} |
| **Dependency** | \`${vuln.dependency.name}\` |
| **Current Version** | \`${vuln.dependency.version}\` |
| **Ecosystem** | ${vuln.dependency.ecosystem} |
| **Manifest File** | \`${vuln.dependency.manifestFile}\` |
| **CVSS Score** | **${cve.cvssScore}** (${cve.severity}) |
| **Published Date** | ${new Date(cve.publishedDate).toLocaleDateString()} |
| **CVSS Vector** | \`${cve.vectorString}\` |

### Description

${cve.description}

### Version Info

${cve.versionStartIncluding ? `- Fixed from: ${cve.versionStartIncluding}` : ""}
${cve.versionEndExcluding ? `- Up to: ${cve.versionEndExcluding}` : ""}

### 💡 Recommendation

**Upgrade** \`${vuln.dependency.name}\` to a **safe version** (${cve.versionEndExcluding ? `>= ${cve.versionEndExcluding}` : "check NVD for exact fixed version"}).

### 📋 Remediation Steps

1. Update the dependency in your \`${vuln.dependency.manifestFile}\` file
2. Run \`${
        vuln.dependency.ecosystem === "MAVEN"
          ? "mvn clean install"
          : vuln.dependency.ecosystem === "NPM"
            ? "npm install"
            : "pip install -r requirements.txt"
      }\`
3. Test the application to ensure compatibility
4. Mark this issue as **RESOLVED** after verification

### 🔗 References

- [NVD Entry for ${cve.id}](https://nvd.nist.gov/vuln/detail/${cve.id})
`;

      const issueData = {
        title: `[${vuln.dependency.ecosystem}] ${cve.id} - ${vuln.dependency.name}@${vuln.dependency.version}`,
        severity: cve.severity as "CRITICAL" | "HIGH" | "MEDIUM" | "LOW",
        description: description,
        cvssVector: cve.vectorString,
        cvssScore: cve.cvssScore,
        cweId: undefined,
      };

      const response = await createIssue(issueData);
      toast.success(`Issue created: ${response.title}`);

      // Update the result with the new issue ID
      setResult((prev) => {
        if (!prev) return prev;
        const updated = { ...prev };
        const vulnIndex = updated.vulnerabilities.findIndex(
          (v) => v.dependency.name === vuln.dependency.name,
        );
        if (vulnIndex !== -1) {
          const cveIndex = updated.vulnerabilities[vulnIndex].cves.findIndex(
            (c) => c.id === cve.id,
          );
          if (cveIndex !== -1) {
            updated.vulnerabilities[vulnIndex].createdIssueIds[cveIndex] =
              response.id;
          }
        }
        return updated;
      });

      onIssueCreated?.();
    } catch (error) {
      console.error("Failed to create issue:", error);
      toast.error("Failed to create issue");
    } finally {
      setCreatingIssues((prev) => ({ ...prev, [key]: false }));
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
      {/* Upload Area */}
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white dark:from-blue-950 dark:to-gray-900">
          <div className="flex items-center gap-2">
            <DocumentMagnifyingGlassIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
            <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
              Dependency Scanner
            </h3>
          </div>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Upload a manifest file (pom.xml, package.json, requirements.txt) to
            scan for vulnerabilities
          </p>
        </div>

        <div className="p-6">
          <div
            {...getRootProps()}
            className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors ${
              isDragActive
                ? "border-blue-500 bg-blue-50 dark:bg-blue-900/25"
                : "border-gray-300 dark:border-gray-600 hover:border-blue-400 hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800"
            }`}
          >
            <input {...getInputProps()} />
            <CloudArrowUpIcon className="h-12 w-12 mx-auto text-gray-400 mb-4" />
            {file ? (
              <div className="flex items-center justify-center gap-2">
                <CheckCircleIcon className="h-5 w-5 text-green-500" />
                <span className="text-sm text-gray-700 dark:text-gray-300">{file.name}</span>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setFile(null);
                    setResult(null);
                  }}
                  className="text-red-500 hover:text-red-700 dark:text-red-300 dark:hover:text-red-300"
                >
                  <XMarkIcon className="h-5 w-5" />
                </button>
              </div>
            ) : (
              <>
                <p className="text-gray-600 dark:text-gray-400">
                  Drag & drop a manifest file here, or click to select
                </p>
                <p className="text-sm text-gray-400 mt-1">
                  Supports: pom.xml, package.json, requirements.txt
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
        <div className="space-y-4">
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
              <p className="text-sm text-gray-500 dark:text-gray-400">Total Dependencies</p>
              <p className="text-2xl font-bold text-gray-900 dark:text-white">
                {result.totalDependencies}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
              <p className="text-sm text-gray-500 dark:text-gray-400">Vulnerable Dependencies</p>
              <p className="text-2xl font-bold text-red-600 dark:text-red-400">
                {result.vulnerableDependencies}
              </p>
            </div>
            <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-4">
              <p className="text-sm text-gray-500 dark:text-gray-400">Total Vulnerabilities</p>
              <p className="text-2xl font-bold text-orange-600 dark:text-orange-400">
                {result.vulnerabilities.reduce(
                  (sum, v) => sum + v.cves.length,
                  0,
                )}
              </p>
            </div>
          </div>

          {/* Vulnerable Dependencies List */}
          <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-red-50 dark:bg-red-900/30">
              <div className="flex items-center gap-2">
                <ExclamationTriangleIcon className="h-5 w-5 text-red-600 dark:text-red-400" />
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  Vulnerable Dependencies
                </h3>
              </div>
            </div>

            <div className="divide-y divide-gray-200 dark:divide-gray-700">
              {result.vulnerabilities.length === 0 ? (
                <div className="p-8 text-center">
                  <CheckCircleIcon className="h-12 w-12 mx-auto text-green-500 mb-3" />
                  <p className="text-gray-500 dark:text-gray-400">
                    No vulnerabilities found! Your dependencies are secure.
                  </p>
                </div>
              ) : (
                result.vulnerabilities.map((vuln, idx) => (
                  <div
                    key={idx}
                    className="p-6 hover:bg-gray-50 dark:bg-gray-800 dark:hover:bg-gray-800 transition-colors"
                  >
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-3 mb-2">
                          <span className="text-2xl">
                            {getEcosystemIcon(vuln.dependency.ecosystem)}
                          </span>
                          <h4 className="text-lg font-semibold text-gray-900 dark:text-white">
                            {vuln.dependency.name}
                          </h4>
                          <span className="px-2 py-1 text-xs rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400">
                            v{vuln.dependency.version}
                          </span>
                          <span className="text-xs text-gray-400">
                            {vuln.dependency.ecosystem}
                          </span>
                        </div>
                        <p className="text-sm text-gray-500 dark:text-gray-400">
                          Manifest: {vuln.dependency.manifestFile}
                        </p>
                      </div>
                      <div className="text-right">
                        <span className="text-sm font-medium text-gray-600 dark:text-gray-400">
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
                        const isCreated = vuln.createdIssueIds?.[cveIdx];
                        const isCreating = creatingIssues[key];

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
                                  className={`px-2 py-0.5 text-xs rounded-full bg-white dark:bg-gray-900 bg-opacity-50`}
                                >
                                  CVSS {cve.cvssScore}
                                </span>
                                <span className="text-sm">{cve.severity}</span>
                              </div>
                              <div className="flex items-center gap-2">
                                {isCreated && (
                                  <span className="text-xs text-green-600 dark:text-green-400 flex items-center gap-1">
                                    <CheckCircleIcon className="h-4 w-4" />
                                    Created
                                  </span>
                                )}
                                <button
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleCreateIssue(vuln, cve);
                                  }}
                                  disabled={isCreating || !!isCreated}
                                  className="px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1"
                                >
                                  {isCreating ? (
                                    <ArrowPathIcon className="h-3 w-3 animate-spin" />
                                  ) : (
                                    <PlusCircleIcon className="h-3 w-3" />
                                  )}
                                  {isCreated ? "Issue Created" : "Create Issue"}
                                </button>
                                <InformationCircleIcon className="h-5 w-5 text-gray-400" />
                              </div>
                            </div>

                            {isExpanded && (
                              <div className="px-4 py-3 bg-white dark:bg-gray-900 border-t border-gray-200 dark:border-gray-700">
                                <MarkdownRenderer
                                  content={`### Description\n\n${cve.description}\n\n### Version Info\n\n${
                                    cve.versionStartIncluding
                                      ? `- Fixed from: ${cve.versionStartIncluding}\n`
                                      : ""
                                  }${
                                    cve.versionEndExcluding
                                      ? `- Up to: ${cve.versionEndExcluding}\n`
                                      : ""
                                  }\n\n### Recommendation\n\nUpgrade to a secure version as recommended by NVD.`}
                                />
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

export default DependencyScanner;
