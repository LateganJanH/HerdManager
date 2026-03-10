"use client";

import { useQuery } from "@tanstack/react-query";
import { useAuth } from "./useAuth";
import { getFirebaseDb, isFirebaseConfigured } from "./firebase";

export type TransactionType = "SALE" | "PURCHASE" | "EXPENSE";

export interface TransactionDoc {
  id: string;
  type: TransactionType;
  amountCents: number;
  dateEpochDay: number;
  farmId: string;
  notes?: string | null;
  createdAt: number;
  updatedAt: number;
  animalId?: string | null;
  contactName?: string | null;
  contactPhone?: string | null;
  contactEmail?: string | null;
  categoryId?: string | null;
  description?: string | null;
}

export interface ExpenseCategoryDoc {
  id: string;
  name: string;
  farmId: string;
  sortOrder: number;
  createdAt: number;
  updatedAt: number;
}

export interface TransactionTotals {
  totalThisMonth: number;
  totalThisYear: number;
  grandTotal: number;
}

const TRANSACTIONS_QUERY_KEY = "transactions-firestore";
const CATEGORIES_QUERY_KEY = "expense-categories-firestore";

async function fetchTransactionsAndCategories(
  db: unknown,
  uid: string
): Promise<{ transactions: TransactionDoc[]; categories: ExpenseCategoryDoc[] }> {
  const firestore = await import("firebase/firestore");
  const d = db as import("firebase/firestore").Firestore;
  const transactionsRef = firestore.collection(d, "users", uid, "transactions");
  const categoriesRef = firestore.collection(d, "users", uid, "expense_categories");

  const [txSnap, catSnap] = await Promise.all([
    firestore.getDocs(transactionsRef),
    firestore.getDocs(categoriesRef),
  ]);

  const transactions: TransactionDoc[] = txSnap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      type: (data.type as TransactionType) || "EXPENSE",
      amountCents: Number(data.amountCents) || 0,
      dateEpochDay: Number(data.dateEpochDay) || 0,
      farmId: (data.farmId as string) || "",
      notes: (data.notes as string) || null,
      createdAt: Number(data.createdAt) || 0,
      updatedAt: Number(data.updatedAt) || 0,
      animalId: (data.animalId as string) || null,
      contactName: (data.contactName as string) || null,
      contactPhone: (data.contactPhone as string) || null,
      contactEmail: (data.contactEmail as string) || null,
      categoryId: (data.categoryId as string) || null,
      description: (data.description as string) || null,
    };
  });

  const categories: ExpenseCategoryDoc[] = catSnap.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      name: (data.name as string) || "",
      farmId: (data.farmId as string) || "",
      sortOrder: Number(data.sortOrder) || 0,
      createdAt: Number(data.createdAt) || 0,
      updatedAt: Number(data.updatedAt) || 0,
    };
  });

  return { transactions, categories };
}

function computeTotals(
  list: TransactionDoc[],
  now: Date = new Date()
): TransactionTotals {
  const thisMonthStart = new Date(now.getFullYear(), now.getMonth(), 1);
  const thisYearStart = new Date(now.getFullYear(), 0, 1);
  const monthStartEpochDay = Math.floor(thisMonthStart.getTime() / 86400_000);
  const yearStartEpochDay = Math.floor(thisYearStart.getTime() / 86400_000);
  let totalMonth = 0;
  let totalYear = 0;
  let grand = 0;
  for (const t of list) {
    grand += t.amountCents;
    if (t.dateEpochDay >= monthStartEpochDay) totalMonth += t.amountCents;
    if (t.dateEpochDay >= yearStartEpochDay) totalYear += t.amountCents;
  }
  return { totalThisMonth: totalMonth, totalThisYear: totalYear, grandTotal: grand };
}

/** ISO 4217 code -> symbol. Default ZAR = R (South African Rand). */
const CURRENCY_SYMBOLS: Record<string, string> = {
  ZAR: "R",
  USD: "$",
  EUR: "€",
  GBP: "£",
  BWP: "P",
  NAD: "N$",
  SZL: "E",
  LSL: "L",
  AUD: "A$",
  CAD: "C$",
  CHF: "CHF",
  JPY: "¥",
  INR: "₹",
  CNY: "¥",
  KES: "KSh",
  NGN: "₦",
};

function formatCentsWithCurrency(cents: number, currencyCode: string): string {
  const code = (currencyCode || "ZAR").trim().toUpperCase();
  const symbol = CURRENCY_SYMBOLS[code] ?? "R";
  const sign = cents < 0 ? "-" : "";
  const abs = Math.abs(cents);
  return `${sign}${symbol} ${Math.floor(abs / 100)}.${String(abs % 100).padStart(2, "0")}`;
}

function epochDayToYyyyMmDd(epochDay: number): string {
  const date = new Date(epochDay * 86400_000);
  return date.toISOString().slice(0, 10);
}

export function useTransactions(currencyCode: string = "ZAR"): {
  transactions: TransactionDoc[];
  categories: ExpenseCategoryDoc[];
  totalsByType: Record<TransactionType, TransactionTotals>;
  loading: boolean;
  isError: boolean;
  refetch: () => void;
  formatCents: (cents: number) => string;
  epochDayToDate: (epochDay: number) => string;
} {
  const { user } = useAuth();
  const uid = user?.uid ?? "";
  const enabled = Boolean(uid && isFirebaseConfigured());
  const code = (currencyCode || "ZAR").trim() || "ZAR";
  const formatCents = (cents: number) => formatCentsWithCurrency(cents, code);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: [TRANSACTIONS_QUERY_KEY, uid],
    queryFn: async () => {
      const db = await getFirebaseDb();
      if (!db || !uid) return { transactions: [], categories: [] };
      return fetchTransactionsAndCategories(db, uid);
    },
    enabled,
    placeholderData: { transactions: [], categories: [] },
  });

  const transactions = data?.transactions ?? [];
  const categories = data?.categories ?? [];

  const sales = transactions.filter((t) => t.type === "SALE");
  const purchases = transactions.filter((t) => t.type === "PURCHASE");
  const expenses = transactions.filter((t) => t.type === "EXPENSE");

  const totalsByType: Record<TransactionType, TransactionTotals> = {
    SALE: computeTotals(sales),
    PURCHASE: computeTotals(purchases),
    EXPENSE: computeTotals(expenses),
  };

  return {
    transactions,
    categories,
    totalsByType,
    loading: isLoading,
    isError,
    refetch: () => refetch(),
    formatCents,
    epochDayToDate: epochDayToYyyyMmDd,
  };
}
