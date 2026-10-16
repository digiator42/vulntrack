// components/issues/ActivityFeed.tsx
import React, { useState, useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import {
  ClockIcon,
  ArrowPathIcon,
  ChatBubbleLeftIcon,
  UserGroupIcon,
  CheckCircleIcon,
  XCircleIcon,
} from "@heroicons/react/24/outline";
import { formatDistanceToNow } from "date-fns";

import { getActivities } from "@/services/api";

interface Activity {
  id: number;
  action: string;
  oldValue: string;
  newValue: string;
  createdAt: string;
  performedBy: {
    id: number;
    username: string;
    role: string;
  };
}

interface ActivityFeedProps {
  issueId: number;
}

const ActivityFeed: React.FC<ActivityFeedProps> = ({ issueId }) => {
  const { token } = useAuth();
  const [activities, setActivities] = useState<Activity[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchActivities();
  }, [issueId]);

  const fetchActivities = async () => {
    console.log("===> ", token);
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/collaborate/${issueId}/activity`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        },
      );
      const data = await response.json();
      setActivities(data);
    } catch (error) {
      console.error("Failed to fetch activities:", error);
      toast.error("Failed to load activity history");
    } finally {
      setLoading(false);
    }
  };

  const getActivityIcon = (action: string) => {
    const icons = {
      STATUS_CHANGE: <ArrowPathIcon className="h-4 w-4" />,
      COMMENT_ADDED: <ChatBubbleLeftIcon className="h-4 w-4" />,
      ASSIGNED: <UserGroupIcon className="h-4 w-4" />,
      RESOLVED: <CheckCircleIcon className="h-4 w-4" />,
      REOPENED: <XCircleIcon className="h-4 w-4" />,
    };
    return (
      icons[action as keyof typeof icons] || <ClockIcon className="h-4 w-4" />
    );
  };

  const getActivityColor = (action: string) => {
    const colors = {
      STATUS_CHANGE: "bg-yellow-100 text-yellow-600",
      COMMENT_ADDED: "bg-blue-100 text-blue-600",
      ASSIGNED: "bg-purple-100 text-purple-600",
      RESOLVED: "bg-green-100 text-green-600",
      REOPENED: "bg-red-100 text-red-600",
    };
    return colors[action as keyof typeof colors] || "bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400";
  };

  const getActivityText = (activity: Activity) => {
    switch (activity.action) {
      case "STATUS_CHANGE":
        return `changed status from ${activity.oldValue} to ${activity.newValue}`;
      case "COMMENT_ADDED":
        return "added a comment";
      case "ASSIGNED":
        return `assigned to ${activity.newValue}`;
      default:
        return activity.action.toLowerCase().replace("_", " ");
    }
  };

  if (loading) {
    return (
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <div className="flex justify-center items-center h-32">
          <div className="animate-pulse text-gray-400">Loading activity...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800">
        <div className="flex items-center gap-2">
          <ClockIcon className="h-5 w-5 text-gray-600 dark:text-gray-400" />
          <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
            Activity History
          </h3>
        </div>
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
          Track all changes and updates to this issue
        </p>
      </div>

      <div className="max-h-96 overflow-y-auto p-4">
        {activities.length === 0 ? (
          <div className="text-center py-12">
            <ClockIcon className="h-12 w-12 mx-auto text-gray-300 mb-3" />
            <p className="text-gray-500 dark:text-gray-400">No activity yet</p>
          </div>
        ) : (
          <div className="flow-root">
            <ul className="-mb-8">
              {activities.map((activity, idx) => (
                <li key={activity.id}>
                  <div className="relative pb-8">
                    {idx !== activities.length - 1 && (
                      <span
                        className="absolute top-5 left-5 -ml-px h-full w-0.5 bg-gray-200"
                        aria-hidden="true"
                      />
                    )}
                    <div className="relative flex items-start space-x-3">
                      <div className="relative">
                        <div
                          className={`h-10 w-10 rounded-full flex items-center justify-center ring-8 ring-white ${getActivityColor(activity.action)}`}
                        >
                          {getActivityIcon(activity.action)}
                        </div>
                      </div>
                      <div className="min-w-0 flex-1">
                        <div>
                          <div className="text-sm">
                            <span className="font-medium text-gray-900 dark:text-white">
                              {activity.performedBy.username}
                            </span>{" "}
                            <span className="text-gray-500 dark:text-gray-400">
                              {getActivityText(activity)}
                            </span>
                          </div>
                          <p className="mt-0.5 text-xs text-gray-400">
                            {/* {formatDistanceToNow(new Date(activity.createdAt) || new Date(), {
                              addSuffix: true,
                            })} */}
                          </p>
                          <span className="font-small text-gray-900 dark:text-white">
                            {activity.performedBy.role.replace("_", ": ").toLowerCase()}
                          </span>{" "}
                        </div>
                        {activity.oldValue &&
                          activity.newValue &&
                          activity.action !== "STATUS_CHANGE" && (
                            <div className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                              From:{" "}
                              <span className="font-mono">
                                {activity.oldValue}
                              </span>{" "}
                              → To:{" "}
                              <span className="font-mono">
                                {activity.newValue}
                              </span>
                            </div>
                          )}
                      </div>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};

export default ActivityFeed;
