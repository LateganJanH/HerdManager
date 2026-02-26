"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import type { FarmProfile } from "./mockHerdData";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchFarmSettingsFromFirestore, saveFarmSettingsToFirestore, parseContactsFromFirestore } from "./firestoreStats";

const FARM_SETTINGS_QUERY_KEY = "farm-settings-firestore";

/**
 * Fetches farm profile from Firestore (synced from Android). Subscribes for real-time
 * updates so when any device updates farm settings and syncs, the web dashboard reflects it.
 */
export function useFarmSettings(): {
  farm: FarmProfile | null;
  fromApi: boolean;
  loading: boolean;
  isError: boolean;
  refetch: () => void;
  /** Save farm settings to Firestore. Linked devices receive them on next sync. */
  updateFarmSettings: (farm: FarmProfile) => Promise<void>;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured());
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<(() => void) | null>(null);

  const { data, isLoading, isError, refetch: queryRefetch } = useQuery({
    queryKey: useFirestore ? [FARM_SETTINGS_QUERY_KEY, uid] : ["farm-settings"],
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return null;
          return fetchFarmSettingsFromFirestore(db, uid);
        }
      : () => Promise.resolve(null),
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
      const ref = firestore.doc(d, "users", uid, "settings", "farm");
      unsubRef.current = firestore.onSnapshot(ref, (snapshot) => {
        if (!mountedRef.current) return;
        if (!snapshot.exists()) {
          queryClient.setQueryData([FARM_SETTINGS_QUERY_KEY, uid], null);
          return;
        }
        const data = snapshot.data()!;
        const calving =
          (data.calvingAlertDays as number | undefined) ?? 14;
        const pregnancy =
          (data.pregnancyCheckDaysAfterBreeding as number | undefined) ?? 28;
        const gestation = (data.gestationDays as number | undefined) ?? 283;
        const weaning = (data.weaningAgeDays as number | undefined) ?? 200;
        const contacts = parseContactsFromFirestore(data.contacts, data.contactPhone, data.contactEmail);
        const farm: FarmProfile = {
          id: (data.id as string) ?? "farm",
          name: (data.name as string) ?? "",
          address: (data.address as string) ?? "",
          contacts,
          calvingAlertDays: Math.max(1, Math.min(90, calving)),
          pregnancyCheckDaysAfterBreeding: Math.max(14, Math.min(60, pregnancy)),
          gestationDays: Math.max(250, Math.min(320, gestation)),
          weaningAgeDays: Math.max(150, Math.min(300, weaning)),
        };
        queryClient.setQueryData([FARM_SETTINGS_QUERY_KEY, uid], farm);
      });
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current?.();
      unsubRef.current = null;
    };
  }, [uid, useFirestore, queryClient]);

  const fromApi = useFirestore && data !== undefined;

  const updateFarmSettings = async (farm: FarmProfile) => {
    const db = await getFirebaseDb();
    if (!db || !uid) throw new Error("Not signed in or Firebase not configured");
    await saveFarmSettingsToFirestore(db, uid, farm);
    await queryClient.invalidateQueries({ queryKey: [FARM_SETTINGS_QUERY_KEY, uid] });
  };

  return {
    farm: data ?? null,
    fromApi,
    loading: isLoading,
    isError,
    refetch: queryRefetch,
    updateFarmSettings,
  };
}
