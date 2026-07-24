package iti.grad.nutriscan.data.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `water_log` (" +
                "`date` TEXT NOT NULL, `glassCount` INTEGER NOT NULL, " +
                "`goalGlasses` INTEGER NOT NULL, PRIMARY KEY(`date`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_log` (" +
                "`date` TEXT NOT NULL, `done` INTEGER NOT NULL, PRIMARY KEY(`date`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `streak` (" +
                "`id` INTEGER NOT NULL, `currentStreak` INTEGER NOT NULL, " +
                "`longestStreak` INTEGER NOT NULL, `lastActiveDate` TEXT, PRIMARY KEY(`id`))"
        )
    }
}
