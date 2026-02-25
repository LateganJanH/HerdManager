"use client";

import { useEffect, useState, useCallback } from "react";
import { getFirebaseAuth, isFirebaseConfigured } from "./firebase";

/** Minimal auth user shape (avoids importing firebase/auth at module load). */
export interface AuthUser {
  uid: string;
  email: string | null;
}

export interface AuthState {
  user: AuthUser | null;
  loading: boolean;
  error: string | null;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (email: string, password: string) => Promise<void>;
  signOut: () => Promise<void>;
  clearError: () => void;
}

export function useAuth(): AuthState {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isFirebaseConfigured()) {
      setLoading(false);
      return;
    }
    let unsub: (() => void) | undefined;
    (async () => {
      const auth = await getFirebaseAuth();
      if (!auth) {
        setLoading(false);
        return;
      }
      const { onAuthStateChanged } = await import("firebase/auth");
      unsub = onAuthStateChanged(auth, (u) => {
        setUser(u ? { uid: u.uid, email: u.email ?? null } : null);
        setLoading(false);
      });
    })();
    return () => {
      unsub?.();
    };
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    setError(null);
    const auth = await getFirebaseAuth();
    if (!auth) {
      setError("Firebase is not configured. Add NEXT_PUBLIC_FIREBASE_* to .env.local.");
      return;
    }
    try {
      const { signInWithEmailAndPassword } = await import("firebase/auth");
      await signInWithEmailAndPassword(auth, email, password);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : "Sign in failed";
      setError(message);
      throw e;
    }
  }, []);

  const signUp = useCallback(async (email: string, password: string) => {
    setError(null);
    const auth = await getFirebaseAuth();
    if (!auth) {
      setError("Firebase is not configured. Add NEXT_PUBLIC_FIREBASE_* to .env.local.");
      return;
    }
    try {
      const { createUserWithEmailAndPassword } = await import("firebase/auth");
      await createUserWithEmailAndPassword(auth, email, password);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : "Sign up failed";
      setError(message);
      throw e;
    }
  }, []);

  const signOut = useCallback(async () => {
    setError(null);
    const auth = await getFirebaseAuth();
    if (auth) {
      const { signOut: firebaseSignOut } = await import("firebase/auth");
      await firebaseSignOut(auth);
    }
  }, []);

  const clearError = useCallback(() => setError(null), []);

  return {
    user,
    loading,
    error,
    signIn,
    signUp,
    signOut,
    clearError,
  };
}

export function isAuthConfigured(): boolean {
  return isFirebaseConfigured();
}
