package com.example.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Collections

object AppDebugLogger {
    private const val TAG = "SmartBudgetDebug"
    private const val MAX_LINES = 300

    private val buffer = Collections.synchronizedList(mutableListOf<String>())
    private var logFile: File? = null
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return

        initialized = true
        val dir = File(context.filesDir, "debug_logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        logFile = File(dir, "app_debug_log.txt")

        val existingLines = if (logFile!!.exists()) {
            logFile!!.readLines().takeLast(MAX_LINES)
        } else {
            emptyList()
        }

        buffer.clear()
        buffer.addAll(existingLines)

        log("App debug logger initialized")

        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            log("Uncaught exception on ${thread.name}: ${throwable.stackTraceToString()}")
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    fun log(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = "$timestamp $message"
        Log.d(TAG, line)

        synchronized(buffer) {
            buffer.add(line)
            while (buffer.size > MAX_LINES) {
                buffer.removeAt(0)
            }
        }

        writeToFile()
    }

    fun getLatestLogText(): String {
        return synchronized(buffer) {
            if (buffer.isEmpty()) {
                "No debug logs captured yet.\n"
            } else {
                buffer.joinToString(separator = "\n")
            }
        }
    }

    fun clearLogs() {
        synchronized(buffer) {
            buffer.clear()
        }
        writeToFile()
        log("Debug log cleared")
    }

    fun buildShareIntent(context: Context, subject: String = "Smart Budget debug log"): Intent {
        val text = getLatestLogText()
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(shareIntent, "Share debug log")
    }

    private fun writeToFile() {
        try {
            logFile?.writeText(getLatestLogText())
        } catch (_: Exception) {
            // Intentionally ignore file-write failures so the app remains stable.
        }
    }
}
