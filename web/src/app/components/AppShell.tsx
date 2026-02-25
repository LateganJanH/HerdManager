"use client";

import React, { useState, useEffect } from "react";
import { applyTheme } from "../lib/theme";
import { AppShellContent, type TabId } from "./AppShellContent";

export type { TabId } from "./AppShellContent";

interface AppShellProps {
  children: React.ReactNode;
  currentTab: TabId;
  onTabChange: (tab: TabId) => void;
}

export function AppShell(props: AppShellProps) {
  const { children, currentTab, onTabChange } = props;
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    applyTheme();
  }, []);

  return React.createElement(AppShellContent, {
    children,
    currentTab,
    onTabChange,
    menuOpen,
    setMenuOpen,
  });
}
