package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.components.accountInitials
import com.example.ui.components.suggestedIconName
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.accentFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {

    @Test
    fun theUsersOwnCategoriesGetFittingIcons() {
        assertEquals("SetMeal", suggestedIconName("Non-Veg"))
        assertEquals("Eco", suggestedIconName("Vegetable"))
        assertEquals("Grain", suggestedIconName("Ulundai"))
        assertEquals("Cookie", suggestedIconName("snacks"))
        assertEquals("Home", suggestedIconName("Rent EB water"))
        assertEquals("ElectricBolt", suggestedIconName("EB bill"))
        assertEquals("LocalGasStation", suggestedIconName("Petrol"))
        assertEquals("PhoneAndroid", suggestedIconName("Jio recharge"))
        assertEquals("PhoneAndroid", suggestedIconName("Recharges"))
        assertEquals("Person", suggestedIconName("Wife Expenses"))
        assertEquals("TwoWheeler", suggestedIconName("Rapido"))
        assertEquals("Spa", suggestedIconName("Cosmetics"))
        assertEquals("VolunteerActivism", suggestedIconName("Given to"))
        assertEquals("AccountBalance", suggestedIconName("EMI"))
        // A person's name gives no hint, so it keeps the plain icon until the user picks one.
        assertNull(suggestedIconName("Vijay"))
        // Short hints must match whole words, not pieces of other words.
        assertNull(suggestedIconName("Debit misc"))
    }

    @Test
    fun accountBadgesUseShortReadableInitials() {
        assertEquals("HD", accountInitials("HDFC Bank Account"))
        assertEquals("IB", accountInitials("Indian Bank Savings"))
        assertEquals("SB", accountInitials("SBI Bank"))
        assertEquals("CA", accountInitials("Cash"))
        assertEquals("?", accountInitials("  "))
    }

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return maxOf(la, lb) / minOf(la, lb)
    }

    @Test
    fun everyAccentIsReadableInLightAndDarkMode() {
        // The picker's colours plus the bright ones older categories were created with.
        val colours = AccentPalette + listOf("#3B82F6", "#10B981", "#F59E0B", "#FDE047", "#64748B", "#000000", "#FFFFFF")
        for (hex in colours) {
            val base = Color(java.lang.Long.parseLong(hex.removePrefix("#"), 16) or 0xFF000000)
            for (dark in listOf(false, true)) {
                val accent = accentFor(base, dark)
                assertTrue("$hex ink on fill (dark=$dark)", contrast(accent.ink, accent.fill) >= 4.4f)
                assertTrue("$hex white on solid (dark=$dark)", contrast(Color.White, accent.solid) >= 4.4f)
            }
        }
    }
}
