import type { NextConfig } from "next";

const nextConfig: NextConfig = {
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
      config.resolve.alias = {
        ...config.resolve.alias,
        "playwright": false,
        "playwright-core": false,
        "@playwright/test": false,
      };
    }
    return config;
  },
};

export default nextConfig;
