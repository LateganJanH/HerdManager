"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import { getMockHerdStats, type HerdStats } from "./mockHerdData";
import { isValidStats } from "./herdStatsValidation";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchHerdStatsFromFirestore } from "./firestoreStats";

const HERD_STATS_QUERY_KEY = "herd-stats-firestore";

async function fetchHerdStatsFromApi(): Promise<HerdStats | null> {
  const res = await fetch("/api/stats");
  if (!res.ok) return null;
  const data: unknown = await res.json();
  return isValidStats(data) ? data : null;
}

/**
 * Fetches herd stats. When the user is signed in and Firebase is configured,
 * reads from Firestore (same data as Android sync) and subscribes for real-time updates.
 * Otherwise uses /api/stats or mock.
 */
export function useHerdStats(): {
  stats: HerdStats;
  fromApi: boolean;
  loading: boolean;
  isError: boolean;
  dataUpdatedAt: number;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured());
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<Array<() => void>>([]);

  const query = useQuery({
    queryKey: useFirestore ? [HERD_STATS_QUERY_KEY, uid] : ["herd-stats"],
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return fetchHerdStatsFromApi();
          return fetchHerdStatsFromFirestore(db, uid);
        }
      : fetchHerdStatsFromApi,
    staleTime: 30_000,
    retry: 2,
    retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10_000),
    enabled: !useFirestore || !!uid,
  });

  const data = query?.data;
  const isLoading = query?.isLoading ?? false;
  const isError = query?.isError ?? false;
  const dataUpdatedAt = typeof query?.dataUpdatedAt === "number" ? query.dataUpdatedAt : 0;
  const queryRefetch = query?.refetch ?? (() => Promise.resolve());

  useEffect(() => {
    if (!useFirestore || !uid) return;
    mountedRef.current = true;
    (async () => {
      const db = await getFirebaseDb();
      if (!db || !mountedRef.current) return;
      const firestore = await import("firebase/firestore");
      const d = db as import("firebase/firestore").Firestore;
      const invalidate = () => {
        if (mountedRef.current) {
          queryClient.invalidateQueries({ queryKey: [HERD_STATS_QUERY_KEY, uid] });
        }
      };
      const animalsRef = firestore.collection(d, "users", uid, "animals");
      const breedingRef = firestore.collection(d, "users", uid, "breeding_events");
      const calvingRef = firestore.collection(d, "users", uid, "calving_events");
      unsubRef.current = [
        firestore.onSnapshot(animalsRef, invalidate),
        firestore.onSnapshot(breedingRef, invalidate),
        firestore.onSnapshot(calvingRef, invalidate),
      ];
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current.forEach((fn) => fn());
      unsubRef.current = [];
    };
  }, [uid, useFirestore, queryClient]);

  const stats = data !== undefined && data !== null ? data : getMockHerdStats();
  const fromApi = data != null;

  return {
    stats,
    fromApi,
    loading: isLoading,
    isError,
    dataUpdatedAt,
    refetch: queryRefetch,
  };
}
