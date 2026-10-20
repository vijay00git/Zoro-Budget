package com.example.data.savings

import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity

/** The latest shared market rates entered on the Gold and Silver sections. */
data class MarketRates(
    val gold24PerGram: Double? = null,
    val gold22PerGram: Double? = null,
    val silverPerKg: Double? = null,
    val goldDate: Long? = null, // when the 24K rate was last entered
    val silverDate: Long? = null
)

/**
 * Turns a savings record's details, history and the shared rates into the two numbers the
 * Savings page totals: what went in and what it is worth today.
 */
object SavingsValuation {

    data class Value(
        val invested: Double,
        val current: Double,
        val missingRate: Boolean = false // gold/silver valued at purchase price until a rate is entered
    )

    fun value(
        item: SavingsItemEntity,
        details: SavingsDetails?,
        entries: List<SavingsEntryEntity>,
        rates: MarketRates,
        today: Long
    ): Value {
        val invested = if (entries.isNotEmpty()) SavingsCalculator.summarizeEntries(entries).netInvested else fallbackInvested(details)
        val c = SavingsCalculator
        return when (details) {
            is SavingsDetails.Fd -> {
                val maturity = item.maturityDate ?: return Value(invested, details.principal)
                Value(invested, c.fixedDeposit(details.principal, details.ratePct, item.startDate, maturity, details.compounding, details.payout, today).valueToday)
            }
            is SavingsDetails.Rd -> {
                val paid = entries.count { EntryType.from(it.type) == EntryType.INVEST }.takeIf { entries.isNotEmpty() }
                Value(invested, c.recurringDeposit(details.installment, details.ratePct, details.tenureMonths, item.startDate, today, paid).valueToday)
            }
            is SavingsDetails.Stock -> Value(invested, details.quantity * details.currentPrice)
            is SavingsDetails.MutualFund -> Value(invested, details.units * details.nav)
            // Without a weight there is nothing to value at today's rate; fall back to what was paid, if known.
            is SavingsDetails.Gold -> {
                val grams = details.grams ?: return Value(invested, details.purchaseAmount ?: 0.0)
                val rate24 = rates.gold24PerGram ?: return Value(invested, details.purchaseAmount ?: 0.0, missingRate = true)
                Value(invested, c.gold(grams, details.purity, rate24, rates.gold22PerGram, details.purchaseAmount ?: 0.0).value)
            }
            is SavingsDetails.Silver -> {
                val grams = details.grams ?: return Value(invested, details.purchaseAmount ?: 0.0)
                val rate = rates.silverPerKg ?: return Value(invested, details.purchaseAmount ?: 0.0, missingRate = true)
                Value(invested, c.silver(grams, details.purityPct, rate, details.purchaseAmount ?: 0.0).value)
            }
            is SavingsDetails.Land -> Value(invested, details.currentValue ?: 0.0)
            is SavingsDetails.Scheme -> Value(invested, details.balance)
            is SavingsDetails.Insurance -> {
                val paid = c.insurance(details.premium, details.premiumFrequency, item.startDate, details.termYears, today).amountPaid
                Value(invested, paid + details.bonusSoFar)
            }
            // Once the prize is taken the member has had their money; until then it is what was paid in.
            is SavingsDetails.Chit -> Value(invested, if (details.prizeAmount > 0) 0.0 else c.chit(details).amountPaid)
            is SavingsDetails.Bond -> Value(invested, details.faceValue)
            is SavingsDetails.LoanGiven -> Value(invested, c.loanGiven(details, item.startDate, today).outstanding)
            is SavingsDetails.Generic -> Value(invested, details.currentValue)
            null -> Value(invested, item.currentValue)
        }
    }

    /** What went in, when the record has no investment history yet. */
    private fun fallbackInvested(details: SavingsDetails?): Double = when (details) {
        is SavingsDetails.Fd -> details.principal
        is SavingsDetails.Stock -> details.quantity * details.avgPrice
        is SavingsDetails.Gold -> details.purchaseAmount ?: 0.0
        is SavingsDetails.Silver -> details.purchaseAmount ?: 0.0
        is SavingsDetails.Land -> (details.purchasePrice ?: 0.0) + details.stampDutyAndRegistration + details.brokerage + details.otherCosts
        is SavingsDetails.Bond -> details.faceValue
        is SavingsDetails.LoanGiven -> details.amount
        is SavingsDetails.Chit -> details.monthlyInstallment * details.monthsPaid
        is SavingsDetails.Generic -> details.invested
        is SavingsDetails.MutualFund -> details.lumpSum ?: 0.0
        else -> 0.0
    }

    /**
     * The investment history a new record starts with: one entry for a lump sum, or one per
     * month for an RD or SIP up to [today]. The user edits or deletes these afterwards.
     */
    fun suggestedEntries(details: SavingsDetails, startDate: Long, today: Long): List<SavingsEntryEntity> {
        fun one(amount: Double, units: Double? = null, unitPrice: Double? = null) =
            if (amount > 0) listOf(SavingsEntryEntity(itemId = 0, date = startDate, amount = amount, units = units, unitPrice = unitPrice)) else emptyList()

        return when (details) {
            is SavingsDetails.Fd -> one(details.principal)
            is SavingsDetails.Rd -> {
                val end = minOf(today, SavingsCalculator.addMonths(startDate, details.tenureMonths - 1))
                SavingsCalculator.monthlySchedule(startDate, details.installmentDay, end)
                    .take(details.tenureMonths)
                    .map { SavingsEntryEntity(itemId = 0, date = it, amount = details.installment) }
            }
            is SavingsDetails.MutualFund -> {
                val amount = details.sipAmount
                val day = details.sipDay
                val sip = if (amount == null || amount <= 0 || day == null) {
                    emptyList()
                } else {
                    SavingsCalculator.monthlySchedule(startDate, day, minOf(today, details.sipStoppedDate ?: today))
                        .map { SavingsEntryEntity(itemId = 0, date = it, amount = amount) }
                }
                one(details.lumpSum ?: 0.0) + sip
            }
            is SavingsDetails.Stock -> one(details.quantity * details.avgPrice, details.quantity, details.avgPrice)
            is SavingsDetails.Gold -> one(details.purchaseAmount ?: 0.0, details.grams)
            is SavingsDetails.Silver -> one(details.purchaseAmount ?: 0.0, details.grams)
            is SavingsDetails.Land -> one((details.purchasePrice ?: 0.0) + details.stampDutyAndRegistration + details.brokerage + details.otherCosts)
            is SavingsDetails.Bond -> one(details.faceValue)
            is SavingsDetails.LoanGiven -> one(details.amount)
            is SavingsDetails.Generic -> one(details.invested)
            else -> emptyList()
        }
    }
}
