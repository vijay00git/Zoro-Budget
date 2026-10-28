package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.data.local.DemoData

/**
 * First-install state: whether to seed demo data, and the welcome tour that follows it.
 *
 * Demo data and the tour are only for a brand-new install. Someone updating the app keeps their
 * own data and never sees either.
 */
object FirstRun {
    // Not part of backups: it describes this installation, not the user's data.
    private const val INSTALL_PREFS = "install_prefs"
    private const val KEY_FIRST_RUN_HANDLED = "first_run_handled"

    // Kept with the other settings, so a backup and its database always travel together and the
    // demo ids can never point at rows of a different database.
    const val SETTINGS_PREFS = "smart_budget_prefs"
    private const val KEY_DEMO_DATA = "demo_data"
    private const val KEY_TOUR_PENDING = "tour_pending"

    /**
     * True exactly once, on the first launch of a fresh install. An update (the app was
     * installed before and has since been replaced) returns false.
     */
    fun claimDemoSeeding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(INSTALL_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_FIRST_RUN_HANDLED, false)) return false
        prefs.edit(commit = true) { putBoolean(KEY_FIRST_RUN_HANDLED, true) }
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return isFreshInstall(info.firstInstallTime, info.lastUpdateTime)
    }

    /** Android sets both times on install; an update moves only [lastUpdateTime]. */
    fun isFreshInstall(firstInstallTime: Long, lastUpdateTime: Long) = firstInstallTime == lastUpdateTime

    fun recordDemoData(prefs: SharedPreferences, demo: DemoData) = prefs.edit {
        putString(KEY_DEMO_DATA, demo.toJson())
        putBoolean(KEY_TOUR_PENDING, true)
    }

    fun demoData(prefs: SharedPreferences): DemoData? = DemoData.fromJson(prefs.getString(KEY_DEMO_DATA, null))

    fun forgetDemoData(prefs: SharedPreferences) = prefs.edit { remove(KEY_DEMO_DATA) }

    fun isTourPending(prefs: SharedPreferences) = prefs.getBoolean(KEY_TOUR_PENDING, false)

    fun finishTour(prefs: SharedPreferences) = prefs.edit { remove(KEY_TOUR_PENDING) }
}
