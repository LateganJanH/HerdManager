"use client";

import { useState } from "react";
import {
  useTransactions,
  type TransactionDoc,
  type TransactionType,
} from "../lib/useTransactions";
import { useFarmSettings } from "../lib/useFarmSettings";
import { DEFAULT_CURRENCY_CODE } from "../lib/mockHerdData";

const TYPES: { id: TransactionType; label: string }[] = [
  { id: "SALE", label: "Sales" },
  { id: "PURCHASE", label: "Purchases" },
  { id: "EXPENSE", label: "Expenses" },
];

export function DashboardTransactions() {
  const { farm } = useFarmSettings();
  const currencyCode = farm?.currencyCode ?? DEFAULT_CURRENCY_CODE;
  const {
    transactions,
    categories,
    totalsByType,
    loading,
    isError,
    refetch,
    formatCents,
    epochDayToDate,
  } = useTransactions(currencyCode);
  const [activeTab, setActiveTab] = useState<TransactionType>("SALE");

  const filtered = transactions.filter((t) => t.type === activeTab);

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-xl font-bold text-stone-800 dark:text-stone-200">
          Transactions
        </h2>
        <button
          type="button"
          onClick={() => refetch()}
          disabled={loading}
          className="rounded-lg px-3 py-2 text-sm font-medium bg-primary text-white hover:opacity-90 disabled:opacity-60"
        >
          {loading ? "Loading…" : "Refresh"}
        </button>
      </div>

      {isError && (
        <p className="text-sm text-red-600 dark:text-red-400">
          Failed to load transactions. Sync from the app to see data here.
        </p>
      )}

      <div className="flex gap-2 border-b border-stone-200 dark:border-stone-700">
        {TYPES.map(({ id, label }) => (
          <button
            key={id}
            type="button"
            onClick={() => setActiveTab(id)}
            className={`px-4 py-2 text-sm font-medium rounded-t-lg transition-colors ${
              activeTab === id
                ? "bg-primary/10 text-primary border-b-2 border-primary"
                : "text-stone-600 dark:text-stone-400 hover:bg-stone-100 dark:hover:bg-stone-800"
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      <TotalsCard
        totalThisMonth={totalsByType[activeTab].totalThisMonth}
        totalThisYear={totalsByType[activeTab].totalThisYear}
        grandTotal={totalsByType[activeTab].grandTotal}
        formatCents={formatCents}
      />

      <section aria-label={`${TYPES.find((t) => t.id === activeTab)?.label ?? activeTab} list`}>
        <h3 className="text-sm font-medium text-stone-600 dark:text-stone-400 mb-2">
          List ({filtered.length})
        </h3>
        {filtered.length === 0 ? (
          <p className="text-stone-500 dark:text-stone-400 text-sm">
            No {activeTab.toLowerCase()} yet. Add them in the HerdManager app; they will sync here.
          </p>
        ) : (
          <ul className="space-y-2">
            {filtered
              .sort((a, b) => b.dateEpochDay - a.dateEpochDay)
              .map((t) => (
                <TransactionRow
                  key={t.id}
                  transaction={t}
                  formatCents={formatCents}
                  epochDayToDate={epochDayToDate}
                  categories={categories}
                />
              ))}
          </ul>
        )}
      </section>

      <section aria-label="Analytics summary">
        <h3 className="text-sm font-medium text-stone-600 dark:text-stone-400 mb-2">
          Analytics – All types
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {TYPES.map(({ id, label }) => (
            <div
              key={id}
              className="rounded-lg border border-stone-200 dark:border-stone-700 p-4 bg-stone-50 dark:bg-stone-800/50"
            >
              <p className="text-xs font-medium text-stone-500 dark:text-stone-400 uppercase tracking-wide">
                {label}
              </p>
              <p className="text-lg font-semibold text-stone-800 dark:text-stone-200 mt-1">
                {formatCents(totalsByType[id].grandTotal)}
              </p>
              <p className="text-xs text-stone-500 dark:text-stone-400 mt-0.5">
                This year: {formatCents(totalsByType[id].totalThisYear)}
              </p>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function TotalsCard({
  totalThisMonth,
  totalThisYear,
  grandTotal,
  formatCents,
}: {
  totalThisMonth: number;
  totalThisYear: number;
  grandTotal: number;
  formatCents: (c: number) => string;
}) {
  return (
    <div className="rounded-lg border border-stone-200 dark:border-stone-700 p-4 bg-white dark:bg-stone-800 shadow-sm">
      <p className="text-xs font-medium text-stone-500 dark:text-stone-400 uppercase tracking-wide">
        Totals
      </p>
      <div className="grid grid-cols-3 gap-4 mt-2">
        <div>
          <p className="text-2xl font-bold text-primary">{formatCents(totalThisMonth)}</p>
          <p className="text-xs text-stone-500">This month</p>
        </div>
        <div>
          <p className="text-2xl font-bold text-stone-800 dark:text-stone-200">
            {formatCents(totalThisYear)}
          </p>
          <p className="text-xs text-stone-500">This year</p>
        </div>
        <div>
          <p className="text-2xl font-bold text-stone-800 dark:text-stone-200">
            {formatCents(grandTotal)}
          </p>
          <p className="text-xs text-stone-500">Grand total</p>
        </div>
      </div>
    </div>
  );
}

function TransactionRow({
  transaction,
  formatCents,
  epochDayToDate,
  categories,
}: {
  transaction: TransactionDoc;
  formatCents: (c: number) => string;
  epochDayToDate: (d: number) => string;
  categories: { id: string; name: string }[];
}) {
  const categoryName =
    transaction.categoryId != null
      ? categories.find((c) => c.id === transaction.categoryId)?.name ?? transaction.categoryId
      : null;
  const contact = transaction.contactName ?? transaction.contactEmail ?? transaction.contactPhone ?? null;

  return (
    <li className="flex flex-wrap items-center justify-between gap-2 rounded-lg border border-stone-200 dark:border-stone-700 p-3 bg-white dark:bg-stone-800">
      <div>
        <p className="font-medium text-stone-800 dark:text-stone-200">
          {formatCents(transaction.amountCents)}
        </p>
        <p className="text-xs text-stone-500 dark:text-stone-400">
          {epochDayToDate(transaction.dateEpochDay)}
        </p>
        {(contact || categoryName || transaction.description) && (
          <p className="text-sm text-stone-600 dark:text-stone-300 mt-0.5">
            {contact ?? categoryName ?? transaction.description ?? ""}
          </p>
        )}
      </div>
    </li>
  );
}
