"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import { getMockAnimalProfiles, isSampleDataEnabled } from "./mockHerdData";
import type { AnimalProfile } from "./mockHerdData";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchAnimalsFromFirestore } from "./firestoreStats";

const ANIMAL_PROFILES_QUERY_KEY = "animal-profiles-firestore";

function mapSnapshotToProfiles(
  docs: Array<{ id: string; data: () => Record<string, unknown> }>
): AnimalProfile[] {
  return docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      earTag: (data.earTagNumber as string) ?? "",
      status: (data.status as string) ?? "ACTIVE",
      sex: (data.sex as string) ?? "FEMALE",
    };
  });
}

export function useAnimalProfiles(): {
  animals: AnimalProfile[];
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
  const unsubRef = useRef<(() => void) | null>(null);

  const { data, isLoading, isError, refetch: queryRefetch } = useQuery({
    queryKey: useFirestore ? [ANIMAL_PROFILES_QUERY_KEY, uid] : ["animal-profiles"],
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return getMockAnimalProfiles();
          return fetchAnimalsFromFirestore(db, uid);
        }
      : () => Promise.resolve(getMockAnimalProfiles()),
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
      const ref = firestore.collection(d, "users", uid, "animals");
      unsubRef.current = firestore.onSnapshot(ref, (snapshot) => {
        if (!mountedRef.current) return;
        const profiles = mapSnapshotToProfiles(snapshot.docs);
        queryClient.setQueryData([ANIMAL_PROFILES_QUERY_KEY, uid], profiles);
      });
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current?.();
      unsubRef.current = null;
    };
  }, [uid, useFirestore, queryClient]);

  const animals = data ?? getMockAnimalProfiles();
  const fromApi = useFirestore && data != null;

  return {
    animals,
    fromApi: fromApi || (isSampleDataEnabled() && !useFirestore),
    loading: isLoading,
    isError,
    refetch: queryRefetch,
  };
}
