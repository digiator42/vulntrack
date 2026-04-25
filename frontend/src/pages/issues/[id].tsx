// src/pages/issues/[id].tsx
import React, { useState, useEffect } from "react";
import { useRouter } from "next/router";
import { withAuth } from "@/components/common/ProtectedRoute";
import LoadingSpinner from "@/components/common/LoadingSpinner";
import {
  getIssueById,
  updateIssue,
  patchIssue,
  deleteIssue,
} from "@/services/api";
import { Issue } from "@/types";
import { format } from "date-fns";
import { toast } from "sonner";
import { useAuth } from "@/hooks/useAuth";

const IssueDetailPage: React.FC = () => {
  const router = useRouter();
  const { id } = router.query;
  const { user } = useAuth();
  const [issue, setIssue] = useState<Issue | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Edit form state
  const [editForm, setEditForm] = useState({
    title: "",
    description: "",
    severity: "",
    status: "",
  });

  // Role-based permissions
  const isAdmin = user?.role === "ROLE_ADMIN";
  const isSecurityAdmin = user?.role === "ROLE_SECURITY_ADMIN";
  const isDeveloper = user?.role === "ROLE_DEVELOPER";
  
  const canEdit = isAdmin || isSecurityAdmin;
  const canDelete = isAdmin; // Only admins can delete
  const canChangeStatus = isAdmin || isSecurityAdmin || isDeveloper;
  
  // For developers, check if issue is assigned to them
  const isAssignedToMe = issue?.assignedTo?.id === user?.id;
  const canUpdateStatus = canChangeStatus && (!isDeveloper || isAssignedToMe);

  useEffect(() => {
    if (id) {
      loadIssue();
    }
  }, [id]);

  const loadIssue = async () => {
    try {
      setLoading(true);
      const data = await getIssueById(Number(id));
      
      // Check if developer has access to this issue
      if (isDeveloper && data.assignedTo?.id !== user?.id) {
        toast.error("You don't have access to this issue");
        router.push("/issues");
        return;
      }
      
      setIssue(data);
      setEditForm({
        title: data.title,
        description: data.description || "",
        severity: data.severity,
        status: data.status,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || "Failed to load issue");
      router.push("/issues");
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async () => {
    if (!canEdit) {
      toast.error("You don't have permission to edit this issue");
      return;
    }
    
    try {
      setLoading(true);
      const updated = await updateIssue(Number(id), {
        title: editForm.title,
        description: editForm.description,
        severity: editForm.severity,
      });
      setIssue(updated);
      setIsEditing(false);
      toast.success("Issue updated successfully");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to update issue");
    } finally {
      setLoading(false);
    }
  };

  const handleStatusUpdate = async (newStatus: string) => {
    if (!canUpdateStatus) {
      toast.error("You don't have permission to change status");
      return;
    }
    
    try {
      const updated = await patchIssue(Number(id), { status: newStatus });
      setIssue(updated);
      setEditForm({ ...editForm, status: updated.status });
      toast.success(`Status updated to ${newStatus.replace("_", " ")}`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to update status");
    }
  };

  const handleDelete = async () => {
    if (!canDelete) {
      toast.error("You don't have permission to delete this issue");
      return;
    }
    
    if (
      !confirm(
        "Are you sure you want to delete this issue? This action cannot be undone.",
      )
    ) {
      return;
    }

    try {
      setIsDeleting(true);
      await deleteIssue(Number(id));
      toast.success("Issue deleted successfully");
      router.push("/issues");
    } catch (err: any) {
      toast.error(err.response?.data?.message || "Failed to delete issue");
      setIsDeleting(false);
    }
  };

  const getSeverityColor = (severity: string) => {
    const colors = {
      CRITICAL: "bg-red-100 text-red-800",
      HIGH: "bg-orange-100 text-orange-800",
      MEDIUM: "bg-yellow-100 text-yellow-800",
      LOW: "bg-blue-100 text-blue-800",
    };
    return (
      colors[severity as keyof typeof colors] || "bg-gray-100 text-gray-800"
    );
  };

  const getStatusColor = (status: string) => {
    const colors = {
      OPEN: "bg-red-100 text-red-800",
      IN_PROGRESS: "bg-yellow-100 text-yellow-800",
      RESOLVED: "bg-green-100 text-green-800",
      CLOSED: "bg-gray-400 text-gray-800",
    };
    return colors[status as keyof typeof colors] || "bg-gray-100 text-gray-800";
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner />
      </div>
    );
  }

  if (error || !issue) {
    toast.error(error || "Issue not found");
    router.push("/issues");
    return null;
  }

  if (isEditing) {
    return (
      <div className="max-w-4xl mx-auto py-8">
        <div className="bg-white rounded-lg shadow p-6">
          <h1 className="text-2xl font-bold mb-6">Edit Issue</h1>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Title *
              </label>
              <input
                type="text"
                value={editForm.title}
                onChange={(e) =>
                  setEditForm({ ...editForm, title: e.target.value })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Severity *
              </label>
              <select
                value={editForm.severity}
                onChange={(e) =>
                  setEditForm({ ...editForm, severity: e.target.value })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="CRITICAL">Critical</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                rows={8}
                value={editForm.description}
                onChange={(e) =>
                  setEditForm({ ...editForm, description: e.target.value })
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div className="flex justify-end space-x-3 pt-4">
              <button
                onClick={() => setIsEditing(false)}
                className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={handleUpdate}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto py-8">
      {/* Header */}
      <div className="bg-white rounded-lg shadow mb-6">
        <div className="p-6 border-b border-gray-200">
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <div className="flex items-center gap-3 mb-2">
                <h1 className="text-2xl font-bold text-gray-900">
                  {issue.title}
                </h1>
                <span
                  className={`px-2 py-1 text-xs rounded-full ${getSeverityColor(issue.severity)}`}
                >
                  {issue.severity}
                </span>
              </div>

              <div className="flex items-center gap-4 text-sm text-gray-500">
                <span>Issue #{issue.id}</span>
                <span>•</span>
                <span>
                  Created {format(new Date(issue.createdAt), "MMM d, yyyy")}
                </span>
                {issue.cvssScore && (
                  <>
                    <span>•</span>
                    <span>
                      CVSS Score:{" "}
                      <strong className="text-gray-900">
                        {issue.cvssScore.toFixed(1)}
                      </strong>
                    </span>
                  </>
                )}
              </div>
            </div>

            <div className="flex gap-2">
              {canEdit && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-3 py-1.5 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
                >
                  Edit
                </button>
              )}
              {canDelete && (
                <button
                  onClick={handleDelete}
                  disabled={isDeleting}
                  className="px-3 py-1.5 text-sm text-red-600 border border-red-300 rounded-md hover:bg-red-50 disabled:opacity-50"
                >
                  {isDeleting ? "Deleting..." : "Delete"}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Status Section */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-500 mb-3">Status</h3>
          <div className="flex gap-2">
            {["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"].map((status) => (
              <button
                key={status}
                onClick={() => handleStatusUpdate(status)}
                disabled={!canUpdateStatus || (isDeveloper && !isAssignedToMe)}
                className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
                  issue.status === status
                    ? `${getStatusColor(status)} font-medium`
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                } ${
                  (!canUpdateStatus || (isDeveloper && !isAssignedToMe)) && 
                  "opacity-50 cursor-not-allowed"
                }`}
              >
                {status.replace("_", " ")}
              </button>
            ))}
          </div>
          {isDeveloper && !isAssignedToMe && (
            <p className="text-xs text-gray-500 mt-2">
              You can only change status for issues assigned to you
            </p>
          )}
        </div>

        {/* Assigned To Section - Show for all roles */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-500 mb-3">Assigned To</h3>
          <div className="flex items-center gap-2">
            <span className="text-gray-900">
              {issue.assignedTo?.username || "Unassigned"}
            </span>
            {issue.assignedTo?.role && (
              <span className="text-xs text-gray-500">
                ({issue.assignedTo.role?.replace("ROLE_", "")})
              </span>
            )}
          </div>
        </div>

        {/* Description */}
        <div className="p-6 border-b border-gray-200">
          <h3 className="text-sm font-medium text-gray-500 mb-3">
            Description
          </h3>
          <div className="prose max-w-none">
            {issue.description ? (
              <p className="text-gray-700 whitespace-pre-wrap">
                {issue.description}
              </p>
            ) : (
              <p className="text-gray-400 italic">No description provided</p>
            )}
          </div>
        </div>

        {/* Additional Info */}
        <div className="p-6">
          <h3 className="text-sm font-medium text-gray-500 mb-3">
            Additional Information
          </h3>
          <div className="grid grid-cols-2 gap-4 text-sm">
            {issue.cweId && (
              <div>
                <span className="text-gray-500">CWE ID:</span>
                <span className="ml-2 text-gray-900">{issue.cweId}</span>
              </div>
            )}
            {issue.cvssVector && (
              <div className="col-span-2">
                <span className="text-gray-500">CVSS Vector:</span>
                <code className="ml-2 text-xs bg-gray-100 px-2 py-1 rounded">
                  {issue.cvssVector}
                </code>
              </div>
            )}
            <div>
              <span className="text-gray-500">Created By:</span>
              <span className="ml-2 text-gray-900">
                {issue.owner?.username || "Unknown"}
              </span>
            </div>
            <div>
              <span className="text-gray-500">Last Updated:</span>
              <span className="ml-2 text-gray-900">
                {format(new Date(issue.updatedAt), "MMM d, yyyy h:mm a")}
              </span>
            </div>
          </div>
        </div>

        {/* Back Button */}
        <div className="p-6 border-t border-gray-200 bg-gray-50 rounded-b-lg">
          <button
            onClick={() => router.push("/issues")}
            className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900"
          >
            ← Back to Issues
          </button>
        </div>
      </div>
    </div>
  );
};

// Allow access based on role
export default withAuth(IssueDetailPage, {
  allowedRoles: ["ROLE_ADMIN", "ROLE_SECURITY_ADMIN", "ROLE_DEVELOPER"],
  redirectTo: "/dashboard"
});