"use client";

import React from "react";

export function ShellRoot({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return React.createElement("div", { className }, children);
}
