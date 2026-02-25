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
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchLinkedDevicesFromFirestore } from "./firestoreDevices";

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

const LINKED_DEVICES_QUERY_KEY = ["linked-devices"] as const;

/**
 * Fetches linked field devices. When signed in and Firebase is configured,
 * reads from Firestore (users/{uid}/devices) where Android writes on each sync.
 * Otherwise uses /api/devices or mock when sample data is enabled.
 */
export function useLinkedDevices(): {
  devices: LinkedDevice[];
  loading: boolean;
  isError: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured());

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: useFirestore ? ["linked-devices-firestore", uid] : LINKED_DEVICES_QUERY_KEY,
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return { devices: [] };
          const devices = await fetchLinkedDevicesFromFirestore(db, uid);
          return { devices };
        }
      : fetchLinkedDevicesFromApi,
    staleTime: 30_000,
    enabled: !useFirestore || !!uid,
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
