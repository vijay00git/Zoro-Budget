package com.example.ui.savings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.GoldForm
import com.example.data.savings.MarketRates
import com.example.data.savings.SavingsCalculator
import com.example.data.savings.SavingsDetails
import com.example.data.savings.SavingsSection
import com.example.data.savings.SavingsStatus
import com.example.data.savings.SavingsSummary
import com.example.data.savings.SavingsTotals
import com.example.data.savings.SnapshotKeys
import com.example.ui.components.formatMoney

/**
 * The Savings tab: a record of savings and assets the user enters by hand. It never touches the
 * ledger. The list, a record's detail page and the add/edit form all live inside this tab.
 */
@Composable
fun SavingsScreen(
    viewModel: SavingsViewModel,
    accountBalances: List<Double>,
    currencySymbol: String,
    snackbarHostState: SnackbarHostState
) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val items by viewModel.visibleItems.collectAsStateWithLifecycle()
    val entriesByItem by viewModel.entriesByItem.collectAsStateWithLifecycle()
    val holders by viewModel.holders.collectAsStateWithLifecycle()
    val holderFilter by viewModel.holderFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val groupByHolder by viewModel.groupByHolder.collectAsStateWithLifecycle()
    val collapsed by viewModel.collapsedGroups.collectAsStateWithLifecycle()
    val undo by viewModel.undo.collectAsStateWithLifecycle()
    val rates by viewModel.rates.collectAsStateWithLifecycle()
    var editingRate by remember { mutableStateOf<SavingsSection?>(null) }

    var openItemId by rememberSaveable { mutableStateOf<Long?>(null) }
    var formSection by remember { mutableStateOf<SavingsSection?>(null) }
    var formItem by remember { mutableStateOf<SavingsItemEntity?>(null) }
    var pickSection by remember { mutableStateOf(false) }

    val undoMessage = undo?.let { stringResource(R.string.savings_deleted, it.item.name) }
    val undoLabel = stringResource(R.string.savings_undo)
    LaunchedEffect(undo) {
        val deleted = undo ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(undoMessage ?: "", actionLabel = undoLabel, duration = SnackbarDuration.Long)
        viewModel.clearUndo()
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(deleted)
    }

    val openItem = openItemId?.let { id -> allItems.firstOrNull { it.id == id } }
    if (openItem != null) {
        SavingsDetailScreen(
            item = openItem,
            entries = entriesByItem[openItem.id].orEmpty(),
            rates = rates,
            currencySymbol = currencySymbol,
            onBack = { openItemId = null },
            onEdit = {
                formItem = openItem
                formSection = SavingsSection.from(openItem.section)
            },
            onDelete = {
                openItemId = null
                viewModel.deleteItem(openItem)
            },
            onSaveEntry = { viewModel.saveEntry(it) },
            onDeleteEntry = { viewModel.deleteEntry(it) }
        )
    } else {
        SavingsList(
            items = items,
            allCount = allItems.size,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            holders = holders,
            holderFilter = holderFilter,
            statusFilter = statusFilter,
            groupByHolder = groupByHolder,
            collapsed = collapsed,
            onHolderFilter = viewModel::setHolderFilter,
            onToggleStatus = viewModel::toggleStatus,
            onGroupByHolder = viewModel::setGroupByHolder,
            onToggleGroup = viewModel::toggleCollapsed,
            rates = rates,
            onEditRate = { editingRate = it },
            onItemClick = { openItemId = it.id },
            onAdd = { section ->
                formItem = null
                if (section == null) pickSection = true else formSection = section
            }
        )
    }

    if (pickSection) {
        AlertDialog(
            onDismissRequest = { pickSection = false },
            title = { Text(stringResource(R.string.savings_pick_section)) },
            text = {
                Column {
                    availableSections.forEach { s ->
                        val st = s.style()
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                pickSection = false
                                formSection = s
                            }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(st.icon, contentDescription = null, tint = st.color)
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(st.title), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickSection = false }) { Text(stringResource(R.string.savings_cancel)) } }
        )
    }

    editingRate?.let { section ->
        RateDialog(
            section = section,
            rates = rates,
            currencySymbol = currencySymbol,
            onDismiss = { editingRate = null },
            onSave = { gold24, gold22, silver ->
                gold24?.let { viewModel.setRate(SnapshotKeys.GOLD_24K, it) }
                gold22?.let { viewModel.setRate(SnapshotKeys.GOLD_22K, it) }
                silver?.let { viewModel.setRate(SnapshotKeys.SILVER_KG, it) }
                editingRate = null
            }
        )
    }

    formSection?.let { section ->
        SavingsFormDialog(
            section = section,
            existing = formItem,
            holders = holders,
            defaultHolder = holderFilter,
            onDismiss = {
                formSection = null
                formItem = null
            },
            onSave = { item, details ->
                val isNew = item.id == 0L
                viewModel.saveItem(item, details) { id -> if (isNew) openItemId = id }
                formSection = null
                formItem = null
            }
        )
    }
}

@Composable
private fun SavingsList(
    items: List<SavingsItemEntity>,
    allCount: Int,
    accountBalances: List<Double>,
    currencySymbol: String,
    holders: List<String>,
    holderFilter: String?,
    statusFilter: Set<SavingsStatus>,
    groupByHolder: Boolean,
    collapsed: Set<String>,
    onHolderFilter: (String?) -> Unit,
    onToggleStatus: (SavingsStatus) -> Unit,
    onGroupByHolder: (Boolean) -> Unit,
    onToggleGroup: (String) -> Unit,
    rates: MarketRates,
    onEditRate: (SavingsSection) -> Unit,
    onItemClick: (SavingsItemEntity) -> Unit,
    onAdd: (SavingsSection?) -> Unit
) {
    val today = remember { System.currentTimeMillis() }
    val totals = SavingsTotals.of(items)

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("savings_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryCard(
                totals = totals,
                netWorth = if (holderFilter == null) SavingsSummary.netWorth(accountBalances, items) else null,
                maturities = SavingsSummary.upcomingMaturities(items, today),
                currencySymbol = currencySymbol,
                today = today,
                onAdd = { onAdd(null) },
                onItemClick = onItemClick
            )
        }

        if (holders.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = holderFilter == null, onClick = { onHolderFilter(null) }, label = { Text(stringResource(R.string.savings_holder_all)) })
                    }
                    items(holders) { h ->
                        FilterChip(
                            selected = h.equals(holderFilter, ignoreCase = true),
                            onClick = { onHolderFilter(h) },
                            label = { Text(h) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("savings_holder_chip_$h")
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                item {
                    FilterChip(selected = !groupByHolder, onClick = { onGroupByHolder(false) }, label = { Text(stringResource(R.string.savings_group_section)) })
                }
                item {
                    FilterChip(
                        selected = groupByHolder, onClick = { onGroupByHolder(true) }, label = { Text(stringResource(R.string.savings_group_holder)) },
                        modifier = Modifier.testTag("savings_group_by_holder")
                    )
                }
                item { Spacer(Modifier.width(8.dp)) }
                items(SavingsStatus.entries) { s ->
                    FilterChip(selected = s in statusFilter, onClick = { onToggleStatus(s) }, label = { Text(stringResource(s.label())) })
                }
            }
        }

        if (allCount > 0 && items.isEmpty()) {
            item {
                Text(stringResource(R.string.savings_no_match), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (groupByHolder) {
            val byHolder = items.groupBy { it.holder }.toSortedMap(String.CASE_INSENSITIVE_ORDER)
            byHolder.forEach { (holder, list) ->
                val key = "holder:$holder"
                val open = key !in collapsed
                item(key = key) {
                    GroupHeader(
                        title = holder, icon = Icons.Default.Person, color = MaterialTheme.colorScheme.primary,
                        count = list.size, totals = SavingsTotals.of(list), currencySymbol = currencySymbol,
                        expanded = open, onToggle = { onToggleGroup(key) }, onAdd = null
                    )
                }
                if (open) items(list, key = { it.id }) { ItemCard(it, currencySymbol, showSection = true, onClick = { onItemClick(it) }) }
            }
        } else {
            availableSections.forEach { section ->
                val list = items.filter { it.section == section.name }
                val key = "section:${section.name}"
                val open = key !in collapsed
                val style = section.style()
                item(key = key) {
                    GroupHeader(
                        title = stringResource(style.title), icon = style.icon, color = style.color,
                        count = list.size, totals = SavingsTotals.of(list), currencySymbol = currencySymbol,
                        expanded = open, onToggle = { onToggleGroup(key) }, onAdd = { onAdd(section) },
                        modifier = Modifier.testTag("savings_section_${section.name}")
                    )
                }
                if (open && (section == SavingsSection.GOLD || section == SavingsSection.SILVER)) {
                    item(key = "$key:rate") { RateRow(section, rates, list, currencySymbol, onEdit = { onEditRate(section) }) }
                }
                if (open) {
                    // Once anything is recorded, empty sections keep just their header and + button.
                    if (list.isEmpty() && allCount == 0) {
                        item(key = "$key:empty") {
                            TextButton(onClick = { onAdd(section) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(style.addFirst))
                            }
                        }
                    } else if (list.isNotEmpty()) {
                        items(list, key = { it.id }) { ItemCard(it, currencySymbol, showSection = false, onClick = { onItemClick(it) }) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SummaryCard(
    totals: SavingsTotals,
    netWorth: Double?,
    maturities: List<SavingsItemEntity>,
    currencySymbol: String,
    today: Long,
    onAdd: () -> Unit,
    onItemClick: (SavingsItemEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("savings_summary"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.savings_total_value), style = MaterialTheme.typography.labelMedium)
                    Text(formatMoney(currencySymbol, totals.current), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }
                FilledTonalButton(onClick = onAdd, modifier = Modifier.testTag("savings_add")) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.savings_add))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.savings_invested_amount, formatMoney(currencySymbol, totals.invested)), style = MaterialTheme.typography.bodyMedium)
            if (totals.invested > 0 || totals.current > 0) GainText(currencySymbol, totals.gain, totals.gainPct)
            if (netWorth != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LabelValueRow(stringResource(R.string.savings_net_worth), formatMoney(currencySymbol, netWorth), detail = stringResource(R.string.savings_net_worth_detail), emphasize = true)
            }
            if (maturities.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.savings_upcoming_maturities), style = MaterialTheme.typography.labelMedium)
                maturities.forEach { m ->
                    val days = SavingsCalculator.daysBetween(today, m.maturityDate!!).toInt()
                    Row(modifier = Modifier.fillMaxWidth().clickable { onItemClick(m) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatDay(m.maturityDate) + " · " + pluralStringResource(R.plurals.savings_in_days, days, days),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    count: Int,
    totals: SavingsTotals,
    currencySymbol: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAdd: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title + " (" + count + ")", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                if (count > 0) {
                    Text(formatMoney(currencySymbol, totals.current), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        formatSignedMoney(currencySymbol, totals.gain), color = gainColor(totals.gain),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (onAdd != null) {
                IconButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = stringResource(R.string.savings_add_to, title), tint = color) }
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(if (expanded) R.string.savings_collapse else R.string.savings_expand)
            )
        }
    }
}

@Composable
private fun ItemCard(item: SavingsItemEntity, currencySymbol: String, showSection: Boolean, onClick: () -> Unit) {
    val section = SavingsSection.from(item.section)
    val style = section.style()
    val totals = SavingsTotals(item.investedAmount, item.currentValue)
    val status = SavingsStatus.from(item.status)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("savings_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(style.color))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(
                    if (showSection) stringResource(style.title) else null,
                    subTypeLabel(item)?.let { stringResource(it) },
                    item.institution,
                    item.holder,
                    item.maturityDate?.let { stringResource(R.string.savings_matures_on, formatDay(it)) }
                ).joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (status != SavingsStatus.ACTIVE) {
                    Text(stringResource(status.label()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(currencySymbol, totals.current), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(formatSignedMoney(currencySymbol, totals.gain), color = gainColor(totals.gain), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Today's gold or silver rate for the section, plus the total weight held. Every item is valued from it. */
@Composable
private fun RateRow(section: SavingsSection, rates: MarketRates, items: List<SavingsItemEntity>, currencySymbol: String, onEdit: () -> Unit) {
    val gold = section == SavingsSection.GOLD
    val hasRate = if (gold) rates.gold24PerGram != null else rates.silverPerKg != null
    val weights = remember(items) {
        items.mapNotNull { SavingsDetails.fromJson(it.detailsJson) }.groupBy {
            when (it) {
                is SavingsDetails.Gold -> if (it.form == GoldForm.SGB) "SGB" else "${it.purity.carats}K"
                is SavingsDetails.Silver -> "${formatPlain(it.purityPct)}%"
                else -> ""
            }
        }.mapValues { (_, list) -> list.sumOf { (it as? SavingsDetails.Gold)?.grams ?: (it as? SavingsDetails.Silver)?.grams ?: 0.0 } }
    }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("savings_rate_${section.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (hasRate) {
                    val text = if (gold) {
                        listOfNotNull(
                            "24K " + formatMoney(currencySymbol, rates.gold24PerGram!!) + "/g",
                            rates.gold22PerGram?.let { "22K " + formatMoney(currencySymbol, it) + "/g" }
                        ).joinToString(" · ")
                    } else {
                        formatMoney(currencySymbol, rates.silverPerKg!!) + "/kg"
                    }
                    Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                    (if (gold) rates.goldDate else rates.silverDate)?.let {
                        Text(stringResource(R.string.savings_rate_as_of, formatDay(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(stringResource(if (gold) R.string.savings_rate_missing_gold else R.string.savings_rate_missing_silver), style = MaterialTheme.typography.bodyMedium)
                }
                if (weights.isNotEmpty()) {
                    Text(
                        stringResource(R.string.savings_rate_total_weight, weights.entries.joinToString(" · ") { (k, g) -> "$k ${formatPlain(Math.round(g * 1000) / 1000.0)} g" }),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TextButton(onClick = onEdit) { Text(stringResource(if (hasRate) R.string.savings_rate_update else R.string.savings_rate_enter)) }
        }
    }
}

@Composable
private fun RateDialog(
    section: SavingsSection,
    rates: MarketRates,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (gold24: Double?, gold22: Double?, silverKg: Double?) -> Unit
) {
    val gold = section == SavingsSection.GOLD
    var first by remember { mutableStateOf((if (gold) rates.gold24PerGram else rates.silverPerKg)?.let { formatPlain(it) } ?: "") }
    var second by remember { mutableStateOf(rates.gold22PerGram?.let { formatPlain(it) } ?: "") }
    val firstValue = parseAmount(first)
    val secondValue = parseAmount(second)
    val valid = firstValue != null && firstValue > 0 && (second.isBlank() || (secondValue != null && secondValue > 0))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (gold) R.string.savings_rate_gold_title else R.string.savings_rate_silver_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (gold) {
                    NumberField(first, { first = it }, R.string.savings_rate_gold24, suffix = currencySymbol + "/g")
                    NumberField(second, { second = it }, R.string.savings_rate_gold22, suffix = currencySymbol + "/g")
                    Text(stringResource(R.string.savings_rate_gold22_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    NumberField(first, { first = it }, R.string.savings_rate_silver, suffix = currencySymbol + "/kg")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                if (gold) onSave(firstValue, secondValue, null) else onSave(null, null, firstValue)
            }) { Text(stringResource(R.string.savings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.savings_cancel)) } }
    )
}
