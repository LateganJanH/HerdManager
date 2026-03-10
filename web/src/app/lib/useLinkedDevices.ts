"use client";

import { useQuery } from "@tanstack/react-query";
import {
  filterValidDevices,
  getMockLinkedDevices,
  type LinkedDevice,
  type LinkedDevicesResponse,
} from "./linkedDevices";
import { isSampleDataEnabled } from "./mockHerdData";
import { useAuth } from "./useAuth";
import { getFirebaseDb, getFirebaseFunctions, isFirebaseConfigured } from "./firebase";
import { fetchLinkedDevicesFromFirestore } from "./firestoreDevices";

const useStatsViaCallable = process.env.NEXT_PUBLIC_USE_STATS_VIA_CALLABLE === "true";

async function fetchLinkedDevicesFromApi(): Promise<LinkedDevicesResponse> {
  const res = await fetch("/api/devices");
  if (!res.ok) return { devices: [] };
  const data: unknown = await res.json();
  if (data && typeof data === "object" && Array.isArray((data as LinkedDevicesResponse).devices)) {
    const devices = filterValidDevices((data as LinkedDevicesResponse).devices);
    return { devices };
  }
  return { devices: [] };
}

async function fetchLinkedDevicesFromCallable(): Promise<LinkedDevicesResponse> {
  const functions = await getFirebaseFunctions();
  if (!functions) return { devices: [] };
  const { httpsCallable } = await import("firebase/functions");
  const getDevices = httpsCallable<unknown, { devices: Array<{ id: string; name: string; lastSyncAt: number }> }>(functions, "getDevices");
  const result = await getDevices({});
  const devices = filterValidDevices(result.data?.devices ?? []);
  return { devices };
}

const LINKED_DEVICES_QUERY_KEY = ["linked-devices"] as const;

/**
 * Fetches linked field devices. When signed in and Firebase is configured,
 * uses Cloud Function getDevices (if NEXT_PUBLIC_USE_STATS_VIA_CALLABLE=true),
 * else reads from Firestore. Otherwise uses /api/devices or mock when sample data is enabled.
 */
export function useLinkedDevices(): {
  devices: LinkedDevice[];
  loading: boolean;
  isError: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useCallable = Boolean(useStatsViaCallable && uid && isFirebaseConfigured());
  const useFirestore = Boolean(uid && isFirebaseConfigured() && !useCallable);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: useCallable ? ["linked-devices-callable", uid] : useFirestore ? ["linked-devices-firestore", uid] : LINKED_DEVICES_QUERY_KEY,
    queryFn: useCallable
      ? fetchLinkedDevicesFromCallable
      : useFirestore
        ? async () => {
            const db = await getFirebaseDb();
            if (!db || !uid) return { devices: [] };
            const devices = await fetchLinkedDevicesFromFirestore(db, uid);
            return { devices };
          }
        : fetchLinkedDevicesFromApi,
    staleTime: 30_000,
    enabled: useCallable ? !!uid : !useFirestore || !!uid,
  });

  const apiDevices = data?.devices ?? [];
  const devices: LinkedDevice[] =
    apiDevices.length > 0 ? apiDevices : (isSampleDataEnabled() ? getMockLinkedDevices() : []);

  return {
    devices,
    loading: isLoading,
    isError,
    refetch,
  };
}
