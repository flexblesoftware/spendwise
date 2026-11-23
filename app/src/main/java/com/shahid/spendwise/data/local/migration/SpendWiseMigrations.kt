package com.shahid.spendwise.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SpendWiseMigrations {
    /**
     * Migration from Version 1 to Version 2:
     * Creates user_sync_meta table to track local device metadata and preserves all user transactions,
     * budgets, categories, and debts without any destructive resets.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_sync_meta (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    key TEXT NOT NULL,
                    value TEXT NOT NULL,
                    updatedAtMillis INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_sync_meta_key ON user_sync_meta (key)")
        }
    }

    val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
}
