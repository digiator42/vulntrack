// src/components/dependency/NvdSyncPanel.tsx
import React, { useCallback, useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import {
  ArrowPathIcon,
  CheckCircleIcon,
  ExclamationTriangleIcon,
} from "@heroicons/react/24/outline";
import { useAuth } from "@/hooks/useAuth";
import { triggerNvdSync, getNvdSyncStatus } from "@/services/dependencyScanner";
import LoadingSpinner from "@/components/common/LoadingSpinner";

/**
 * Admin-only NVD cache panel. Scans read from the local CVE cache, so an
 * empty/stale cache means empty results. Shows sync state and lets an admin
 * kick off a full background sync (202 Accepted) with status polling.
 */
const NvdSyncPanel: React.FC = () => {
  const { user } = useAuth();
  const [running, setRunning] = useState(false);
  const [position, setPosition] = useState({ lastStartIndex: 0, totalResults: 0 });
  const [lastError, setLastError] = useState("");
  const [starting, setStarting] = useState(false);
  const pollRef = useRef<NodeJS.Timeout | null>(null);

  const refreshStatus = useCallback(async () => {
    try {
      const s = await getNvdSyncStatus();
      setRunning(s.running);
      setPosition({ lastStartIndex: s.lastStartIndex ?? 0, totalResults: s.totalResults ?? 0 });
      setLastError(s.lastError ?? "");
      return s.running;
    } catch {
      return null;
    }
  }, []);

  useEffect(() => {
    void refreshStatus();
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [refreshStatus]);

  useEffect(() => {
    if (running && !pollRef.current) {
      pollRef.current = setInterval(async () => {
        const stillRunning = await refreshStatus();
        if (stillRunning === false && pollRef.current) {
          clearInterval(pollRef.current);
          pollRef.current = null;
          toast.success("NVD sync completed — scan results will now use fresh CVE data");
        }
      }, 10000);
    }
    if (!running && pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }, [running, refreshStatus]);

  if (user?.role !== "ROLE_ADMIN") return null;

  const { lastStartIndex, totalResults } = position;
  const hasProgress = totalResults > 0 && lastStartIndex > 0;
  const interrupted = !running && hasProgress && lastStartIndex < totalResults;
  const complete = !running && hasProgress && lastStartIndex >= totalResults;

  const handleSync = async () => {
    setStarting(true);
    try {
      const res = await triggerNvdSync();
      if (res.status === "already_running") {
        toast.info("An NVD sync is already running");
      } else {
        toast.success("Full NVD sync started in background");
      }
      setRunning(true);
    } catch (error: any) {
      const status = error?.response?.status;
      toast.error(
        status === 403
          ? "Not allowed: sync requires an admin account"
          : "Failed to start NVD sync",
      );
    } finally {
      setStarting(false);
    }
  };

  return (
    <div className="mb-6 rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          {running ? (
            <LoadingSpinner />
          ) : (
            <CheckCircleIcon className="h-6 w-6 text-green-600" />
          )}
          <div>
            <p className="text-sm font-semibold text-gray-900">
              NVD CVE cache{" "}
              <span
                className={`ml-1 rounded-full px-2 py-0.5 text-xs font-medium ${
                  running
                    ? "bg-amber-100 text-amber-800"
                    : interrupted
                      ? "bg-red-100 text-red-800"
                      : "bg-green-100 text-green-800"
                }`}
              >
                {running
                  ? `syncing… ${lastStartIndex.toLocaleString()} / ${totalResults.toLocaleString()}`
                  : interrupted
                    ? `interrupted at ${lastStartIndex.toLocaleString()} / ${totalResults.toLocaleString()}`
                    : complete
                      ? `complete (${totalResults.toLocaleString()} pages fetched)`
                      : "ready"}
              </span>
            </p>
            <p className="text-xs text-gray-500">
              {running
                ? "A full sync is downloading CVEs in the background — scans keep working meanwhile."
                : "Scans match against the local cache (daily delta 02:00, full refresh Sundays 02:00). Sync now after a fresh deploy."}
            </p>
          </div>
        </div>
        <button
          onClick={handleSync}
          disabled={starting || running}
          className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <ArrowPathIcon className="h-4 w-4" />
          {starting ? "Starting…" : running ? "Syncing…" : "Sync now"}
        </button>
      </div>
      {!running && (
        <p className="mt-2 flex items-center gap-1 text-xs text-amber-700">
          <ExclamationTriangleIcon className="h-4 w-4" />
          {interrupted
            ? "Sync stopped before finishing — press Sync now to resume from the saved position."
            : "Empty results on a fresh install usually mean the cache hasn't been filled yet — run a sync first."}
        </p>
      )}
      {!running && lastError && (
        <p className="mt-1 rounded-md bg-red-50 px-2 py-1 font-mono text-[11px] text-red-800">
          Last failure: {lastError}
        </p>
      )}
    </div>
  );
};

export default NvdSyncPanel;
