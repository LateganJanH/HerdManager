"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import { getMockAlerts, isSampleDataEnabled } from "./mockHerdData";
import type { AlertItem } from "./mockHerdData";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchAlertsFromFirestore } from "./firestoreStats";

const ALERTS_QUERY_KEY = "alerts-firestore";

export function useAlerts(): {
  alerts: AlertItem[];
  fromApi: boolean;
  loading: boolean;
  isError: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured());
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<Array<() => void>>([]);

  const { data, isLoading, isError, refetch: queryRefetch } = useQuery({
    queryKey: useFirestore ? [ALERTS_QUERY_KEY, uid] : ["alerts"],
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return getMockAlerts();
          return fetchAlertsFromFirestore(db, uid);
        }
      : () => Promise.resolve(getMockAlerts()),
    staleTime: 30_000,
    enabled: !useFirestore || !!uid,
  });

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
          queryClient.invalidateQueries({ queryKey: [ALERTS_QUERY_KEY, uid] });
        }
      };
      unsubRef.current = [
        firestore.onSnapshot(firestore.collection(d, "users", uid, "animals"), invalidate),
        firestore.onSnapshot(firestore.collection(d, "users", uid, "breeding_events"), invalidate),
        firestore.onSnapshot(firestore.collection(d, "users", uid, "calving_events"), invalidate),
        firestore.onSnapshot(firestore.collection(d, "users", uid, "health_events"), invalidate),
        firestore.onSnapshot(firestore.collection(d, "users", uid, "weight_records"), invalidate),
        firestore.onSnapshot(firestore.doc(d, "users", uid, "settings", "farm"), invalidate),
      ];
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current.forEach((fn) => fn());
      unsubRef.current = [];
    };
  }, [uid, useFirestore, queryClient]);

  const alerts = data ?? getMockAlerts();
  const fromApi = useFirestore && data != null;

  return {
    alerts,
    fromApi: fromApi || (isSampleDataEnabled() && !useFirestore),
    loading: isLoading,
    isError,
    refetch: queryRefetch,
  };
}
