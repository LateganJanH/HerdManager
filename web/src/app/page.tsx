"use client";

import { useState, useEffect, useCallback } from "react";
import { useSearchParams, usePathname, useRouter } from "next/navigation";
import { APP_NAME } from "./lib/version";
import { AppShell, type TabId } from "./components/AppShell";
import { SignIn } from "./components/SignIn";
import { DashboardHome } from "./components/DashboardHome";
import { DashboardProfiles } from "./components/DashboardProfiles";
import { DashboardAlerts } from "./components/DashboardAlerts";
import { DashboardAnalytics } from "./components/DashboardAnalytics";
import { DashboardSettings } from "./components/DashboardSettings";
import { onSampleDataChange } from "./lib/mockHerdData";
import { useAuth, isAuthConfigured } from "./lib/useAuth";

const TAB_TITLES: Record<TabId, string> = {
  home: "Home",
  profiles: "Profiles",
  alerts: "Alerts",
  analytics: "Analytics",
  settings: "Settings",
};

const VALID_TABS: TabId[] = ["home", "profiles", "alerts", "analytics", "settings"];
const TAB_BY_DIGIT: Record<string, TabId> = {
  "1": "home",
  "2": "profiles",
  "3": "alerts",
  "4": "analytics",
  "5": "settings",
};

export default function Home() {
  const { user, loading } = useAuth();
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const [currentTab, setCurrentTab] = useState<TabId>("home");
  const [, setRefresh] = useState(0);

  const onTabChange = useCallback(
    (tab: TabId) => {
      setCurrentTab(tab);
      const params = new URLSearchParams(searchParams.toString());
      params.set("tab", tab);
      router.replace(`${pathname}?${params.toString()}`, { scroll: false });
    },
    [pathname, router, searchParams]
  );

  useEffect(() => {
    const tab = searchParams.get("tab");
    if (tab && VALID_TABS.includes(tab as TabId)) {
      setCurrentTab(tab as TabId);
    }
  }, [searchParams]);

  useEffect(() => {
    return onSampleDataChange(() => setRefresh((k) => k + 1));
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const target = e.target as Node | null;
      if (
        !target ||
        !(document.activeElement instanceof HTMLElement) ||
        ["INPUT", "TEXTAREA", "SELECT"].includes(document.activeElement.tagName) ||
        document.activeElement.isContentEditable
      ) {
        return;
      }
      const tab = TAB_BY_DIGIT[e.key];
      if (tab) {
        e.preventDefault();
        onTabChange(tab);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onTabChange]);

  useEffect(() => {
    const title = TAB_TITLES[currentTab];
    document.title = title ? `${title} | ${APP_NAME}` : `${APP_NAME} - Cattle Management`;
  }, [currentTab]);

  useEffect(() => {
    const main = document.getElementById("main-content");
    if (main && typeof (main as HTMLElement).focus === "function") {
      (main as HTMLElement).focus({ preventScroll: true });
    }
  }, [currentTab]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-stone-50 dark:bg-stone-900" aria-busy="true">
        <p className="text-stone-600 dark:text-stone-400">Loading…</p>
      </div>
    );
  }
  if (isAuthConfigured() && !user) {
    return <SignIn />;
  }

  return (
    <AppShell currentTab={currentTab} onTabChange={onTabChange}>
      {currentTab === "home" && <DashboardHome />}
      {currentTab === "profiles" && <DashboardProfiles />}
      {currentTab === "alerts" && <DashboardAlerts />}
      {currentTab === "analytics" && <DashboardAnalytics />}
      {currentTab === "settings" && (
        <DashboardSettings onBack={() => setCurrentTab("home")} />
      )}
    </AppShell>
  );
}
