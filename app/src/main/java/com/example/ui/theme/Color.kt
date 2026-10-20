package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Status colours follow the theme: a deep shade on light backgrounds and a lighter one on dark
// backgrounds, so money in/out and warnings stay readable in dialogs, lists and charts alike.

/** Money in, on-track budgets. */
val IncomeGreen: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF86EFAC) else Color(0xFF166534)

val IncomeGreenContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF12351F) else Color(0xFFD7F5E3)

val IncomeOnGreenContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFBBF7D0) else Color(0xFF052E16)

/** Warnings: over budget, low balance, delete. */
val ExpenseRed: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFCA5A5) else Color(0xFFB91C1C)

val ExpenseRedContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF451818) else Color(0xFFFFE0E0)

val ExpenseOnRedContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFECACA) else Color(0xFF7F1D1D)

/** SMS waiting for review, near a limit. */
val PendingAmber: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFFFCD34D) else Color(0xFF92400E)

val PendingAmberContainer: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalIsDarkTheme.current) Color(0xFF3D3110) else Color(0xFFFBEFC5)

val LightOnSurface = Color(0xFF1B1B1F)
