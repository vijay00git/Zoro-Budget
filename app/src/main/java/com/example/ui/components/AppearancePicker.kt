package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.accentOf

/**
 * Pick a category's colour and icon, with a live preview of how it looks in lists. [iconName]
 * may be "Label" for "not chosen yet": the preview then shows the icon suggested from [name].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearancePicker(
    name: String,
    iconName: String,
    colorHex: String,
    onIconChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentOf(colorHex)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryAvatar(iconName = iconName, categoryName = name, colorHex = colorHex, size = 52.dp)
            Text(
                text = name.ifBlank { stringResource(R.string.appearance_preview) },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(stringResource(R.string.appearance_colour), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Keep a colour the category already had even if it is not in the palette.
            (if (AccentPalette.any { it.equals(colorHex, ignoreCase = true) }) AccentPalette else listOf(colorHex) + AccentPalette).forEach { hex ->
                val selected = hex.equals(colorHex, ignoreCase = true)
                val swatch = accentOf(hex).solid
                IconButton(
                    onClick = { onColorChange(hex) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .testTag("appearance_colour_$hex")
                ) {
                    if (selected) Icon(Icons.Default.Check, contentDescription = stringResource(R.string.appearance_selected), tint = Color.White)
                }
            }
        }

        Text(stringResource(R.string.appearance_icon), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        CategoryIconGroups.forEach { group ->
            Text(stringResource(group.title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                group.icons.forEach { (key, icon) ->
                    val selected = key == iconName
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) accent.fill else MaterialTheme.colorScheme.surfaceVariant)
                            .then(if (selected) Modifier.border(2.dp, accent.ink, RoundedCornerShape(12.dp)) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { onIconChange(key) }, modifier = Modifier.size(44.dp).testTag("appearance_icon_$key")) {
                            Icon(icon, contentDescription = key, tint = if (selected) accent.ink else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
