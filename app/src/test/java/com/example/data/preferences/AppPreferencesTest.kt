package com.example.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun savesAndLoadsCurrencyAndBudgetPreferences() {
        AppPreferences.save(
            context = context,
            currencySymbol = "₹",
            savingsGoalTarget = 1200.0,
            lowBalanceThreshold = 300.0,
            themeMode = "DARK"
        )

        val prefs = AppPreferences.load(context)

        assertEquals("₹", prefs.currencySymbol)
        assertEquals(1200.0, prefs.savingsGoalTarget, 0.001)
        assertEquals(300.0, prefs.lowBalanceThreshold, 0.001)
        assertEquals("DARK", prefs.themeMode)
    }

    @Test
    fun aThemeSavedByAnOlderVersionSwitchesToSystemOnceThenChoicesStick() {
        // What an install from before the redesign has on disk: LIGHT, and no migration flag.
        context.getSharedPreferences("smart_budget_prefs", Context.MODE_PRIVATE).edit().clear().putString("theme_mode", "LIGHT").commit()

        assertEquals("SYSTEM", AppPreferences.load(context).themeMode)

        AppPreferences.save(context, themeMode = "LIGHT")
        assertEquals("LIGHT", AppPreferences.load(context).themeMode)
        assertEquals("LIGHT", AppPreferences.load(context).themeMode)
    }

    @Test
    fun loadReturnsDefaultsWhenNothingSaved() {
        val prefs = AppPreferences.load(context)

        assertEquals("$", prefs.currencySymbol)
        assertEquals(500.0, prefs.savingsGoalTarget, 0.001)
        assertEquals(200.0, prefs.lowBalanceThreshold, 0.001)
        assertEquals("SYSTEM", prefs.themeMode)
    }

    @Test
    fun savePartiallyUpdatesOnlyGivenFields() {
        AppPreferences.save(context = context, currencySymbol = "€", themeMode = "LIGHT")
        AppPreferences.save(context = context, savingsGoalTarget = 999.0)

        val prefs = AppPreferences.load(context)

        assertEquals("€", prefs.currencySymbol)
        assertEquals("LIGHT", prefs.themeMode)
        assertEquals(999.0, prefs.savingsGoalTarget, 0.001)
    }
}
