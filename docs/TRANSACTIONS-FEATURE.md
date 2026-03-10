# Transactions (Sales, Purchases, Expenses)

Transactions tracking is implemented end-to-end on Android and the web dashboard.

## Data model

- **Transaction**: `id`, `type` (SALE | PURCHASE | EXPENSE), `amountCents`, `dateEpochDay`, `farmId`, `notes`, `createdAt`, `updatedAt`.  
  - **Sale**: `animalId` (sold animal), `contactName`, `contactPhone`, `contactEmail` (buyer). Optionally set animal status to SOLD when recording a sale.  
  - **Purchase**: `animalId` (purchased animal), `contactName`, `contactPhone`, `contactEmail` (seller / provenance).  
  - **Expense**: `categoryId`, `description`.
- **ExpenseCategory**: `id`, `name`, `farmId`, `sortOrder`, `createdAt`, `updatedAt`.

Amounts are stored in **cents** to avoid floating-point issues. Dates use **epoch day** (days since 1970-01-01).

**Currency:** Farm settings include a **currency code** (e.g. ZAR, USD). All transaction amount displays use this setting. Default is **South African Rand (R)**. Set in **Settings → Farm** (Android) or **Settings → Farm settings** (web); syncs across devices.

## Android

- **Room**: `TransactionEntity`, `ExpenseCategoryEntity`; DAOs and DB version 15 with migration 14→15.
- **Repositories**: `TransactionRepository`, `ExpenseCategoryRepository` (interfaces + impl).
- **Sync**: Transactions and expense categories are uploaded to and downloaded from Firestore (`users/{uid}/transactions`, `users/{uid}/expense_categories`) in `SyncRepositoryImpl`. Merge by document (remote wins when newer by `updatedAt`).
- **Backup**: Export/import in `BackupRepositoryImpl` includes `transactions` and `expenseCategories` JSON arrays.
- **UI**:
  - **Transactions** (bottom nav): Tabs Sales / Purchases / Expenses; list with amount, date, contact or description; totals (this month, this year, grand total); FAB to add; tap row to edit, long-press to delete; “Manage categories” on Expenses tab.
  - **Add/Edit transaction**: Type, amount, date, notes; for Sale/Purchase: buyer/seller contact and animal link; for Expense: category and description. Option “Set animal status to SOLD” when adding a sale with an animal.
  - **Expense categories**: List of categories with inline edit (name) and delete; FAB to add.
- **Navigation**: Home quick action “Transactions”; bottom nav “Transactions”; routes `add_transaction/{type}`, `edit_transaction/{transactionId}`, `expense_categories`.

## Web dashboard

- **Tab**: “Transactions” in the main tab bar.
- **Data**: `useTransactions()` reads from Firestore `users/{uid}/transactions` and `users/{uid}/expense_categories` (when signed in and Firebase is configured).
- **UI**: Tabs Sales / Purchases / Expenses; totals card (this month, this year, grand total); list of transactions; “Analytics – All types” summary cards with grand total and year total per type.
- **Home**: “Transactions” card in the quick-stats strip links to the Transactions tab.

## Firestore

- Paths: `users/{userId}/transactions/{transactionId}`, `users/{userId}/expense_categories/{categoryId}`.
- Existing rules (`users/{userId}/{document=**}`) already allow read/write for the authenticated user.

## Analytics

- **Per type**: Totals for “this month”, “this year”, and “grand total” for Sales, Purchases, and Expenses.
- **Android**: Shown in the Transactions screen per tab.
- **Web**: Shown in the Transactions tab and in the “Analytics – All types” section.

## Links to animals

- **Sales**: Transaction can reference an animal by `animalId`; optional “Set animal status to SOLD” when saving a sale.
- **Purchases**: Transaction references the purchased animal by `animalId` (provenance).
- **Animal detail**: Can be extended to show related sale/purchase transactions; list rows can navigate to the animal profile.
