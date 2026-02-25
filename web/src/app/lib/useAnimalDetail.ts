"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect } from "react";
import type { AnimalDetail } from "./mockHerdData";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";
import { fetchAnimalDetailFromFirestore } from "./firestoreStats";

const ANIMAL_DETAIL_QUERY_KEY = "animal-detail";

export function useAnimalDetail(animalId: string | null): {
  detail: AnimalDetail | null;
  loading: boolean;
  isError: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured() && animalId);
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<Array<() => void>>([]);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: [ANIMAL_DETAIL_QUERY_KEY, uid, animalId],
    queryFn: async () => {
      const db = await getFirebaseDb();
      if (!db || !uid || !animalId) return null;
      return fetchAnimalDetailFromFirestore(db, uid, animalId);
    },
    staleTime: 30_000,
    enabled: useFirestore,
  });

  useEffect(() => {
    if (!useFirestore || !uid || !animalId) return;
    mountedRef.current = true;
    (async () => {
      const db = await getFirebaseDb();
      if (!db || !mountedRef.current) return;
      const firestore = await import("firebase/firestore");
      const d = db as import("firebase/firestore").Firestore;
      const base = `users/${uid}`;
      const refresh = async () => {
        if (!mountedRef.current) return;
        const detail = await fetchAnimalDetailFromFirestore(db, uid, animalId!);
        queryClient.setQueryData([ANIMAL_DETAIL_QUERY_KEY, uid, animalId], detail);
      };
      unsubRef.current = [
        firestore.onSnapshot(firestore.doc(d, base, "animals", animalId), refresh),
        firestore.onSnapshot(
          firestore.query(
            firestore.collection(d, base, "breeding_events"),
            firestore.where("animalId", "==", animalId)
          ),
          refresh
        ),
        firestore.onSnapshot(
          firestore.query(
            firestore.collection(d, base, "calving_events"),
            firestore.where("damId", "==", animalId)
          ),
          refresh
        ),
        firestore.onSnapshot(
          firestore.query(
            firestore.collection(d, base, "health_events"),
            firestore.where("animalId", "==", animalId)
          ),
          refresh
        ),
        firestore.onSnapshot(
          firestore.query(
            firestore.collection(d, base, "photos"),
            firestore.where("animalId", "==", animalId)
          ),
          refresh
        ),
      ];
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current.forEach((fn) => fn());
      unsubRef.current = [];
    };
  }, [uid, animalId, useFirestore, queryClient]);

  return {
    detail: data ?? null,
    loading: isLoading,
    isError,
    refetch,
  };
}
