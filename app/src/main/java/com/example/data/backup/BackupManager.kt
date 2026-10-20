package com.example.data.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.core.content.edit
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_NAME
import com.example.data.local.DATABASE_VERSION
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

/**
 * Full backups as one ZIP file: an exact copy of the database (every table and field, so bills,
 * savings, dismissed SMS and history all come back), the app's settings, and the savings
 * attachments folder. Restoring replaces everything on the device; it never merges.
 *
 * Layout: `manifest.json`, `database/smart_budget_db`, `files/savings_attachments/...`.
 */
object BackupManager {

    const val FORMAT = "smart-budget-backup"
    const val FORMAT_VERSION = 1
    const val ATTACHMENTS_DIR = "savings_attachments"
    private const val SAFETY_DIR = "backups"
    private const val SAFETY_COPIES_KEPT = 2
    private const val MANIFEST = "manifest.json"
    private const val DB_ENTRY = "database/$DATABASE_NAME"
    private const val ATTACHMENTS_PREFIX = "files/$ATTACHMENTS_DIR/"

    /** SharedPreferences files that travel with a backup. */
    val PREFERENCE_FILES = listOf("smart_budget_prefs", "savings_prefs")

    class BackupException(message: String) : Exception(message)

    data class Counts(val accounts: Int, val transactions: Int, val bills: Int, val savingsItems: Int, val attachments: Int)

    data class BackupInfo(val createdAt: Long, val dbVersion: Int, val counts: Counts)

    fun suggestedFileName(now: Long = System.currentTimeMillis()): String =
        "smart_budget_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(now)) + ".zip"

    /** Writes a full backup of [db] and this app's settings and attachments to [out]. */
    fun createBackup(context: Context, db: AppDatabase, out: OutputStream, now: Long = System.currentTimeMillis()): BackupInfo {
        val snapshot = File(context.cacheDir, "backup-snapshot-${System.nanoTime()}.db")
        try {
            snapshotDatabase(context, db, snapshot)
            val attachmentsRoot = File(context.filesDir, ATTACHMENTS_DIR)
            val attachments = attachmentsRoot.walkTopDown().filter { it.isFile }.toList()
            val info = BackupInfo(now, readDbVersion(snapshot), countsOf(snapshot, attachments.size))

            ZipOutputStream(out.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST))
                zip.write(manifest(context, info).toString(2).toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry(DB_ENTRY))
                snapshot.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                for (file in attachments) {
                    zip.putNextEntry(ZipEntry(ATTACHMENTS_PREFIX + file.relativeTo(attachmentsRoot).invariantSeparatorsPath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            return info
        } finally {
            snapshot.delete()
        }
    }

    /** Reads only the manifest, to show what a backup holds before the user confirms a restore. */
    fun readInfo(input: InputStream): BackupInfo {
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == MANIFEST) return parseInfo(JSONObject(zip.readBytes().decodeToString()))
                entry = zip.nextEntry
            }
        }
        throw BackupException("This file is not a Smart Budget backup.")
    }

    /** What is on the device right now, to compare with a backup before replacing it. */
    fun currentCounts(context: Context, db: AppDatabase): Counts {
        val snapshot = File(context.cacheDir, "count-snapshot-${System.nanoTime()}.db")
        try {
            snapshotDatabase(context, db, snapshot)
            val attachments = File(context.filesDir, ATTACHMENTS_DIR).walkTopDown().count { it.isFile }
            return countsOf(snapshot, attachments)
        } finally {
            snapshot.delete()
        }
    }

    /**
     * Keeps a copy of the current data in the app's own storage before a restore replaces it,
     * so a wrong file can be undone. Only the newest [SAFETY_COPIES_KEPT] copies are kept.
     */
    fun saveSafetyCopy(context: Context, db: AppDatabase): File {
        val dir = File(context.filesDir, SAFETY_DIR).apply { mkdirs() }
        val file = File(dir, "before-restore-" + suggestedFileName().removePrefix("smart_budget_backup_"))
        file.outputStream().use { createBackup(context, db, it) }
        dir.listFiles { f -> f.name.startsWith("before-restore-") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(SAFETY_COPIES_KEPT)
            ?.forEach { it.delete() }
        return file
    }

    /**
     * Replaces the database, settings and attachments with the backup in [input]. The file is
     * unpacked and checked completely before anything is touched; any problem throws
     * [BackupException] and leaves the current data as it was. The open database is closed, so
     * the caller must restart the app afterwards ([restartApp]).
     */
    fun restore(context: Context, input: InputStream): BackupInfo {
        val stage = File(context.cacheDir, "restore-stage").apply {
            deleteRecursively()
            mkdirs()
        }
        try {
            unzipSafely(input, stage)
            val manifestFile = File(stage, MANIFEST)
            if (!manifestFile.isFile) throw BackupException("This file is not a Smart Budget backup.")
            val manifest = JSONObject(manifestFile.readText())
            val info = parseInfo(manifest)
            val stagedDb = File(stage, DB_ENTRY)
            if (!stagedDb.isFile) throw BackupException("The backup has no database in it.")
            checkDatabase(stagedDb)

            // Everything checked: from here on the current data is replaced.
            AppDatabase.closeInstance()
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            for (suffix in listOf("", "-wal", "-shm", "-journal")) File(dbFile.path + suffix).delete()
            dbFile.parentFile?.mkdirs()
            stagedDb.copyTo(dbFile, overwrite = true)

            restorePreferences(context, manifest.optJSONObject("preferences"))

            val attachments = File(context.filesDir, ATTACHMENTS_DIR)
            attachments.deleteRecursively()
            val stagedAttachments = File(stage, ATTACHMENTS_PREFIX)
            if (stagedAttachments.isDirectory) stagedAttachments.copyRecursively(attachments, overwrite = true)
            return info
        } finally {
            stage.deleteRecursively()
        }
    }

    /** Starts the app afresh so every screen reopens the restored database. */
    fun restartApp(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        exitProcess(0)
    }

    // ---- Internals ---------------------------------------------------------------------------

    /** A consistent single-file copy of the live database, including what is still in the WAL. */
    private fun snapshotDatabase(context: Context, db: AppDatabase, target: File) {
        target.delete()
        val sqlite = db.openHelper.writableDatabase
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // SQLite 3.27+ (Android 11+) writes a clean copy in one statement.
            sqlite.execSQL("VACUUM INTO '" + target.absolutePath.replace("'", "''") + "'")
        } else {
            sqlite.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            context.getDatabasePath(DATABASE_NAME).copyTo(target, overwrite = true)
        }
    }

    private fun readDbVersion(file: File): Int =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }

    private fun countsOf(file: File, attachments: Int): Counts =
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            fun count(table: String, where: String = ""): Int {
                val exists = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use { it.moveToFirst() }
                if (!exists) return 0
                return db.rawQuery("SELECT COUNT(*) FROM $table $where", null).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            }
            Counts(
                accounts = count("accounts"),
                transactions = count("transactions", "WHERE isDismissed = 0"),
                bills = count("recurring_bills"),
                savingsItems = count("savings_items"),
                attachments = attachments
            )
        }

    /** Refuses a damaged database, or one from a newer app version this build cannot open. */
    private fun checkDatabase(file: File) {
        val (integrity, version) = try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val result = db.rawQuery("PRAGMA integrity_check", null).use { if (it.moveToFirst()) it.getString(0) else "" }
                result to db.version
            }
        } catch (e: Exception) {
            throw BackupException("The database in this backup cannot be opened.")
        }
        if (integrity != "ok") throw BackupException("The database in this backup is damaged.")
        if (version > DATABASE_VERSION) throw BackupException("This backup was made by a newer version of the app. Update the app first.")
        if (version < 4) throw BackupException("This backup is from a version of the app that is too old to restore.")
    }

    /** Unpacks [input] into [dir], refusing any entry that would land outside it. */
    private fun unzipSafely(input: InputStream, dir: File) {
        val root = dir.canonicalFile
        var found = false
        ZipInputStream(input.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                found = true
                val target = File(root, entry.name).canonicalFile
                if (!target.path.startsWith(root.path + File.separator)) throw BackupException("This backup file is not valid.")
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                entry = zip.nextEntry
            }
        }
        if (!found) throw BackupException("This file is not a Smart Budget backup.")
    }

    private fun manifest(context: Context, info: BackupInfo): JSONObject {
        val prefs = JSONObject()
        for (name in PREFERENCE_FILES) {
            val values = JSONObject()
            for ((key, value) in context.getSharedPreferences(name, Context.MODE_PRIVATE).all) {
                val typed = when (value) {
                    is String -> JSONObject().put("type", "string").put("value", value)
                    is Boolean -> JSONObject().put("type", "boolean").put("value", value)
                    is Int -> JSONObject().put("type", "int").put("value", value)
                    is Long -> JSONObject().put("type", "long").put("value", value)
                    is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
                    is Set<*> -> JSONObject().put("type", "stringSet").put("value", JSONArray(value.map { it.toString() }))
                    else -> null
                }
                if (typed != null) values.put(key, typed)
            }
            prefs.put(name, values)
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("formatVersion", FORMAT_VERSION)
            .put("createdAt", info.createdAt)
            .put("dbVersion", info.dbVersion)
            .put("counts", JSONObject()
                .put("accounts", info.counts.accounts)
                .put("transactions", info.counts.transactions)
                .put("bills", info.counts.bills)
                .put("savingsItems", info.counts.savingsItems)
                .put("attachments", info.counts.attachments))
            .put("preferences", prefs)
    }

    private fun parseInfo(manifest: JSONObject): BackupInfo {
        if (manifest.optString("format") != FORMAT) throw BackupException("This file is not a Smart Budget backup.")
        if (manifest.optInt("formatVersion") > FORMAT_VERSION) throw BackupException("This backup was made by a newer version of the app. Update the app first.")
        val counts = manifest.optJSONObject("counts") ?: JSONObject()
        return BackupInfo(
            createdAt = manifest.optLong("createdAt"),
            dbVersion = manifest.optInt("dbVersion"),
            counts = Counts(
                accounts = counts.optInt("accounts"),
                transactions = counts.optInt("transactions"),
                bills = counts.optInt("bills"),
                savingsItems = counts.optInt("savingsItems"),
                attachments = counts.optInt("attachments")
            )
        )
    }

    /** Replaces each backed-up settings file with exactly the values in the backup. */
    private fun restorePreferences(context: Context, prefs: JSONObject?) {
        if (prefs == null) return
        for (name in PREFERENCE_FILES) {
            val values = prefs.optJSONObject(name) ?: continue
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) {
                clear()
                for (key in values.keys()) {
                    val typed = values.getJSONObject(key)
                    when (typed.optString("type")) {
                        "string" -> putString(key, typed.getString("value"))
                        "boolean" -> putBoolean(key, typed.getBoolean("value"))
                        "int" -> putInt(key, typed.getInt("value"))
                        "long" -> putLong(key, typed.getLong("value"))
                        "float" -> putFloat(key, typed.getDouble("value").toFloat())
                        "stringSet" -> {
                            val array = typed.getJSONArray("value")
                            putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                        }
                    }
                }
            }
        }
    }
}
