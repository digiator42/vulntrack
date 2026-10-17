// components/common/MarkdownRenderer.tsx
import React from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import rehypeRaw from "rehype-raw";
import rehypeSanitize from "rehype-sanitize";
import { Prism as SyntaxHighlighter } from "react-syntax-highlighter";
import {
  vscDarkPlus,
  atomDark,
  coy,
  dracula,
  materialDark,
  oneDark,
} from "react-syntax-highlighter/dist/esm/styles/prism";

interface MarkdownRendererProps {
  content: string;
  className?: string;
  codeTheme?:
    | "vscDarkPlus"
    | "atomDark"
    | "coy"
    | "dracula"
    | "materialDark"
    | "oneDark";
}

const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({
  content,
  className = "",
  codeTheme = "oneDark",
}) => {
  if (!content) {
    return <p className="text-gray-400 italic">No description provided</p>;
  }

  const getCodeTheme = () => {
    switch (codeTheme) {
      case "vscDarkPlus":
        return vscDarkPlus;
      case "atomDark":
        return atomDark;
      case "coy":
        return coy;
      case "dracula":
        return dracula;
      case "materialDark":
        return materialDark;
      case "oneDark":
      default:
        return dracula;
    }
  };

  return (
    <div
      className={`prose prose-sm max-w-none prose-headings:text-gray-900 dark:text-white prose-headings:font-semibold prose-p:text-gray-700 dark:text-gray-300 prose-strong:text-gray-900 dark:text-white prose-table:border prose-table:border-gray-200 dark:border-gray-700 prose-th:bg-gray-50 dark:bg-gray-800 prose-th:p-2 prose-td:p-2 prose-td:border prose-td:border-gray-200 dark:border-gray-700 prose-code:bg-gray-100 dark:bg-gray-700 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-sm prose-code:font-mono prose-pre:bg-transparent prose-pre:p-0 ${className}`}
    >
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeRaw, rehypeSanitize]}
        components={{
          // Code blocks with syntax highlighting
          code({ node, inline, className, children, ...props }: any) {
            const match = /language-(\w+)/.exec(className || "");
            const language = match ? match[1] : "";

            // For inline code
            if (inline) {
              return (
                <code
                  className="bg-gray-100 dark:bg-gray-700 px-1.5 py-0.5 rounded text-sm font-mono text-red-600"
                  {...props}
                >
                  {children}
                </code>
              );
            }

            // For code blocks with language
            if (match) {
              return (
                <div className="relative group my-4">
                  {/* Language label */}
                  <div className="absolute top-0 right-0 bg-gray-700 text-gray-300 text-xs px-2 py-1 rounded-bl-md rounded-tr-md opacity-0 group-hover:opacity-100 transition-opacity z-10">
                    {language}
                  </div>
                  <SyntaxHighlighter
                    style={getCodeTheme()}
                    language={language}
                    PreTag="div"
                    customStyle={{
                      margin: 0,
                      borderRadius: "0.5rem",
                      fontSize: "0.875rem",
                    }}
                    {...props}
                  >
                    {String(children).replace(/\n$/, "")}
                  </SyntaxHighlighter>
                </div>
              );
            }

            // For code blocks without language specified
            return (
              <div className="my-4">
                <SyntaxHighlighter
                  style={getCodeTheme()}
                  language="text"
                  PreTag="div"
                  customStyle={{
                    margin: 0,
                    borderRadius: "0.5rem",
                    fontSize: "0.575rem",
                  }}
                  {...props}
                >
                  {String(children).replace(/\n$/, "")}
                </SyntaxHighlighter>
              </div>
            );
          },

          // Tables
          table({ children }) {
            return (
              <div className="overflow-x-auto my-4">
                <table className="min-w-full border-collapse border border-gray-200 dark:border-gray-700">
                  {children}
                </table>
              </div>
            );
          },

          th({ children }) {
            return (
              <th className="bg-gray-50 dark:bg-gray-800 px-3 py-2 text-left text-sm font-semibold text-gray-900 dark:text-white border border-gray-200 dark:border-gray-700">
                {children}
              </th>
            );
          },

          td({ children }) {
            return (
              <td className="px-3 py-2 text-sm text-gray-700 dark:text-gray-300 border border-gray-200 dark:border-gray-700">
                {children}
              </td>
            );
          },

          // Links
          a({ href, children }) {
            return (
              <a
                href={href}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:text-blue-300 dark:hover:text-blue-300 underline"
              >
                {children}
              </a>
            );
          },

          // Headers
          h1({ children }) {
            return (
              <h1 className="text-2xl font-bold mt-6 mb-4 pb-2 border-b border-gray-200 dark:border-gray-700">
                {children}
              </h1>
            );
          },

          h2({ children }) {
            return (
              <h2 className="text-xl font-semibold mt-5 mb-3 pb-1 border-b border-gray-200 dark:border-gray-700">
                {children}
              </h2>
            );
          },

          h3({ children }) {
            return (
              <h3 className="text-lg font-semibold mt-4 mb-2">{children}</h3>
            );
          },

          h4({ children }) {
            return (
              <h4 className="text-base font-semibold mt-3 mb-2">{children}</h4>
            );
          },

          // Lists
          ul({ children }) {
            return (
              <ul className="list-disc pl-6 my-3 space-y-1">{children}</ul>
            );
          },

          ol({ children }) {
            return (
              <ol className="list-decimal pl-6 my-3 space-y-1">{children}</ol>
            );
          },

          li({ children }) {
            return <li className="text-gray-700 dark:text-gray-300">{children}</li>;
          },

          // Blockquotes
          blockquote({ children }) {
            return (
              <blockquote className="border-l-4 border-blue-400 pl-4 py-2 my-4 bg-blue-50 dark:bg-blue-900/25 rounded-r-md">
                {children}
              </blockquote>
            );
          },

          // Horizontal rule
          hr() {
            return <hr className="my-6 border-gray-200 dark:border-gray-700" />;
          },

          // Images
          img({ src, alt }) {
            return (
              <img
                src={src}
                alt={alt}
                className="max-w-full h-auto rounded-lg shadow-md my-4"
                loading="lazy"
              />
            );
          },
        }}
      >
        {content}
      </ReactMarkdown>
    </div>
  );
};

export default MarkdownRenderer;
