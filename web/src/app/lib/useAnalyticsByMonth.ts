"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import type { EventsByMonth } from "./firestoreStats";
import { fetchEventsByMonthFromFirestore } from "./firestoreStats";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";

const EVENTS_BY_MONTH_QUERY_KEY = "analytics-events-by-month";

export function useAnalyticsByMonth(): {
  data: EventsByMonth | null;
  loading: boolean;
  isError: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const enabled = Boolean(uid && isFirebaseConfigured());
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<Array<() => void>>([]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: [EVENTS_BY_MONTH_QUERY_KEY, uid],
    queryFn: async () => {
      const db = await getFirebaseDb();
      if (!db || !uid) return null;
      return fetchEventsByMonthFromFirestore(db, uid);
    },
    staleTime: 60_000,
    enabled,
  });

  useEffect(() => {
    if (!enabled || !uid) return;
    mountedRef.current = true;
    (async () => {
      const db = await getFirebaseDb();
      if (!db || !mountedRef.current) return;
      const firestore = await import("firebase/firestore");
      const d = db as import("firebase/firestore").Firestore;
      const base = `users/${uid}`;
      const invalidate = () => {
        if (mountedRef.current) {
          queryClient.invalidateQueries({ queryKey: [EVENTS_BY_MONTH_QUERY_KEY, uid] });
        }
      };
      unsubRef.current = [
        firestore.onSnapshot(firestore.collection(d, base, "breeding_events"), invalidate),
        firestore.onSnapshot(firestore.collection(d, base, "calving_events"), invalidate),
      ];
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current.forEach((fn) => fn());
      unsubRef.current = [];
    };
  }, [uid, enabled, queryClient]);

  return {
    data: data ?? null,
    loading: isLoading,
    isError,
    refetch,
  };
}
