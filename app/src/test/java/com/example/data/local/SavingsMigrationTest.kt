package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.SavingsItemEntity
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Builds a real v9 database from the exported schema, migrates it to v10 and lets Room validate
 * the result against its entities (Room throws if any table, column or index differs).
 */
@RunWith(RobolectricTestRunner::class)
class SavingsMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV9Database() {
        val schema = JSONObject(File("schemas/com.example.data.local.AppDatabase/9.json").readText()).getJSONObject("database")
        val callback = object : SupportSQLiteOpenHelper.Callback(9) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    val table = entity.getString("tableName")
                    db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                    val indices = entity.optJSONArray("indices") ?: continue
                    for (j in 0 until indices.length()) {
                        db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(dbName).callback(callback).build()
        )
        helper.writableDatabase.apply {
            execSQL(
                "INSERT INTO accounts (name, type, balance, colorHex, isDefault) VALUES ('HDFC', 'SAVINGS', 12345.5, '#2563EB', 1)"
            )
            execSQL(
                "INSERT INTO transactions (title, amount, type, accountId, categoryTag, dateTimestamp, source, isPendingReview, " +
                    "isRecurring, isDismissed) VALUES ('Swiggy', 450.0, 'DEBIT', 1, 'Food & Dining', 1760000000000, 'SMS_AUTO', 0, 0, 0)"
            )
        }
        helper.close()
    }

    @Test
    fun migratingFromV9KeepsExistingDataAndAddsSavingsTables() = runTest {
        createV9Database()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_9_10)
            .allowMainThreadQueries()
            .build()
        try {
            val account = db.accountDao().getAllAccountsList().single()
            assertEquals(12345.5, account.balance, 0.001)
            assertEquals(1, db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); it.getInt(0) })

            val id = db.savingsDao().insertItem(SavingsItemEntity(section = "FD", name = "SBI FD", holder = "Vijay", startDate = 0))
            assertEquals("SBI FD", db.savingsDao().getItemById(id)!!.name)
        } finally {
            db.close()
        }
    }
}
