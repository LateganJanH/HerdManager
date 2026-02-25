const THEME_KEY = "hm-theme";

export type Theme = "light" | "dark" | "system";

export function getTheme(): Theme {
  if (typeof window === "undefined") return "system";
  try {
    const s = localStorage.getItem(THEME_KEY);
    if (s === "light" || s === "dark" || s === "system") return s;
  } catch (_) {}
  return "system";
}

function getEffectiveDark(): boolean {
  const t = getTheme();
  if (t === "light") return false;
  if (t === "dark") return true;
  return typeof window !== "undefined" && window.matchMedia("(prefers-color-scheme: dark)").matches;
}

export function applyTheme(): void {
  if (typeof document === "undefined") return;
  const dark = getEffectiveDark();
  document.documentElement.classList.toggle("dark", dark);
}

let systemPrefListener: (() => void) | null = null;

function ensureSystemPrefListener(): void {
  if (typeof window === "undefined" || systemPrefListener) return;
  const mq = window.matchMedia("(prefers-color-scheme: dark)");
  const handler = () => {
    if (getTheme() === "system") applyTheme();
  };
  mq.addEventListener("change", handler);
  systemPrefListener = () => {
    mq.removeEventListener("change", handler);
    systemPrefListener = null;
  };
}

export function setTheme(theme: Theme): void {
  try {
    localStorage.setItem(THEME_KEY, theme);
  } catch (_) {}
  applyTheme();
  if (theme === "system") ensureSystemPrefListener();
  try {
    window.dispatchEvent(new CustomEvent("hm-theme-change", { detail: theme }));
  } catch (_) {}
}


/** Subscribe to theme changes (e.g. from menu or Settings). Returns unsubscribe. */
export function subscribeTheme(callback: (theme: Theme) => void): () => void {
  const handler = () => callback(getTheme());
  window.addEventListener("hm-theme-change", handler);
  return () => window.removeEventListener("hm-theme-change", handler);
}
