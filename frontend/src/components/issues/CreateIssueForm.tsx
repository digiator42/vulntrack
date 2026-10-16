// components/issues/CreateIssueForm.tsx
import React, { useState } from "react";
import { useRouter } from "next/router";
import CVSSCalculator from "../cvss/CVSSCalculator";
import CVELookup from "../cvss/CVELookup";
import { createIssue } from "../../services/api";
import { CVEEntry, Issue } from "../../types";
import { ButtonShimmer } from "@/components/common/Shimmer";
import MarkdownEditor from "@/components/common/MarkdownEditor";

interface FormData {
  title: string;
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
  description: string;
  cvssVector?: string;
  cvssScore?: number;
  cweId?: string;
}

const CreateIssueForm: React.FC = () => {
  const router = useRouter();
  const [formData, setFormData] = useState<FormData>({
    title: "",
    severity: "MEDIUM",
    description: "",
  });
  const [cvssVector, setCvssVector] = useState<string>("");
  const [cvssScore, setCvssScore] = useState<number>(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleCVSSChange = (vector: string, score: number) => {
    setCvssVector(vector);
    setCvssScore(score);

    // Auto-set severity based on CVSS score
    if (score >= 9) {
      setFormData({ ...formData, severity: "CRITICAL" });
    } else if (score >= 7) {
      setFormData({ ...formData, severity: "HIGH" });
    } else if (score >= 4) {
      setFormData({ ...formData, severity: "MEDIUM" });
    } else if (score > 0) {
      setFormData({ ...formData, severity: "LOW" });
    }
  };

  const handleCVESelect = (cveData: CVEEntry) => {
    setFormData({
      ...formData,
      title: cveData.id,
      description: cveData.description,
      cweId: cveData.weaknesses?.[0]?.cweId || formData.cweId,
    });

    if (cveData.cvssVector) {
      setCvssVector(cveData.cvssVector);
      setCvssScore(cveData.cvssScore || 0);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const issueData = {
        title: formData.title,
        severity: formData.severity,
        description: formData.description,
        cvssVector: cvssVector || undefined,
        cvssScore: cvssScore || undefined,
        cweId: formData.cweId || undefined,
      };

      const response = await createIssue(issueData);
      router.push(`/issues/${response.id}`);
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to create issue");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-md text-red-700">
          {error}
        </div>
      )}

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Title *
        </label>
        <input
          type="text"
          required
          value={formData.title}
          onChange={(e) => setFormData({ ...formData, title: e.target.value })}
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="Enter vulnerability title"
        />
      </div>

      {/* CVE Lookup Component */}
      <CVELookup onCVESelect={handleCVESelect} />

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          CWE ID (Optional)
        </label>
        <input
          type="text"
          value={formData.cweId || ""}
          onChange={(e) => setFormData({ ...formData, cweId: e.target.value })}
          placeholder="e.g., CWE-89"
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Severity *
        </label>
        <select
          required
          value={formData.severity}
          onChange={(e) =>
            setFormData({ ...formData, severity: e.target.value as any })
          }
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="CRITICAL">Critical</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </div>

      {/* CVSS Calculator Component */}
      <CVSSCalculator onVectorChange={handleCVSSChange} />

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Description
        </label>
        <MarkdownEditor
          value={formData.description}
          onChange={(value) => setFormData({ ...formData, description: value })}
          placeholder="## Vulnerability Details

            Describe the vulnerability here...

            ### Impact
            - List impact points

            ### Recommendation
            Provide remediation steps"
          helpText="Supports Markdown formatting. Use tables, lists, code blocks, and more."
        />
      </div>

      <div className="flex justify-end space-x-3">
        <button
          type="button"
          onClick={() => router.back()}
          className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          {loading ? <ButtonShimmer /> : "Create Issue"}
        </button>
      </div>
    </form>
  );
};

export default CreateIssueForm;
