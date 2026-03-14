import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Reduce client chunk size; can help avoid ChunkLoadError/timeout in dev
  experimental: {
    optimizePackageImports: ["@tanstack/react-query"],
  },
  async headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
  webpack: (config, { isServer }) => {
    // Prevent Playwright (E2E devDependency) from being pulled into client bundle
    // (e.g. by pnpm’s node_modules layout or Next.js context).
    if (!isServer) {
      const path = require("path");
      const emptyPath = path.join(__dirname, "src", "app", "lib", "emptyModule.js");
      config.resolve.alias = {
        ...config.resolve.alias,
        "playwright": emptyPath,
        "playwright-core": emptyPath,
        "@playwright/test": emptyPath,
      };
    }
    return config;
  },
};

export default nextConfig;
