import type { MetadataRoute } from "next";
import { APP_NAME } from "./lib/version";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `${APP_NAME} - Cattle Management`,
    short_name: APP_NAME,
    description: "Modern digital cattle herd management system",
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "any",
    background_color: "#FAF9F6",
    theme_color: "#1B4332",
    categories: ["business", "productivity"],
    icons: [
      {
        src: "/favicon.svg",
        sizes: "any",
        type: "image/svg+xml",
        purpose: "any",
      },
    ],
  };
}
