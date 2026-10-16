// src/components/common/ThemeToggle.tsx
import React from "react";
import { MoonIcon, SunIcon } from "@heroicons/react/24/outline";
import { useTheme } from "@/hooks/useTheme";

/** Light/dark mode switch. Persisted to localStorage, applied to <html>. */
const ThemeToggle: React.FC<{ className?: string }> = ({ className = "" }) => {
  const { theme, toggle, mounted } = useTheme();

  return (
    <button
      onClick={toggle}
      title={theme === "dark" ? "Switch to light mode" : "Switch to dark mode"}
      aria-label="Toggle color theme"
      className={`flex h-9 w-9 items-center justify-center rounded-full text-gray-500 hover:bg-gray-100 hover:text-gray-700 dark:text-gray-400 dark:hover:bg-gray-800 dark:hover:text-gray-200 ${className}`}
    >
      {!mounted || theme === "light" ? (
        <MoonIcon className="h-5 w-5" />
      ) : (
        <SunIcon className="h-5 w-5" />
      )}
    </button>
  );
};

export default ThemeToggle;
