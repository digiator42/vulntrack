// components/common/Layout.tsx
import React, { ReactNode } from "react";
import Navbar from "@/components/common/Navbar";
import { Toaster } from "sonner";

interface LayoutProps {
  children: ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />
      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        {children}
        <Toaster position="top-right" richColors />
      </main>
    </div>
  );
};

export default Layout;
