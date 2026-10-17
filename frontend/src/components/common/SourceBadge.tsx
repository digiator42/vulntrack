// src/components/common/SourceBadge.tsx
import React from "react";

/**
 * Shows which vulnerability databases reported a finding.
 *
 * - "OSV+NVD" -> both independent sources agreed; highest confidence
 * - "OSV"     -> ecosystem-native advisory database only
 * - "NVD"     -> local NVD CPE cache only
 *
 * Missing provenance is treated as NVD, matching the backend default.
 */
const SourceBadge: React.FC<{ sources?: string; className?: string }> = ({
  sources,
  className = "",
}) => {
  const value = sources || "NVD";
  const corroborated = value.includes("+");

  const label = corroborated ? "OSV + NVD" : value;
  const title = corroborated
    ? "Confirmed by two independent sources (OSV and NVD)"
    : value === "OSV"
      ? "Reported by the OSV database"
      : "Reported by the NVD cache";

  const tone = corroborated
    ? "bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300"
    : value === "OSV"
      ? "bg-sky-100 text-sky-800 dark:bg-sky-900/40 dark:text-sky-300"
      : "bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300";

  return (
    <span
      title={title}
      className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${tone} ${className}`}
    >
      {corroborated ? "✓ " : ""}
      {label}
    </span>
  );
};

export default SourceBadge;