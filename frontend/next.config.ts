import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  // Required for Docker/Render standalone builds
  output: process.env.NODE_ENV === "production" ? "standalone" : undefined,
  turbopack: {
    root: __dirname,
  },
  async rewrites() {
    // INTERNAL_API_URL is the Docker-internal URL (saas_backend:8080)
    // NEXT_PUBLIC_API_URL is the host-facing URL (localhost:8090) — fallback for local dev without Docker
    const backendUrl =
      process.env.INTERNAL_API_URL ||
      process.env.NEXT_PUBLIC_API_URL ||
      "http://localhost:8090/api/v1";

    return [
      {
        source: "/api/backend/:path*",
        destination: `${backendUrl}/:path*`,
      },
    ];
  },
};

export default nextConfig;
