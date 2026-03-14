"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useRef, useEffect, useCallback } from "react";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";

const FARM_TASKS_QUERY_KEY = "farm-tasks-firestore";

export type FarmTaskStatus = "PENDING" | "IN_PROGRESS" | "DONE" | "CANCELLED";

export interface FarmTaskDoc {
  id: string;
  title: string;
  notes?: string | null;
  dueDateEpochDay?: number | null;
  status: FarmTaskStatus;
  animalId?: string | null;
  priority?: string | null;
  createdAt: number;
  updatedAt: number;
}

/** YYYY-MM-DD to epoch day (matches Android Firestore). */
function yyyyMmDdToEpochDay(yyyyMmDd: string): number {
  const [y, m, d] = yyyyMmDd.split("-").map(Number);
  const date = new Date(Date.UTC(y ?? 0, ((m ?? 1) - 1), d ?? 1));
  return Math.floor(date.getTime() / 86400_000);
}

async function fetchFarmTasksFromFirestore(
  db: import("firebase/firestore").Firestore,
  uid: string
): Promise<FarmTaskDoc[]> {
  const firestore = await import("firebase/firestore");
  const snap = await firestore.getDocs(
    firestore.collection(db, "users", uid, "farm_tasks")
  );
  const tasks: FarmTaskDoc[] = [];
  snap.forEach((doc) => {
    const d = doc.data();
    const createdAt = (d?.createdAt as number | undefined) ?? 0;
    const updatedAt = (d?.updatedAt as number | undefined) ?? createdAt;
    tasks.push({
      id: doc.id,
      title: (d?.title as string) ?? "",
      notes: (d?.notes as string | null) ?? null,
      dueDateEpochDay: d?.dueDateEpochDay != null ? Number(d.dueDateEpochDay) : null,
      status: ((d?.status as string) ?? "PENDING") as FarmTaskStatus,
      animalId: (d?.animalId as string | null) ?? null,
      priority: (d?.priority as string | null) ?? null,
      createdAt,
      updatedAt,
    });
  });
  return tasks;
}

export async function addFarmTaskToFirestore(
  db: unknown,
  uid: string,
  payload: {
    title: string;
    notes?: string | null;
    dueDate?: string | null;
    status: FarmTaskStatus;
    animalId?: string | null;
  }
): Promise<string> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const col = firestore.collection(d, "users", uid, "farm_tasks");
  const now = Date.now();
  const docRef = await firestore.addDoc(col, {
    title: payload.title.trim(),
    notes: payload.notes?.trim() || null,
    dueDateEpochDay: payload.dueDate ? yyyyMmDdToEpochDay(payload.dueDate) : null,
    status: payload.status ?? "PENDING",
    animalId: payload.animalId || null,
    createdAt: now,
    updatedAt: now,
  });
  return docRef.id;
}

export async function updateFarmTaskInFirestore(
  db: unknown,
  uid: string,
  taskId: string,
  payload: {
    title: string;
    notes?: string | null;
    dueDate?: string | null;
    status: FarmTaskStatus;
    animalId?: string | null;
  }
): Promise<void> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "farm_tasks", taskId);
  await firestore.updateDoc(ref, {
    title: payload.title.trim(),
    notes: payload.notes?.trim() || null,
    dueDateEpochDay: payload.dueDate ? yyyyMmDdToEpochDay(payload.dueDate) : null,
    status: payload.status ?? "PENDING",
    animalId: payload.animalId || null,
    updatedAt: Date.now(),
  });
}

export async function deleteFarmTaskFromFirestore(
  db: unknown,
  uid: string,
  taskId: string
): Promise<void> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const ref = firestore.doc(d, "users", uid, "farm_tasks", taskId);
  await firestore.deleteDoc(ref);
}

export function useFarmTasks(): {
  tasks: FarmTaskDoc[];
  loading: boolean;
  fromApi: boolean;
  refetch: () => void;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? null;
  const useFirestore = Boolean(uid && isFirebaseConfigured());
  const queryClient = useQueryClient();
  const mountedRef = useRef(true);
  const unsubRef = useRef<Array<() => void>>([]);

  const { data, isLoading, refetch: queryRefetch } = useQuery({
    queryKey: useFirestore ? [FARM_TASKS_QUERY_KEY, uid] : ["farm-tasks"],
    queryFn: useFirestore
      ? async () => {
          const db = await getFirebaseDb();
          if (!db || !uid) return [];
          return fetchFarmTasksFromFirestore(
            db as import("firebase/firestore").Firestore,
            uid
          );
        }
      : () => Promise.resolve([]),
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
          queryClient.invalidateQueries({ queryKey: [FARM_TASKS_QUERY_KEY, uid] });
        }
      };
      unsubRef.current = [
        firestore.onSnapshot(
          firestore.collection(d, "users", uid, "farm_tasks"),
          invalidate
        ),
      ];
    })();
    return () => {
      mountedRef.current = false;
      unsubRef.current.forEach((fn) => fn());
      unsubRef.current = [];
    };
  }, [uid, useFirestore, queryClient]);

  const tasks = data ?? [];
  const fromApi = useFirestore && data != null;

  return {
    tasks,
    loading: isLoading,
    fromApi,
    refetch: queryRefetch,
  };
}
