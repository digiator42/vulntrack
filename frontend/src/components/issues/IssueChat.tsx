// components/issues/IssueChat.tsx
import React, { useState, useEffect, useRef } from "react";
import { useAuth } from "@/hooks/useAuth";
import { toast } from "sonner";
import {
  PaperAirplaneIcon,
  UserCircleIcon,
  ChatBubbleLeftRightIcon,
} from "@heroicons/react/24/outline";
import { formatDistanceToNow } from "date-fns";
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
  let token = localStorage.getItem('token');
  const [comments, setComments] = useState<Comment[]>([]);
  const [newComment, setNewComment] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [wsConnected, setWsConnected] = useState(false);

  // Fetch existing comments
  useEffect(() => {
    fetchComments();
  }, [issueId]);

  // Setup WebSocket connection
  useEffect(() => {

    const connectWebSocket = async () => {
      try {
        console.log("SENDING WS HANDSHAKE?");
        if (!webSocketService.isConnected()) {
          console.log("SENDING WS HANDSHAKE");
          await webSocketService.connect("");
          setWsConnected(true);
        }

        // Subscribe to real-time comments
        webSocketService.subscribe(issueId, (message) => {
          if (message.type === "NEW_COMMENT") {
            setComments((prev) => {
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

  // Auto-scroll to bottom
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [comments]);

  const fetchComments = async () => {
    try {
      const response = await fetch(
        `http://localhost:8080/api/collaborate/${issueId}/comments`,
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
        `http://localhost:8080/api/collaborate/${issueId}/comments?content=${newComment}`,
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

  const getAuthorColor = (role: string) => {
    const colors = {
      ROLE_ADMIN: "text-purple-600 bg-purple-100",
      ROLE_SECURITY_ADMIN: "text-blue-600 bg-blue-100",
      ROLE_DEVELOPER: "text-green-600 bg-green-100",
    };
    return colors[role as keyof typeof colors] || "text-gray-600 bg-gray-100";
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="flex justify-center items-center h-64">
          <div className="animate-pulse text-gray-400">Loading comments...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
        <div className="flex items-center gap-2">
          <ChatBubbleLeftRightIcon className="h-5 w-5 text-blue-600" />
          <h3 className="text-lg font-semibold text-gray-900">Discussion</h3>
          {wsConnected && (
            <span className="ml-2 px-2 py-0.5 text-xs rounded-full bg-green-100 text-green-700">
              Live
            </span>
          )}
        </div>
        <p className="text-sm text-gray-600 mt-1">
          Collaborate with team members on this vulnerability
        </p>
      </div>

      {/* Comments List */}
      <div className="h-96 overflow-y-auto p-4 space-y-4 bg-gray-50">
        {comments.length === 0 ? (
          <div className="text-center py-12">
            <ChatBubbleLeftRightIcon className="h-12 w-12 mx-auto text-gray-300 mb-3" />
            <p className="text-gray-500">No comments yet</p>
            <p className="text-sm text-gray-400">
              Be the first to start the discussion
            </p>
          </div>
        ) : (
          comments.map((comment) => (
            <div key={comment.id} className="flex gap-3 animate-fade-in">
              <div className="flex-shrink-0">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center ${getAuthorColor(comment.author.role)}`}
                >
                  <UserCircleIcon className="h-5 w-5" />
                </div>
              </div>
              <div className="flex-1">
                <div className="bg-white rounded-lg p-3 shadow-sm border border-gray-200">
                  <div className="flex justify-between items-start mb-1">
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-900">
                        {comment.author.username}
                      </span>
                      <span className="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                        {comment.author.role.replace("ROLE_", "")}
                      </span>
                    </div>
                    <span className="text-xs text-gray-400">
                      {/* {formatDistanceToNow(new Date(comment.createdAt), {
                        addSuffix: true,
                      })} */}
                    </span>
                  </div>
                  <p className="text-sm text-gray-700 whitespace-pre-wrap">
                    {comment.content}
                  </p>
                </div>
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Comment Input */}
      <div className="p-4 border-t border-gray-200 bg-white">
        <div className="flex gap-2">
          <textarea
            value={newComment}
            onChange={(e) => setNewComment(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="Add a comment... (Press Enter to send)"
            rows={2}
            className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
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
      </div>
    </div>
  );
};

export default IssueChat;
