// components/common/Layout.tsx
import React, { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import { Toaster } from "sonner";
import { useTheme } from "@/hooks/useTheme";

interface LayoutProps {
  children: ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const { theme } = useTheme();
  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 dark:bg-gray-950 dark:text-gray-100">
      <Navbar />
      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {children}
        <Toaster position="top-right" richColors theme={theme === "dark" ? "dark" : "light"} />
      </main>
    </div>
  );
};

export default Layout;
