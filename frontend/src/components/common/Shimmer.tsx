// src/components/common/Shimmer.tsx
import React from "react";

/** Base shimmer block. Size it with className (h-*, w-*). */
export const Shimmer: React.FC<{ className?: string }> = ({ className = "" }) => (
  <div aria-hidden="true" className={`animate-shimmer rounded-md ${className}`} />
);

/** Full-page loading state: title, subtitle, cards, body. */
export const PageShimmer: React.FC = () => (
  <div className="mx-auto max-w-6xl space-y-4 p-6" aria-label="Loading">
    <Shimmer className="h-8 w-1/3" />
    <Shimmer className="h-4 w-2/3" />
    <div className="grid gap-4 sm:grid-cols-3">
      {[0, 1, 2].map((i) => (
        <Shimmer key={i} className="h-28" />
      ))}
    </div>
    <Shimmer className="h-64" />
  </div>
);

/** List/table loading state: header bar plus rows. */
export const TableShimmer: React.FC<{ rows?: number }> = ({ rows = 6 }) => (
  <div className="space-y-2" aria-label="Loading">
    <Shimmer className="h-10" />
    {Array.from({ length: rows }).map((_, i) => (
      <Shimmer key={i} className="h-12" />
    ))}
  </div>
);

/** Metric/stat card loading state. */
export const CardShimmer: React.FC<{ cards?: number }> = ({ cards = 4 }) => (
  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-label="Loading">
    {Array.from({ length: cards }).map((_, i) => (
      <div key={i} className="space-y-2 rounded-lg border border-gray-100 p-4">
        <Shimmer className="h-4 w-1/2" />
        <Shimmer className="h-8 w-3/4" />
      </div>
    ))}
  </div>
);

/** Inline shimmer for inside buttons (light tone for colored backgrounds). */
export const ButtonShimmer: React.FC = () => (
  <span className="inline-flex items-center" aria-label="Loading">
    <span className="animate-shimmer-light h-4 w-20 rounded" />
  </span>
);
