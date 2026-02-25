"use client";

import { useState } from "react";
import { useAuth } from "../lib/useAuth";
import { APP_NAME } from "../lib/version";

export function SignIn() {
  const { signIn, signUp, error, clearError } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || password.length < 6) return;
    setSubmitting(true);
    clearError();
    try {
      await signIn(email, password);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim() || password.length < 6) return;
    setSubmitting(true);
    clearError();
    try {
      await signUp(email, password);
    } finally {
      setSubmitting(false);
    }
  };

  const canSubmit = email.trim().length > 0 && password.length >= 6;

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-stone-50 dark:bg-stone-900 px-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-stone-900 dark:text-stone-100">
            {APP_NAME}
          </h1>
          <p className="mt-2 text-stone-600 dark:text-stone-400">
            Sign in to view your herd dashboard
          </p>
        </div>

        <form className="space-y-4" onSubmit={handleSignIn}>
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Email
            </label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); clearError(); }}
              disabled={submitting}
              className="mt-1 block w-full rounded-lg border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100 focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Password
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              value={password}
              onChange={(e) => { setPassword(e.target.value); clearError(); }}
              disabled={submitting}
              minLength={6}
              className="mt-1 block w-full rounded-lg border border-stone-300 dark:border-stone-600 bg-white dark:bg-stone-800 px-3 py-2 text-stone-900 dark:text-stone-100 focus:outline-none focus:ring-2 focus:ring-primary"
            />
            {password.length > 0 && password.length < 6 && (
              <p className="mt-1 text-sm text-amber-600 dark:text-amber-400">
                Use at least 6 characters
              </p>
            )}
          </div>

          {error && (
            <p className="text-sm text-red-600 dark:text-red-400" role="alert">
              {error}
            </p>
          )}

          <div className="flex flex-col gap-2">
            <button
              type="submit"
              onClick={handleSignIn}
              disabled={submitting || !canSubmit}
              className="w-full rounded-lg bg-primary px-4 py-2.5 font-medium text-white hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none"
            >
              {submitting ? "Signing in…" : "Sign in"}
            </button>
            <button
              type="button"
              onClick={handleSignUp}
              disabled={submitting || !canSubmit}
              className="w-full rounded-lg border border-stone-300 dark:border-stone-600 bg-stone-100 dark:bg-stone-800 px-4 py-2.5 font-medium text-stone-800 dark:text-stone-200 hover:bg-stone-200 dark:hover:bg-stone-700 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none"
            >
              Create account
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
