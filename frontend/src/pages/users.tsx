// src/pages/users.tsx
import React, { useState, useEffect } from "react";
import { withAuth } from "@/components/common/ProtectedRoute";
import { TableShimmer } from "@/components/common/Shimmer";
import { toast } from "sonner";
import {
  UserGroupIcon,
  PencilIcon,
  CheckIcon,
  XMarkIcon,
  UserPlusIcon,
  BriefcaseIcon,
  ArrowPathIcon,
  UserIcon,
  ShieldCheckIcon,
  CpuChipIcon,
} from "@heroicons/react/24/outline";
import {
  getIssues,
  getUsers,
  updateUserRole,
  assignIssue,
  unassignIssue,
} from "@/services/api";

interface User {
  id: number;
  username: string;
  email: string;
  role: "ROLE_ADMIN" | "ROLE_SECURITY_ADMIN" | "ROLE_DEVELOPER";
}

interface Issue {
  id: number;
  title: string;
  severity: string;
  status: string;
  assignedTo?: User;
}

interface Role {
  newRole: String
}

const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [issues, setIssues] = useState<Issue[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingRole, setEditingRole] = useState<number | null>(null);
  const [selectedRole, setSelectedRole] = useState<string>("");
  const [assigningIssue, setAssigningIssue] = useState<number | null>(null);
  const [selectedAssignee, setSelectedAssignee] = useState<number | null>(null);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [currentIssueId, setCurrentIssueId] = useState<number | null>(null);
  const [unassigning, setUnassigning] = useState<number | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const [usersRes, issuesRes] = await Promise.all([
        getUsers(),
        getIssues(0, 100, "createdAt,desc"),
      ]);
      setUsers(usersRes);
      setIssues(issuesRes.content);
    } catch (error) {
      console.error("Failed to load data:", error);
      toast.error("Failed to load users and issues");
    } finally {
      setLoading(false);
    }
  };

  const handleRoleUpdate = async (userId: number, newRole: Role) => {
    try {
      await updateUserRole(userId, newRole);
      setUsers(
        users.map((u) =>
          u.id === userId ? { ...u, role: newRole.newRole as any } : u,
        ),
      );
      toast.success("User role updated successfully");
      setEditingRole(null);
    } catch (error) {
      console.error("Failed to update role:", error);
      toast.error("Failed to update user role");
    }
  };

  const handleAssignIssue = async (
    issueId: number,
    assigneeId: number | null,
  ) => {
    try {
      if (assigneeId) {
        const updated = await assignIssue(issueId, assigneeId);
        setIssues(issues.map((i) => (i.id === issueId ? updated : i)));
        toast.success("Issue assigned successfully");
      } else {
        const updated = await unassignIssue(issueId);
        setIssues(issues.map((i) => (i.id === issueId ? updated : i)));
        toast.success("Issue unassigned successfully");
      }
      setShowAssignModal(false);
      setCurrentIssueId(null);
      setSelectedAssignee(null);
    } catch (error) {
      console.error("Failed to assign issue:", error);
      toast.error("Failed to assign issue");
    }
  };

  const getRoleBadge = (role: string) => {
    const badges = {
      ROLE_ADMIN: "bg-purple-100 text-purple-800",
      ROLE_SECURITY_ADMIN: "bg-blue-100 text-blue-800",
      ROLE_DEVELOPER: "bg-green-100 text-green-800",
    };
    return badges[role as keyof typeof badges] || "bg-gray-100 text-gray-800";
  };

  const getRoleIcon = (role: string) => {
    const icons = {
      ROLE_ADMIN: <ShieldCheckIcon className="h-4 w-4" />,
      ROLE_SECURITY_ADMIN: <UserGroupIcon className="h-4 w-4" />,
      ROLE_DEVELOPER: <CpuChipIcon className="h-4 w-4" />,
    };
    return (
      icons[role as keyof typeof icons] || <UserIcon className="h-4 w-4" />
    );
  };

  const getSeverityBadge = (severity: string) => {
    const badges = {
      CRITICAL: "bg-red-100 text-red-800",
      HIGH: "bg-orange-100 text-orange-800",
      MEDIUM: "bg-yellow-100 text-yellow-800",
      LOW: "bg-blue-100 text-blue-800",
    };
    return (
      badges[severity as keyof typeof badges] || "bg-gray-100 text-gray-800"
    );
  };

  const getStatusBadge = (status: string) => {
    const badges = {
      OPEN: "bg-red-100 text-red-800",
      IN_PROGRESS: "bg-yellow-100 text-yellow-800",
      RESOLVED: "bg-green-100 text-green-800",
      CLOSED: "bg-gray-100 text-gray-800",
    };
    return badges[status as keyof typeof badges] || "bg-gray-100 text-gray-800";
  };

  if (loading) {
    return <TableShimmer />;
  }

  const developers = users.filter((u) => u.role === "ROLE_DEVELOPER");
  const unassignedIssues = issues.filter((i) => !i.assignedTo);
  const assignedIssues = issues.filter((i) => i.assignedTo);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex items-center gap-3">
          <div className="bg-blue-100 rounded-full p-2">
            <UserGroupIcon className="h-6 w-6 text-blue-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              User Management
            </h1>
            <p className="text-gray-600 mt-1">
              Manage user roles and assign vulnerabilities to developers
            </p>
          </div>
        </div>
      </div>

      {/* Users Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
          <h2 className="text-lg font-semibold text-gray-900">System Users</h2>
          <p className="text-sm text-gray-600">
            Manage user roles and permissions
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  User
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Email
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Role
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {users.map((user) => (
                <tr
                  key={user.id}
                  className="hover:bg-gray-50 transition-colors"
                >
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="flex-shrink-0 h-10 w-10">
                        <div className="h-10 w-10 rounded-full bg-gray-200 flex items-center justify-center">
                          {getRoleIcon(user.role)}
                        </div>
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">
                          {user.username}
                        </div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">{user.email}</div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {editingRole === user.id ? (
                      <div className="flex items-center gap-2">
                        <select
                          value={selectedRole}
                          onChange={(e) => setSelectedRole(e.target.value)}
                          className="px-2 py-1 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                          <option value="ROLE_ADMIN">Admin</option>
                          <option value="ROLE_SECURITY_ADMIN">
                            Security Admin
                          </option>
                          <option value="ROLE_DEVELOPER">Developer</option>
                        </select>
                        <button
                          onClick={() =>
                            handleRoleUpdate(user.id, {'newRole': selectedRole})
                          }
                          className="text-green-600 hover:text-green-700"
                        >
                          <CheckIcon className="h-4 w-4" />
                        </button>
                        <button
                          onClick={() => setEditingRole(null)}
                          className="text-red-600 hover:text-red-700"
                        >
                          <XMarkIcon className="h-4 w-4" />
                        </button>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <span
                          className={`px-2 py-1 text-xs rounded-full ${getRoleBadge(user.role)}`}
                        >
                          {user.role.replace("ROLE_", "")}
                        </span>
                        <button
                          onClick={() => {
                            setEditingRole(user.id);
                            setSelectedRole(user.role);
                          }}
                          className="text-gray-400 hover:text-gray-600"
                        >
                          <PencilIcon className="h-4 w-4" />
                        </button>
                      </div>
                    )}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {user.role === "ROLE_DEVELOPER" && (
                      <span className="text-xs text-gray-500">
                        Can be assigned issues
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Issues Assignment Section */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Unassigned Issues */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 bg-yellow-50">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">
                  Unassigned Issues
                </h2>
                <p className="text-sm text-gray-600">
                  Assign these to developers
                </p>
              </div>
              <span className="px-2 py-1 text-xs rounded-full bg-yellow-200 text-yellow-800">
                {unassignedIssues.length} pending
              </span>
            </div>
          </div>
          <div className="divide-y divide-gray-200 max-h-96 overflow-y-auto">
            {unassignedIssues.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <BriefcaseIcon className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                <p>All issues are assigned</p>
              </div>
            ) : (
              unassignedIssues.map((issue) => (
                <div
                  key={issue.id}
                  className="p-4 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <h3 className="text-sm font-medium text-gray-900">
                          #{issue.id}
                        </h3>
                        <span
                          className={`px-2 py-0.5 text-xs rounded-full ${getSeverityBadge(issue.severity)}`}
                        >
                          {issue.severity}
                        </span>
                        <span
                          className={`px-2 py-0.5 text-xs rounded-full ${getStatusBadge(issue.status)}`}
                        >
                          {issue.status}
                        </span>
                      </div>
                      <p className="text-sm text-gray-700">{issue.title}</p>
                    </div>
                    <button
                      onClick={() => {
                        setCurrentIssueId(issue.id);
                        setShowAssignModal(true);
                      }}
                      className="ml-4 px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
                    >
                      Assign
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Assigned Issues by Developer */}
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-200 bg-green-50">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-gray-900">
                  Assigned Issues
                </h2>
                <p className="text-sm text-gray-600">Grouped by developer</p>
              </div>
              <span className="px-2 py-1 text-xs rounded-full bg-green-200 text-green-800">
                {assignedIssues.length} assigned
              </span>
            </div>
          </div>
          <div className="divide-y divide-gray-200 max-h-96 overflow-y-auto">
            {assignedIssues.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <UserGroupIcon className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                <p>No issues assigned yet</p>
              </div>
            ) : (
              developers.map((dev) => {
                const devIssues = assignedIssues.filter(
                  (i) => i.assignedTo?.id === dev.id,
                );
                if (devIssues.length === 0) return null;
                return (
                  <div key={dev.id} className="p-4">
                    <div className="flex items-center gap-2 mb-3">
                      <div className="bg-green-100 rounded-full p-1">
                        <CpuChipIcon className="h-4 w-4 text-green-600" />
                      </div>
                      <span className="font-medium text-gray-900">
                        {dev.username}
                      </span>
                      <span className="text-xs text-gray-500">
                        ({devIssues.length} issues)
                      </span>
                    </div>
                    <div className="space-y-2 pl-6">
                      {devIssues.map((issue) => (
                        <div
                          key={issue.id}
                          className="flex justify-between items-center p-2 bg-gray-50 rounded-lg"
                        >
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="text-xs text-gray-500">
                                #{issue.id}
                              </span>
                              <span
                                className={`px-2 py-0.5 text-xs rounded-full ${getSeverityBadge(issue.severity)}`}
                              >
                                {issue.severity}
                              </span>
                              <span className="text-sm text-gray-700 truncate">
                                {issue.title}
                              </span>
                            </div>
                          </div>
                          <button
                            onClick={async () => {
                              if (
                                confirm(
                                  `Unassign issue #${issue.id} from ${dev.username}?`,
                                )
                              ) {
                                await handleAssignIssue(issue.id, null);
                              }
                            }}
                            disabled={unassigning === issue.id}
                            className="ml-2 text-xs text-red-600 hover:text-red-700"
                          >
                            Unassign
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* Assign Modal */}
      {showAssignModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl max-w-md w-full mx-4">
            <div className="px-6 py-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold text-gray-900">
                Assign Issue
              </h3>
              <p className="text-sm text-gray-600">
                Select a developer to assign this issue
              </p>
            </div>
            <div className="p-6">
              <select
                value={selectedAssignee || ""}
                onChange={(e) => setSelectedAssignee(Number(e.target.value))}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="">Select a developer...</option>
                {developers.map((dev) => (
                  <option key={dev.id} value={dev.id}>
                    {dev.username} ({dev.email})
                  </option>
                ))}
              </select>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 flex justify-end gap-3">
              <button
                onClick={() => {
                  setShowAssignModal(false);
                  setCurrentIssueId(null);
                  setSelectedAssignee(null);
                }}
                className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50"
              >
                Cancel
              </button>
              <button
                onClick={() =>
                  handleAssignIssue(currentIssueId!, selectedAssignee!)
                }
                disabled={!selectedAssignee}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                Assign Issue
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

// Only ADMIN can access users page
export default withAuth(UsersPage, {
  allowedRoles: ["ROLE_ADMIN"],
  redirectTo: "/dashboard",
});
