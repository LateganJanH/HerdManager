"use client";

import { useEffect, useRef, useState } from "react";
import { APP_NAME } from "../lib/version";
import { getTheme, setTheme } from "../lib/theme";
import { ShellRoot } from "./ShellRoot";

export type TabId = "home" | "profiles" | "alerts" | "analytics" | "settings";

const tabs = [
  { id: "home" as const, label: "Home", icon: "⌂" },
  { id: "profiles" as const, label: "Profiles", icon: "☰" },
  { id: "alerts" as const, label: "Alerts", icon: "!" },
  { id: "analytics" as const, label: "Analytics", icon: "▣" },
];

export interface AppShellContentProps {
  children: React.ReactNode;
  currentTab: TabId;
  onTabChange: (tab: TabId) => void;
  menuOpen: boolean;
  setMenuOpen: (value: boolean) => void;
}

export function AppShellContent({
  children,
  currentTab,
  onTabChange,
  menuOpen,
  setMenuOpen,
}: AppShellContentProps) {
  const menuRef = useRef<HTMLElement>(null);
  const mainRef = useRef<HTMLElement>(null);
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);
  const [showScrollTop, setShowScrollTop] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [menuOpen, setMenuOpen]);

  useEffect(() => {
    if (menuOpen && menuRef.current) {
      const first = menuRef.current.querySelector<HTMLButtonElement>("button");
      first?.focus();
    }
  }, [menuOpen]);

  useEffect(() => {
    const main = mainRef.current;
    if (!main) return;
    const onScroll = () => setShowScrollTop(main.scrollTop > 400);
    main.addEventListener("scroll", onScroll, { passive: true });
    return () => main.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    const focusMainIfSkipTarget = () => {
      if (window.location.hash === "#main-content" && mainRef.current) {
        mainRef.current.focus();
      }
    };
    focusMainIfSkipTarget();
    window.addEventListener("hashchange", focusMainIfSkipTarget);
    return () => window.removeEventListener("hashchange", focusMainIfSkipTarget);
  }, []);

  return (
    <ShellRoot className="min-h-screen flex flex-col bg-stone-50 dark:bg-stone-900">
      <a
        href="#main-content"
        className="skip-link"
      >
        Skip to main content
      </a>
      <header className="sticky top-0 z-10 border-b border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-800 shadow-card print:hidden">
        <div className="mx-auto max-w-7xl px-4 h-14 flex items-center justify-between sm:px-6 lg:px-8">
          <h1 className="text-xl font-bold text-primary">{APP_NAME}</h1>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setMenuOpen(!menuOpen)}
              className="touch-target inline-flex items-center justify-center rounded-button p-2 text-stone-600 dark:text-stone-300 hover:bg-stone-100 dark:hover:bg-stone-700"
              aria-label="Open menu"
            >
              <span className="text-xl" aria-hidden>☰</span>
            </button>
            {menuOpen && (
              <>
                <div
                  className="fixed inset-0 z-10"
                  aria-hidden
                  onClick={() => setMenuOpen(false)}
                />
                <nav
                  ref={menuRef}
                  className="absolute right-4 top-14 z-20 w-56 rounded-card-lg bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 shadow-card-hover py-2 animate-fade-in"
                  aria-label="Settings menu"
                >
                  <button
                    type="button"
                    className="w-full text-left px-4 py-3 text-base text-stone-700 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700"
                    onClick={() => {
                      setMenuOpen(false);
                      onTabChange("settings");
                    }}
                  >
                    Settings
                  </button>
                  <button
                    type="button"
                    className="w-full text-left px-4 py-3 text-base text-stone-700 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700 flex items-center justify-between"
                    onClick={async () => {
                      try {
                        await navigator.clipboard.writeText(window.location.href);
                        setCopyFeedback("Copied!");
                        setTimeout(() => setCopyFeedback(null), 2000);
                      } catch (_) {
                        setCopyFeedback("Copy failed");
                        setTimeout(() => setCopyFeedback(null), 2000);
                      }
                    }}
                  >
                    <span>Copy link</span>
                    {copyFeedback && (
                      <span className="text-xs text-primary font-medium" aria-live="polite">
                        {copyFeedback}
                      </span>
                    )}
                  </button>
                  <button
                    type="button"
                    className="w-full text-left px-4 py-3 text-base text-stone-700 dark:text-stone-200 hover:bg-stone-100 dark:hover:bg-stone-700"
                    onClick={() => {
                      setMenuOpen(false);
                      const next = getTheme() === "dark" ? "light" : "dark";
                      setTheme(next);
                    }}
                  >
                    Toggle dark mode
                  </button>
                </nav>
              </>
            )}
          </div>
        </div>
      </header>

      <main ref={mainRef} id="main-content" className="flex-1 overflow-auto pb-20 sm:pb-6 print:pb-0" tabIndex={-1}>{children}</main>

      {showScrollTop && (
        <button
          type="button"
          onClick={() => mainRef.current?.scrollTo({ top: 0, behavior: "smooth" })}
          className="fixed bottom-20 right-4 sm:bottom-6 sm:right-6 z-10 rounded-full bg-primary text-white p-3 shadow-card hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 print:hidden"
          aria-label="Scroll to top"
        >
          <span aria-hidden>↑</span>
        </button>
      )}

      <nav
        className="fixed bottom-0 left-0 right-0 z-10 border-t border-stone-200 dark:border-stone-700 bg-white dark:bg-stone-800 sm:relative sm:border-0 sm:bg-stone-100 dark:sm:bg-stone-800/50 print:hidden"
        aria-label="Main navigation"
      >
        <div className="mx-auto max-w-7xl flex justify-around sm:justify-start sm:gap-1 sm:px-4 sm:py-2">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => onTabChange(tab.id)}
              className={
                currentTab === tab.id
                  ? "touch-target flex flex-col items-center justify-center gap-0.5 flex-1 py-2 px-3 min-w-0 rounded-button text-sm font-medium transition-colors text-primary bg-primary/10 dark:bg-primary/20"
                  : "touch-target flex flex-col items-center justify-center gap-0.5 flex-1 py-2 px-3 min-w-0 rounded-button text-sm font-medium transition-colors text-stone-600 dark:text-stone-400 hover:bg-stone-100 dark:hover:bg-stone-700"
              }
              aria-current={currentTab === tab.id ? "page" : undefined}
            >
              <span className="text-lg" aria-hidden>{tab.icon}</span>
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </nav>
    </ShellRoot>
  );
}
