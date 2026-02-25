import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // Agriculture / farm green palette (award-inspired)
        primary: {
          DEFAULT: "#1B4332",
          light: "#2D6A4F",
          dark: "#081C15",
        },
        // Semantic: warnings (e.g. calving due), success, alerts
        accent: {
          warning: "#E6B800",
          "warning-bg": "rgba(230, 184, 0, 0.15)",
          success: "#40916C",
          alert: "#E07A5F",
          "alert-bg": "rgba(224, 122, 95, 0.15)",
          pregnancy: "#3D5A80",
        },
        // Neutrals for readability and contrast
        stone: {
          50: "#FAF9F6",
          100: "#F5F3EF",
          200: "#E8E6E1",
          300: "#D4D0C8",
          400: "#9C9588",
          500: "#6B6358",
          600: "#4A4339",
          700: "#2F2B24",
          800: "#1E1B16",
          900: "#141210",
        },
      },
      fontFamily: {
        sans: ["var(--font-sans)", "Inter", "system-ui", "sans-serif"],
      },
      fontSize: {
        "readable": ["1rem", { lineHeight: "1.5" }], // 16pt base for readability
        "readable-lg": ["1.125rem", { lineHeight: "1.5" }],
      },
      borderRadius: {
        card: "12px",
        button: "12px",
        input: "12px",
        "card-lg": "16px",
      },
      boxShadow: {
        card: "0 1px 3px 0 rgb(0 0 0 / 0.06), 0 1px 2px -1px rgb(0 0 0 / 0.06)",
        "card-hover": "0 4px 6px -1px rgb(0 0 0 / 0.08), 0 2px 4px -2px rgb(0 0 0 / 0.06)",
      },
      animation: {
        "fade-in": "fadeIn 0.2s ease-out",
      },
      keyframes: {
        fadeIn: {
          "0%": { opacity: "0" },
          "100%": { opacity: "1" },
        },
      },
    },
  },
  plugins: [],
};

export default config;
