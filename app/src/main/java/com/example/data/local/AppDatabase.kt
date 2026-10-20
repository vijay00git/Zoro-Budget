package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AccountDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.RecurringBillDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringBillEntity
import com.example.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        RecurringBillEntity::class
    ],
    version = 9,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringBillDao(): RecurringBillDao

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

        // v6 -> v7: added transactions.lastPaidTimestamp (suppress a recurring bill's due/overdue
        // alert for the cycle it was just paid in) and categories.isIncomeCategory (exclude
        // income/savings categories from the overall monthly budget split).
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN lastPaidTimestamp INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE categories ADD COLUMN isIncomeCategory INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE categories SET isIncomeCategory = 1 WHERE name IN ('Salary & Income', 'Investment & Savings')")
            }
        }

        // v7 -> v8: added transactions.smsSender and transactions.smsAccountLast4 so SMS rows
        // from a bank/card no account recognises yet can be flagged and learned on review.
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsSender TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN smsAccountLast4 TEXT DEFAULT NULL")
            }
        }

        // v8 -> v9: recurring bills move out of the transactions table into recurring_bills, and
        // transactions.recurringBillId links each payment to its bill. Existing flagged rows are
        // converted by LegacyRecurringBillConverter without changing any balance.
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `recurring_bills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `accountId` INTEGER NOT NULL, " +
                        "`categoryTag` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, " +
                        "`dueDay` INTEGER NOT NULL, `reminderDaysBefore` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, " +
                        "`notes` TEXT, `matchPayee` TEXT, `lastPaidTimestamp` INTEGER, `createdTimestamp` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_accountId` ON `recurring_bills` (`accountId`)")
                db.execSQL("ALTER TABLE transactions ADD COLUMN recurringBillId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_recurringBillId` ON `transactions` (`recurringBillId`)")
                LegacyRecurringBillConverter.convert(db)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_budget_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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
