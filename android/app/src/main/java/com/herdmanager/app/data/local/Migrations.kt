package com.herdmanager.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations to preserve user data across schema changes.
 * When bumping the database version, add a migration here and register it in [AppModule].
 */
object Migrations {

    /**
     * Adds [updatedAt] to event-style tables for timestamp-based sync merge.
     * Existing rows get updatedAt = 0; app and sync will treat 0 as "use date-derived fallback".
     */
    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE calving_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE health_events ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE weight_records ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE herd_assignments ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE photos ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * Adds transactions and expense_categories tables for sales, purchases, and expenses tracking.
     */
    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id TEXT PRIMARY KEY NOT NULL,
                    type TEXT NOT NULL,
                    amountCents INTEGER NOT NULL,
                    dateEpochDay INTEGER NOT NULL,
                    farmId TEXT NOT NULL,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    animalId TEXT,
                    contactName TEXT,
                    contactPhone TEXT,
                    contactEmail TEXT,
                    categoryId TEXT,
                    description TEXT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_farmId ON transactions(farmId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_dateEpochDay ON transactions(dateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_animalId ON transactions(animalId)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS expense_categories (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    farmId TEXT NOT NULL,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_expense_categories_farmId ON expense_categories(farmId)")
        }
    }

    /**
     * Adds optional weight (kg) and pricePerKgCents columns to transactions
     * for meat price and sold-weight tracking.
     */
    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE transactions ADD COLUMN weightKg REAL")
            db.execSQL("ALTER TABLE transactions ADD COLUMN pricePerKgCents INTEGER")
        }
    }

    /**
     * Adds farm_tasks table for farm-wide tasks and reminders.
     */
    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS farm_tasks (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    notes TEXT,
                    dueDateEpochDay INTEGER,
                    status TEXT NOT NULL,
                    animalId TEXT,
                    priority TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_farm_tasks_status ON farm_tasks(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_farm_tasks_dueDateEpochDay ON farm_tasks(dueDateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_farm_tasks_animalId ON farm_tasks(animalId)")
        }
    }
}
