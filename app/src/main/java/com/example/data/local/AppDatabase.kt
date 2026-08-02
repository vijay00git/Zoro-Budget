package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v4 -> v5: added indices on transactions(accountId/dateTimestamp/categoryTag/isPendingReview)
        // for filter query performance. No column/table changes, so this is just index creation.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_dateTimestamp ON transactions(dateTimestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryTag ON transactions(categoryTag)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_isPendingReview ON transactions(isPendingReview)")
            }
        }

        // v5 -> v6: added transactions.isDismissed, a tombstone flag so deleting an SMS-sourced
        // transaction the user doesn't want doesn't get recreated by the next inbox scan.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN isDismissed INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budget_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    // Versions 1-3 predate this app's first real release and have no schema
                    // history captured (exportSchema was off), so there's no real user data to
                    // preserve for those - a destructive fallback is only acceptable for THEM.
                    // Every version from 4 onward must go through an explicit Migration above;
                    // silently wiping a user's transactions/accounts on every future schema
                    // bump is not.
                    .fallbackToDestructiveMigrationFrom(1, 2, 3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
