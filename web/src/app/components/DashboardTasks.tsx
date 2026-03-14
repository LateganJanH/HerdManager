"use client";

import { useState, useMemo, useCallback } from "react";
import { usePathname } from "next/navigation";
import Link from "next/link";
import {
  useFarmTasks,
  addFarmTaskToFirestore,
  updateFarmTaskInFirestore,
  deleteFarmTaskFromFirestore,
  type FarmTaskDoc,
  type FarmTaskStatus,
} from "../lib/useFarmTasks";
import { useAuth } from "../lib/useAuth";
import { getFirebaseDb } from "../lib/firebase";
import { useAnimalProfiles } from "../lib/useAnimalProfiles";
import { APP_NAME } from "../lib/version";

const STATUS_OPTIONS: { value: FarmTaskStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "DONE", label: "Done" },
  { value: "CANCELLED", label: "Cancelled" },
];

function epochDayToYyyyMmDd(epochDay: number): string {
  const date = new Date(epochDay * 86400_000);
  const y = date.getUTCFullYear();
  const m = String(date.getUTCMonth() + 1).padStart(2, "0");
  const d = String(date.getUTCDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

const OPEN_STATUSES: FarmTaskStatus[] = ["PENDING", "IN_PROGRESS"];
function isOpenTask(t: FarmTaskDoc) {
  return OPEN_STATUSES.includes(t.status);
}

function tasksSummary(tasks: FarmTaskDoc[]) {
  const open = tasks.filter(isOpenTask);
  const todayEpoch = Math.floor(new Date().getTime() / 86400_000);
  const dueToday = open.filter((t) => t.dueDateEpochDay === todayEpoch);
  const overdue = open.filter(
    (t) => t.dueDateEpochDay != null && t.dueDateEpochDay < todayEpoch
  );
  return { openCount: open.length, dueTodayCount: dueToday.length, overdueCount: overdue.length };
}

export function DashboardTasks() {
  const pathname = usePathname();
  const { user } = useAuth();
  const { tasks, loading, fromApi, refetch } = useFarmTasks();
  const { animals } = useAnimalProfiles();
  const [statusFilter, setStatusFilter] = useState<FarmTaskStatus | "ALL">("ALL");
  const [formOpen, setFormOpen] = useState(false);
  const [formTask, setFormTask] = useState<FarmTaskDoc | null>(null);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const openAddForm = useCallback(() => {
    setFormTask(null);
    setFormOpen(true);
  }, []);
  const openEditForm = useCallback((task: FarmTaskDoc) => {
    setFormTask(task);
    setFormOpen(true);
  }, []);
  const closeForm = useCallback(() => {
    setFormOpen(false);
    setFormTask(null);
  }, []);

  const filteredTasks = useMemo(() => {
    if (statusFilter === "ALL") return tasks;
    return tasks.filter((t) => t.status === statusFilter);
  }, [tasks, statusFilter]);

  const summary = useMemo(() => tasksSummary(tasks), [tasks]);
  const earTagByAnimalId = useMemo(
    () => new Map(animals.map((a) => [a.id, a.earTag])),
    [animals]
  );

  const handleAdd = useCallback(
    async (payload: {
      title: string;
      notes?: string | null;
      dueDate?: string | null;
      status: FarmTaskStatus;
      animalId?: string | null;
    }) => {
      const db = await getFirebaseDb();
      if (!db || !user?.uid) return;
      setSaving(true);
      try {
        await addFarmTaskToFirestore(db, user.uid, payload);
        refetch();
        closeForm();
      } finally {
        setSaving(false);
      }
    },
    [user?.uid, refetch, closeForm]
  );

  const handleUpdate = useCallback(
    async (
      taskId: string,
      payload: {
        title: string;
        notes?: string | null;
        dueDate?: string | null;
        status: FarmTaskStatus;
        animalId?: string | null;
      }
    ) => {
      const db = await getFirebaseDb();
      if (!db || !user?.uid) return;
      setSaving(true);
      try {
        await updateFarmTaskInFirestore(db, user.uid, taskId, payload);
        refetch();
        closeForm();
      } finally {
        setSaving(false);
      }
    },
    [user?.uid, refetch, closeForm]
  );

  const handleDelete = useCallback(
    async (taskId: string) => {
      const db = await getFirebaseDb();
      if (!db || !user?.uid) return;
      setSaving(true);
      try {
        await deleteFarmTaskFromFirestore(db, user.uid, taskId);
        refetch();
        setDeleteId(null);
      } finally {
        setSaving(false);
      }
    },
    [user?.uid, refetch]
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <h2 className="text-xl font-semibold text-stone-900 dark:text-stone-100">
        Tasks & reminders
      </h2>
      <p className="text-stone-600 dark:text-stone-300 text-base">
        Farm-wide tasks and reminders. Add, edit, or complete tasks here or in the {APP_NAME} app; they sync across devices.
      </p>

      {!fromApi && (
        <div className="rounded-card border border-stone-200 dark:border-stone-600 bg-stone-50 dark:bg-stone-800/50 p-4">
          <p className="text-sm text-stone-600 dark:text-stone-400">
            Sign in and sync to manage tasks. Tasks are stored in the cloud and shared with the Android app.
          </p>
          <p className="mt-2">
            <Link
              href={`${pathname || "/"}?tab=settings`}
              className="text-sm font-medium text-primary hover:underline"
            >
              Go to Settings →
            </Link>
          </p>
        </div>
      )}

      {fromApi && (
        <>
          <div className="flex flex-wrap items-center gap-4">
            <p className="text-sm text-stone-500 dark:text-stone-400">
              Open: <strong>{summary.openCount}</strong>
              {" · "}
              Due today: <strong>{summary.dueTodayCount}</strong>
              {" · "}
              Overdue:{" "}
              <strong className={summary.overdueCount > 0 ? "text-red-600 dark:text-red-400" : ""}>
                {summary.overdueCount}
              </strong>
            </p>
            <button
              type="button"
              onClick={openAddForm}
              className="rounded-button bg-primary text-white px-4 py-2 text-sm font-medium hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Add task
            </button>
          </div>

          <div className="flex flex-wrap gap-2">
            {STATUS_OPTIONS.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => setStatusFilter(value as FarmTaskStatus | "ALL")}
                className={`rounded-full px-3 py-1.5 text-sm font-medium transition-colors ${
                  statusFilter === value
                    ? "bg-primary text-white"
                    : "bg-stone-200 dark:bg-stone-600 text-stone-700 dark:text-stone-300 hover:bg-stone-300 dark:hover:bg-stone-500"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          {loading ? (
            <p className="text-sm text-stone-500 dark:text-stone-400">Loading tasks…</p>
          ) : filteredTasks.length === 0 ? (
            <div className="rounded-card border border-stone-200 dark:border-stone-600 p-8 text-center">
              <p className="text-stone-600 dark:text-stone-400">
                {statusFilter === "ALL" ? "No tasks yet." : `No ${statusFilter.toLowerCase().replace("_", " ")} tasks.`}
              </p>
              <p className="mt-2 text-sm text-stone-500 dark:text-stone-400">
                Tap &quot;Add task&quot; to create one.
              </p>
            </div>
          ) : (
            <ul className="space-y-3">
              {filteredTasks.map((task) => {
                const todayEpoch = Math.floor(new Date().getTime() / 86400_000);
                const isOverdue =
                  task.dueDateEpochDay != null &&
                  task.dueDateEpochDay < todayEpoch &&
                  OPEN_STATUSES.includes(task.status);
                const isDueToday = task.dueDateEpochDay === todayEpoch;
                const earTag = task.animalId ? earTagByAnimalId.get(task.animalId) : null;
                return (
                  <li
                    key={task.id}
                    className="rounded-card border border-stone-200 dark:border-stone-600 bg-white dark:bg-stone-800 p-4 shadow-card"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div className="min-w-0 flex-1">
                        <p className="font-medium text-stone-900 dark:text-stone-100">
                          {task.title}
                        </p>
                        {task.dueDateEpochDay != null && (
                          <p
                            className={`mt-1 text-sm ${
                              isOverdue
                                ? "text-red-600 dark:text-red-400"
                                : isDueToday
                                  ? "text-primary"
                                  : "text-stone-500 dark:text-stone-400"
                            }`}
                          >
                            {epochDayToYyyyMmDd(task.dueDateEpochDay)}
                            {isOverdue && " (overdue)"}
                            {isDueToday && !isOverdue && " (today)"}
                          </p>
                        )}
                        <p className="mt-1 text-xs text-stone-500 dark:text-stone-400">
                          {task.status.replace("_", " ")}
                          {earTag && ` · ${earTag}`}
                        </p>
                        {task.notes && (
                          <p className="mt-2 text-sm text-stone-600 dark:text-stone-300">
                            {task.notes}
                          </p>
                        )}
                      </div>
                      <span className="flex items-center gap-1">
                        <button
                          type="button"
                          onClick={() => openEditForm(task)}
                          className="text-sm font-medium text-primary hover:underline"
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          onClick={() => setDeleteId(task.id)}
                          className="text-sm font-medium text-red-600 dark:text-red-400 hover:underline"
                        >
                          Delete
                        </button>
                      </span>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </>
      )}

      {formOpen && (
        <TaskForm
          task={formTask}
          animals={animals}
          saving={saving}
          onSave={async (payload) => {
            if (formTask) {
              await handleUpdate(formTask.id, payload);
            } else {
              await handleAdd(payload);
            }
          }}
          onCancel={closeForm}
        />
      )}

      {deleteId && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-task-title"
        >
          <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-6 max-w-sm w-full shadow-card">
            <h3 id="delete-task-title" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
              Delete task
            </h3>
            <p className="mt-2 text-sm text-stone-600 dark:text-stone-400">
              This cannot be undone.
            </p>
            <div className="mt-4 flex gap-2 justify-end">
              <button
                type="button"
                onClick={() => setDeleteId(null)}
                className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={() => deleteId && handleDelete(deleteId)}
                disabled={saving}
                className="rounded-button bg-red-600 text-white px-4 py-2 text-sm font-medium disabled:opacity-50"
              >
                {saving ? "Deleting…" : "Delete"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function TaskForm({
  task,
  animals,
  saving,
  onSave,
  onCancel,
}: {
  task: FarmTaskDoc | null;
  animals: { id: string; earTag: string }[];
  saving: boolean;
  onSave: (payload: {
    title: string;
    notes?: string | null;
    dueDate?: string | null;
    status: FarmTaskStatus;
    animalId?: string | null;
  }) => Promise<void>;
  onCancel: () => void;
}) {
  const isEdit = task != null;
  const [title, setTitle] = useState(task?.title ?? "");
  const [notes, setNotes] = useState(task?.notes ?? "");
  const [dueDate, setDueDate] = useState(
    task?.dueDateEpochDay != null ? epochDayToYyyyMmDd(task.dueDateEpochDay) : ""
  );
  const [status, setStatus] = useState<FarmTaskStatus>(task?.status ?? "PENDING");
  const [animalId, setAnimalId] = useState<string>(task?.animalId ?? "");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const t = title.trim();
    if (!t) {
      setError("Title is required");
      return;
    }
    setError(null);
    await onSave({
      title: t,
      notes: notes.trim() || null,
      dueDate: dueDate.trim() || null,
      status,
      animalId: animalId || null,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="task-form-title"
    >
      <div className="rounded-card bg-white dark:bg-stone-800 border border-stone-200 dark:border-stone-600 p-6 max-w-md w-full max-h-[90vh] overflow-y-auto shadow-card">
        <h3 id="task-form-title" className="text-lg font-semibold text-stone-900 dark:text-stone-100">
          {isEdit ? "Edit task" : "Add task"}
        </h3>
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label htmlFor="task-title" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Title *
            </label>
            <input
              id="task-title"
              type="text"
              value={title}
              onChange={(e) => { setTitle(e.target.value); setError(null); }}
              className="mt-1 w-full rounded border border-stone-300 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 bg-white dark:bg-stone-800"
              required
            />
            {error && <p className="mt-1 text-sm text-red-600 dark:text-red-400">{error}</p>}
          </div>
          <div>
            <label htmlFor="task-notes" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Notes (optional)
            </label>
            <textarea
              id="task-notes"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              className="mt-1 w-full rounded border border-stone-300 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 bg-white dark:bg-stone-800"
            />
          </div>
          <div>
            <label htmlFor="task-due" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Due date (optional)
            </label>
            <input
              id="task-due"
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              className="mt-1 w-full rounded border border-stone-300 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 bg-white dark:bg-stone-800"
            />
          </div>
          {isEdit && (
            <div>
              <label htmlFor="task-status" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
                Status
              </label>
              <select
                id="task-status"
                value={status}
                onChange={(e) => setStatus(e.target.value as FarmTaskStatus)}
                className="mt-1 w-full rounded border border-stone-300 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 bg-white dark:bg-stone-800"
              >
                {STATUS_OPTIONS.filter((o) => o.value !== "ALL").map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div>
            <label htmlFor="task-animal" className="block text-sm font-medium text-stone-700 dark:text-stone-300">
              Link to animal (optional)
            </label>
            <select
              id="task-animal"
              value={animalId}
              onChange={(e) => setAnimalId(e.target.value)}
              className="mt-1 w-full rounded border border-stone-300 dark:border-stone-600 px-3 py-2 text-stone-900 dark:text-stone-100 bg-white dark:bg-stone-800"
            >
              <option value="">— None —</option>
              {animals.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.earTag}
                </option>
              ))}
            </select>
          </div>
          <div className="flex gap-2 justify-end pt-2">
            <button
              type="button"
              onClick={onCancel}
              className="rounded-button border border-stone-300 dark:border-stone-600 px-4 py-2 text-sm"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={saving}
              className="rounded-button bg-primary text-white px-4 py-2 text-sm font-medium disabled:opacity-50"
            >
              {saving ? "Saving…" : isEdit ? "Update" : "Add"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
