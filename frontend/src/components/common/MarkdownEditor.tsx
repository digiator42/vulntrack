// components/common/MarkdownEditor.tsx
import React, { useState } from "react";
import ReactMarkdown from "react-markdown";
import {
  EyeIcon,
  PencilIcon,
  InformationCircleIcon,
} from "@heroicons/react/24/outline";
import MarkdownRenderer from "./MarkdownRenderer";

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  label?: string;
  helpText?: string;
}

const MarkdownEditor: React.FC<MarkdownEditorProps> = ({
  value,
  onChange,
  placeholder = "Write your description using Markdown...",
  rows = 10,
  label,
  helpText,
}) => {
  const [mode, setMode] = useState<"edit" | "preview">("edit");

  const markdownHelp = `# Markdown Guide

## Text Formatting
- **Bold**: **text** or __text__
- *Italic*: *text* or _text_
- ~~Strikethrough~~: ~~text~~

## Lists
- Unordered: - item or * item
1. Ordered: 1. item

## Tables
| Header 1 | Header 2 |
|----------|----------|
| Cell 1   | Cell 2   |

## Code
\`inline code\` or \`\`\`code block\`\`\`

## Links
[Link text](https://example.com)

## Headers
# H1, ## H2, ### H3

> Blockquote
`;

  const [showHelp, setShowHelp] = useState(false);

  return (
    <div className="space-y-2">
      {label && (
        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
          {label}
        </label>
      )}

      {/* Toolbar */}
      <div className="flex justify-between items-center border-b border-gray-200 dark:border-gray-700 pb-2">
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => setMode("edit")}
            className={`px-3 py-1 text-sm rounded-md transition-colors ${
              mode === "edit"
                ? "bg-blue-600 text-white"
                : "bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-200"
            }`}
          >
            <PencilIcon className="h-4 w-4 inline mr-1" />
            Write
          </button>
          <button
            type="button"
            onClick={() => setMode("preview")}
            className={`px-3 py-1 text-sm rounded-md transition-colors ${
              mode === "preview"
                ? "bg-blue-600 text-white"
                : "bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 hover:bg-gray-200"
            }`}
          >
            <EyeIcon className="h-4 w-4 inline mr-1" />
            Preview
          </button>
        </div>

        <button
          type="button"
          onClick={() => setShowHelp(!showHelp)}
          className="text-gray-400 hover:text-gray-600 dark:text-gray-400"
        >
          <InformationCircleIcon className="h-5 w-5" />
        </button>
      </div>

      {/* Help Panel */}
      {showHelp && (
        <div className="bg-blue-50 dark:bg-blue-900/25 border border-blue-200 dark:border-blue-800 rounded-md p-4 text-sm">
          <div className="flex justify-between items-start mb-2">
            <h4 className="font-semibold text-blue-900 dark:text-blue-200">Markdown Guide</h4>
            <button
              type="button"
              onClick={() => setShowHelp(false)}
              className="text-blue-400 hover:text-blue-600 dark:text-blue-400"
            >
              ×
            </button>
          </div>
          <div className="prose prose-sm max-w-none">
            <ReactMarkdown>{markdownHelp}</ReactMarkdown>
          </div>
        </div>
      )}

      {/* Editor / Preview */}
      {mode === "edit" ? (
        <textarea
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          rows={rows}
          className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
        />
      ) : (
        <div className="min-h-[300px] max-h-[500px] overflow-y-auto border border-gray-300 dark:border-gray-600 rounded-md p-4 bg-gray-50 dark:bg-gray-800">
          <MarkdownRenderer content={value || "*No content*"} />
        </div>
      )}

      {helpText && <p className="text-xs text-gray-500 dark:text-gray-400">{helpText}</p>}
    </div>
  );
};

export default MarkdownEditor;
