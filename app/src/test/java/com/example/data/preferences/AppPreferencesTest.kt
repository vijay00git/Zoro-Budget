package com.example.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
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

        assertThat(prefs.currencySymbol).isEqualTo("₹")
        assertThat(prefs.savingsGoalTarget).isWithin(0.001).of(1200.0)
        assertThat(prefs.lowBalanceThreshold).isWithin(0.001).of(300.0)
        assertThat(prefs.themeMode).isEqualTo("DARK")
    }
}
