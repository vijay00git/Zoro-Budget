package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Resolves a CategoryEntity.iconName (e.g. "Fastfood", "ShoppingCart") to the actual Material
 * icon it names. Falls back to a generic tag icon for unknown/custom-category values, since
 * there's currently no icon picker UI for user-created categories.
 */
fun categoryIconFor(iconName: String): ImageVector = when (iconName) {
    "Fastfood" -> Icons.Default.Fastfood
    "ShoppingCart" -> Icons.Default.ShoppingCart
    "ShoppingBag" -> Icons.Default.ShoppingBag
    "Receipt" -> Icons.Default.Receipt
    "Payments" -> Icons.Default.Payments
    "DirectionsCar" -> Icons.Default.DirectionsCar
    "Movie" -> Icons.Default.Movie
    "LocalHospital" -> Icons.Default.LocalHospital
    "Savings" -> Icons.Default.Savings
    else -> Icons.Default.Label
}
