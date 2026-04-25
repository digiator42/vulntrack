// src/pages/_app.tsx
import "@/globals.css";
import type { AppProps } from "next/app";
import Layout from "@/components/common/Layout";
import { useRouter } from "next/router";

export default function App({ Component, pageProps }: AppProps) {
  const router = useRouter();

  // Don't show layout on login and register pages
  const noLayout =
    router.pathname === "/login" || router.pathname === "/register";

  if (noLayout) {
    return <Component {...pageProps} />;
  }

  return (
    <Layout>
      <Component {...pageProps} />
    </Layout>
  );
}
