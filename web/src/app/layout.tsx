import type { Metadata, Viewport } from "next";
import dynamic from "next/dynamic";
import { Inter } from "next/font/google";
import { APP_NAME } from "./lib/version";
import "./globals.css";

// Load Providers in a separate chunk to keep layout chunk small and avoid ChunkLoadError/timeout
const Providers = dynamic(
  () => import("./providers").then((m) => ({ default: m.Providers })),
  { ssr: true }
);

const inter = Inter({ subsets: ["latin"] });

const baseUrl = process.env.NEXT_PUBLIC_BASE_URL ?? "https://herdmanager.example.com";
const title = `${APP_NAME} - Cattle Management`;

export const metadata: Metadata = {
  metadataBase: new URL(baseUrl),
  title,
  description: "Modern digital cattle herd management system",
  icons: {
    icon: [
      { url: "/favicon.svg", type: "image/svg+xml" },
      { url: "/herdmanager-logo.svg", type: "image/svg+xml" },
    ],
  },
  openGraph: {
    title,
    description: "Modern digital cattle herd management system",
    type: "website",
    url: baseUrl,
  },
  twitter: {
    card: "summary",
    title,
    description: "Modern digital cattle herd management system",
  },
};

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: light)", color: "#FAF9F6" },
    { media: "(prefers-color-scheme: dark)", color: "#0D0C0A" },
  ],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const themeScript = `
(function(){
  try {
    var s = localStorage.getItem('hm-theme');
    var dark = s === 'dark' || (s !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    document.documentElement.classList.toggle('dark', !!dark);
  } catch (_) {}
})();
`;
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeScript }} />
      </head>
      <body className={inter.className}>
        <noscript>
          <div style={{ padding: "2rem", textAlign: "center", fontFamily: "system-ui, sans-serif" }}>
            <p>{APP_NAME} needs JavaScript to run.</p>
            <p><a href="/">Go to dashboard</a></p>
          </div>
        </noscript>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
