package com.example.data.savings

import com.example.data.local.entity.SavingsItemEntity

/** Totals for the top of the Savings page, or for one section or holder block. */
data class SavingsTotals(val invested: Double, val current: Double) {
    val gain: Double get() = current - invested
    val gainPct: Double get() = if (invested == 0.0) 0.0 else gain / invested * 100

    companion object {
        fun of(items: List<SavingsItemEntity>) = SavingsTotals(items.sumOf { it.investedAmount }, items.sumOf { it.currentValue })
    }
}

object SavingsSummary {

    /** Records that pass the page filters: a holder (null = everyone) and the chosen statuses. */
    fun filter(items: List<SavingsItemEntity>, holder: String?, statuses: Set<SavingsStatus>): List<SavingsItemEntity> =
        items.filter { item ->
            (holder == null || item.holder.equals(holder, ignoreCase = true)) && SavingsStatus.from(item.status) in statuses
        }

    /** Current value per section, largest first, for the allocation chart. Empty sections are left out. */
    fun allocation(items: List<SavingsItemEntity>): List<Pair<SavingsSection, Double>> =
        items.groupBy { SavingsSection.from(it.section) }
            .map { (section, list) -> section to list.sumOf { it.currentValue } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    /** The next [limit] active records with a maturity date today or later, soonest first. */
    fun upcomingMaturities(items: List<SavingsItemEntity>, today: Long, limit: Int = 3): List<SavingsItemEntity> {
        val from = SavingsCalculator.startOfDay(today)
        return items.filter { SavingsStatus.from(it.status) == SavingsStatus.ACTIVE && (it.maturityDate ?: -1) >= from }
            .sortedBy { it.maturityDate }
            .take(limit)
    }

    /**
     * Bank, cash and card balances plus the savings' current value, less loans against property.
     * Credit-card balances are stored negative (money owed), so they are simply added.
     */
    fun netWorth(accountBalances: List<Double>, items: List<SavingsItemEntity>): Double {
        val loans = items.sumOf { (SavingsDetails.fromJson(it.detailsJson) as? SavingsDetails.Land)?.loanOutstanding ?: 0.0 }
        return accountBalances.sum() + items.sumOf { it.currentValue } - loans
    }
}
