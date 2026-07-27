package iti.grad.nutriscan.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room schema migrations for NutriScanDatabase.
 *
 * Each migration is a standalone `val` — register them in
 * [NutriScanDatabase] via `.addMigrations(...)`.
 */

/**
 * v4 → v5: adds server-computed `bmi` (REAL) and `tdee` (REAL) columns to the
 * `users` table. Both are nullable so existing rows default to NULL without
 * data loss. The values are populated the next time [fetchAndSyncProfile] runs.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN bmi REAL")
        db.execSQL("ALTER TABLE users ADD COLUMN tdee REAL")
    }
}
