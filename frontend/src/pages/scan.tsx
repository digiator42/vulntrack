// src/pages/scan.tsx
import React from "react";
import Head from "next/head";
import PublicDependencyScanner from "@/components/public/PublicDependencyScanner";

const PublicScannerPage: React.FC = () => {
  return (
    <>
      <Head>
        <title>Free Dependency Vulnerability Scanner | VulnTrack</title>
        <meta
          name="description"
          content="Scan your project dependencies for security vulnerabilities. Free, fast, and secure. Supports Maven, NPM, and Python."
        />
        <meta
          name="keywords"
          content="dependency scanner, vulnerability scanner, security, CVE, NVD, Maven, NPM, Python"
        />
        <meta
          property="og:title"
          content="Free Dependency Vulnerability Scanner"
        />
        <meta
          property="og:description"
          content="Scan your dependencies for known vulnerabilities"
        />
        <meta name="robots" content="index, follow" />
      </Head>
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 py-8">
        <div className="max-w-6xl mx-auto px-4">
          <PublicDependencyScanner />
        </div>
      </div>
    </>
  );
};

export default PublicScannerPage;
