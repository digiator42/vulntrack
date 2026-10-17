// components/issues/IssueChat.tsx
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import {
  PaperAirplaneIcon,
  UserCircleIcon,
  ChatBubbleLeftRightIcon,
  EllipsisHorizontalIcon,
  ArrowsPointingOutIcon,
  XMarkIcon,
  ClipboardDocumentIcon,
} from "@heroicons/react/24/outline";
import { formatDistanceToNow, format } from "date-fns";
import webSocketService from "@/services/websocket";

interface Comment {
  id: number;
  content: string;
  createdAt: string;
  author: {
    id: number;
    username: string;
    role: string;
  };
}

interface IssueChatProps {
  issueId: number;
  onNewComment?: (comment: Comment) => void;
}

const IssueChat: React.FC<IssueChatProps> = ({ issueId, onNewComment }) => {
  const { user } = useAuth();
  let token = localStorage.getItem("token");
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [wsConnected, setWsConnected] = useState(false);
  const [expandedComment, setExpandedComment] = useState<Comment | null>(null);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const chatContainerRef = useRef<HTMLDivElement>(null); // ← ref for the scrollable container

  useEffect(() => {
    // Scroll the container
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop =
        chatContainerRef.current.scrollHeight;
    }
  }, [comments]); // Scroll when messages change

  // Fetch existing comments
  useEffect(() => {
    fetchComments();
  }, [issueId]);

  // Setup WebSocket connection
  useEffect(() => {
    const connectWebSocket = async () => {
      try {
        if (!webSocketService.isConnected()) {
          await webSocketService.connect("");
          setWsConnected(true);
        }

        webSocketService.subscribe(issueId, (message) => {
          if (message.type === "NEW_COMMENT") {
            setComments((prev) => {
              console.log("===> recieved ", prev, message.data);
              const exists = prev.some((c) => c.id === message.data.id);
              if (!exists) {
                return [...prev, message.data];
              }
              return prev;
            });
            onNewComment?.(message.data);
          }
        });
      } catch (error) {
        console.error("WebSocket connection failed:", error);
      }
    };

    connectWebSocket();

    return () => {
      webSocketService.unsubscribe(issueId);
    };
  }, [issueId]);

  const fetchComments = async () => {
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/collaborate/${issueId}/comments`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
        },
      );
      const data = await response.json();
      setComments(data);
    } catch (error) {
      console.error("Failed to fetch comments:", error);
      toast.error("Failed to load comments");
    } finally {
      setLoading(false);
    }
  };

  const sendComment = async () => {
    if (!newComment.trim()) return;

    setSending(true);
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/collaborate/${issueId}/comments?content=${encodeURIComponent(newComment)}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${localStorage.getItem("token")}`,
          },
          body: JSON.stringify({ content: newComment }),
        },
      );

      if (!response.ok) throw new Error("Failed to send comment");

      setNewComment("");
      toast.success("Comment sent");
    } catch (error) {
      console.error("Failed to send comment:", error);
      toast.error("Failed to send comment");
    } finally {
      setSending(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendComment();
    }
  };

  const truncateText = (text: string, maxLength: number = 200) => {
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + "...";
  };

  const handleCommentClick = (e: React.MouseEvent, comment: Comment) => {
    if (comment.content.length <= 200) return;

    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    setMenuPosition({
      x: rect.left + window.scrollX,
      y: rect.bottom + window.scrollY + 5,
    });
    setExpandedComment(comment);
  };

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      toast.success("Copied to clipboard");
    } catch (err) {
      toast.error("Failed to copy");
    }
  };

  // Add this helper function at the top of IssueChat component
  const parseDate = (dateString: string): Date | null => {
    const date = new Date(dateString);
    return isNaN(date.getTime()) ? null : date;
  };

  const formatCommentDate = (dateString: string) => {
    const date = parseDate(dateString);
    if (!date) return "Invalid date";

    const now = new Date();
    const diffInHours = (now.getTime() - date.getTime()) / (1000 * 60 * 60);

    if (diffInHours < 24) {
      return formatDistanceToNow(date, { addSuffix: true });
    } else {
      return format(date, "MMM d, yyyy 'at' h:mm a");
    }
  };

  const getAuthorColor = (role: string) => {
    const colors = {
      ROLE_ADMIN: "text-purple-600 dark:text-purple-400 bg-purple-100 dark:bg-purple-900/40",
      ROLE_SECURITY_ADMIN: "text-blue-600 dark:text-blue-400 bg-blue-100 dark:bg-blue-900/40",
      ROLE_DEVELOPER: "text-green-600 dark:text-green-400 bg-green-100 dark:bg-green-900/40",
    };
    return colors[role as keyof typeof colors] || "text-gray-600 dark:text-gray-400 bg-gray-100 dark:bg-gray-700";
  };

  if (loading) {
    return (
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <div className="flex justify-center items-center h-64">
          <div className="animate-pulse text-gray-400">Loading comments...</div>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="bg-white dark:bg-gray-900 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white dark:from-blue-950 dark:to-gray-900">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ChatBubbleLeftRightIcon className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Discussion
              </h3>
              {wsConnected && (
                <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-700 dark:bg-green-900/40 dark:text-green-300 animate-pulse">
                  ● Live
                </span>
              )}
            </div>
            <span className="text-xs text-gray-500 dark:text-gray-400 bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded-full">
              {comments.length} {comments.length === 1 ? "comment" : "comments"}
            </span>
          </div>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Collaborate with team members
          </p>
        </div>

        {/* Comments List */}
        <div
          ref={chatContainerRef}
          className="h-96 overflow-y-auto p-4 space-y-4 bg-gray-50 dark:bg-gray-800"
        >
          {comments.length === 0 ? (
            <div className="text-center py-12">
              <ChatBubbleLeftRightIcon className="h-12 w-12 mx-auto text-gray-300 dark:text-gray-600 mb-3" />
              <p className="text-gray-500 dark:text-gray-400">No comments yet</p>
              <p className="text-sm text-gray-400 dark:text-gray-500">
                Be the first to start the discussion
              </p>
            </div>
          ) : (
            comments.map((comment) => {
              const isLongComment = comment.content.length > 200;
              const displayText = isLongComment
                ? truncateText(comment.content, 200)
                : comment.content;

              return (
                <div
                  key={comment.id}
                  className="flex gap-3 animate-fade-in group"
                >
                  <div className="flex-shrink-0">
                    <div
                      className={`w-8 h-8 rounded-full flex items-center justify-center ${getAuthorColor(comment.author.role)}`}
                    >
                      <UserCircleIcon className="h-5 w-5" />
                    </div>
                  </div>
                  <div className="flex-1">
                    <div className="bg-white dark:bg-gray-900 rounded-lg p-3 shadow-sm border border-gray-200 dark:border-gray-700 hover:shadow-md transition-shadow">
                      <div className="flex justify-between items-start mb-1">
                        <div className="flex items-center gap-2 flex-wrap">
                          <span className="text-sm font-medium text-gray-900 dark:text-white">
                            {comment.author.username}
                          </span>
                          <span className="text-xs px-1.5 py-0.5 rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400">
                            {comment.author.role.replace("ROLE_", "")}
                          </span>
                        </div>
                        <button
                          onClick={() => copyToClipboard(comment.content)}
                          className="opacity-0 group-hover:opacity-100 transition-opacity text-gray-400 hover:text-gray-600 dark:text-gray-400"
                          title="Copy comment"
                        >
                          <ClipboardDocumentIcon className="h-4 w-4" />
                        </button>
                      </div>

                      {/* Comment Content */}
                      <div
                        className={`text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap ${isLongComment ? "cursor-pointer hover:text-blue-600" : ""}`}
                        onClick={(e) => handleCommentClick(e, comment)}
                      >
                        {displayText}
                        {isLongComment && (
                          <span className="text-blue-500 text-xs ml-1 inline-flex items-center gap-1">
                            <ArrowsPointingOutIcon className="h-3 w-3" />
                            Read more
                          </span>
                        )}
                      </div>

                      {/* Full timestamp on hover */}
                      <div className="text-sm text-gray-400 mt-4">
                        {(() => {
                          const date = new Date(comment.createdAt);
                          if (isNaN(date.getTime())) return "Invalid date";
                          return format(
                            date,
                            "EEEE, MMMM do, yyyy 'at' h:mm:ss a",
                          );
                        })()}
                      </div>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Comment Input */}
        <div className="p-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900">
          <div className="flex gap-2">
            <textarea
              value={newComment}
              onChange={(e) => setNewComment(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Add a comment... (Press Enter to send, Shift+Enter for new line)"
              rows={2}
              className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
            />
            <button
              onClick={sendComment}
              disabled={sending || !newComment.trim()}
              className="self-end px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 transition-colors"
            >
              {sending ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              ) : (
                <PaperAirplaneIcon className="h-5 w-5" />
              )}
            </button>
          </div>
          <p className="text-xs text-gray-400 mt-2">
            {user?.role === "ROLE_DEVELOPER"
              ? "Ask clarifying questions about the vulnerability"
              : "Provide guidance or request additional information"}
          </p>
          <div className="text-right text-xs text-gray-400 mt-1">
            {newComment.length}/2000 characters
          </div>
        </div>
      </div>

      {/* Popup Modal for Full Comment */}
      {expandedComment && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 animate-fade-in"
          onClick={() => setExpandedComment(null)}
        >
          <div
            className="bg-white dark:bg-gray-900 rounded-lg shadow-xl max-w-2xl w-full mx-4 max-h-[80vh] overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 bg-gradient-to-r from-blue-50 to-white flex justify-between items-center">
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${getAuthorColor(expandedComment.author.role)}`}
                >
                  <UserCircleIcon className="h-6 w-6" />
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900 dark:text-white">
                    {expandedComment.author.username}
                  </h3>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {(() => {
                      const date = parseDate(expandedComment.createdAt);
                      return date
                        ? format(date, "MMMM do, yyyy 'at' h:mm:ss a")
                        : "Invalid date";
                    })()}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setExpandedComment(null)}
                className="text-gray-400 hover:text-gray-600 dark:text-gray-400 transition-colors"
              >
                <XMarkIcon className="h-5 w-5" />
              </button>
            </div>
            <div className="p-6 overflow-y-auto max-h-[60vh]">
              <p className="text-gray-700 dark:text-gray-300 whitespace-pre-wrap leading-relaxed">
                {expandedComment.content}
              </p>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 flex justify-end gap-2">
              <button
                onClick={() => copyToClipboard(expandedComment.content)}
                className="px-3 py-1.5 text-sm border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-100 dark:bg-gray-700 dark:hover:bg-gray-700 transition-colors flex items-center gap-2"
              >
                <ClipboardDocumentIcon className="h-4 w-4" />
                Copy
              </button>
              <button
                onClick={() => setExpandedComment(null)}
                className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default IssueChat;
