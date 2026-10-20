package com.example.data.savings

import com.example.data.local.entity.SavingsEntryEntity
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Savings & assets arithmetic. Every function is pure and returns its intermediate numbers as
 * well as the answer, so the record screen can show each step of the calculation. Dates are
 * epoch millis; rates are % per year. Formulas follow how Indian banks and markets quote them.
 */
object SavingsCalculator {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    // ---- Dates -------------------------------------------------------------------------------

    private fun cal(millis: Long): Calendar = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    fun startOfDay(millis: Long): Long = cal(millis).timeInMillis

    /** [millis] moved by [months], keeping the day of month (clamped to short months). */
    fun addMonths(millis: Long, months: Int): Long {
        val c = cal(millis)
        val day = c.get(Calendar.DAY_OF_MONTH)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, months)
        c.set(Calendar.DAY_OF_MONTH, min(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
        return c.timeInMillis
    }

    fun daysBetween(from: Long, to: Long): Long = Math.round((startOfDay(to) - startOfDay(from)).toDouble() / DAY_MS)

    /** Whole calendar months from [from] to [to] (0 when [to] is before [from]). */
    fun fullMonthsBetween(from: Long, to: Long): Int {
        if (startOfDay(to) <= startOfDay(from)) return 0
        val a = cal(from)
        val b = cal(to)
        var months = (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12 + (b.get(Calendar.MONTH) - a.get(Calendar.MONTH))
        if (addMonths(from, months) > startOfDay(to)) months--
        return max(months, 0)
    }

    /**
     * Years from [from] to [to] the way deposits are quoted: whole months / 12 plus leftover
     * days / 365. A 2-year FD is exactly 2.0, whatever leap days fall inside it.
     */
    fun yearsBetween(from: Long, to: Long): Double {
        val months = fullMonthsBetween(from, to)
        val leftoverDays = daysBetween(addMonths(from, months), to).coerceAtLeast(0)
        return months / 12.0 + leftoverDays / 365.0
    }

    /** First year of the Indian financial year (April-March) that [millis] falls in: Jan 2026 -> 2025. */
    fun financialYearOf(millis: Long): Int {
        val c = cal(millis)
        val year = c.get(Calendar.YEAR)
        return if (c.get(Calendar.MONTH) >= Calendar.APRIL) year else year - 1
    }

    // ---- Investment history ------------------------------------------------------------------

    data class EntrySummary(
        val totalInvested: Double, // sum of INVEST entries
        val totalWithdrawn: Double,
        val totalIncome: Double, // interest, dividends, rent received
        val netInvested: Double, // invested - withdrawn
        val investedByFinancialYear: Map<Int, Double> // FY start year -> INVEST total, ascending
    )

    fun summarizeEntries(entries: List<SavingsEntryEntity>): EntrySummary {
        var invested = 0.0
        var withdrawn = 0.0
        var income = 0.0
        val byFy = sortedMapOf<Int, Double>()
        for (e in entries) {
            when (EntryType.from(e.type)) {
                EntryType.INVEST -> {
                    invested += e.amount
                    val fy = financialYearOf(e.date)
                    byFy[fy] = (byFy[fy] ?: 0.0) + e.amount
                }
                EntryType.WITHDRAW -> withdrawn += e.amount
                EntryType.INCOME -> income += e.amount
            }
        }
        return EntrySummary(invested, withdrawn, income, invested - withdrawn, byFy)
    }

    /**
     * Monthly instalment dates on [day] (clamped to short months) from [start] up to and
     * including [until]. Used to pre-fill RD and SIP history; the user deletes missed months.
     */
    fun monthlySchedule(start: Long, day: Int, until: Long): List<Long> {
        val c = cal(start)
        c.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
        var date = c.timeInMillis
        if (date < startOfDay(start)) date = monthOnDay(date, 1, day)
        val end = startOfDay(until)
        val dates = mutableListOf<Long>()
        while (date <= end) {
            dates += date
            date = monthOnDay(date, 1, day)
        }
        return dates
    }

    private fun monthOnDay(millis: Long, months: Int, day: Int): Long {
        val c = cal(millis)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, months)
        c.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
        return c.timeInMillis
    }

    // ---- Returns -----------------------------------------------------------------------------

    data class CashFlow(val date: Long, val amount: Double) // money out negative, money in positive

    /**
     * Annualised return of dated cash flows (XIRR, actual days / 365). Null unless there is at
     * least one outflow and one inflow, or when no rate between -99% and +1000% fits.
     */
    fun xirr(flows: List<CashFlow>): Double? {
        if (flows.none { it.amount < 0 } || flows.none { it.amount > 0 }) return null
        val t0 = flows.minOf { it.date }
        val years = flows.map { daysBetween(t0, it.date) / 365.0 }
        fun npv(rate: Double) = flows.indices.sumOf { flows[it].amount / (1 + rate).pow(years[it]) }

        var lo = -0.99
        var hi = 10.0
        var fLo = npv(lo)
        if (fLo * npv(hi) > 0) return null
        repeat(200) {
            val mid = (lo + hi) / 2
            val fMid = npv(mid)
            if (fMid * fLo > 0) {
                lo = mid
                fLo = fMid
            } else {
                hi = mid
            }
        }
        return (lo + hi) / 2
    }

    /** Compound annual growth from [start] to [end] over [years]; null when it can't be computed. */
    fun cagr(start: Double, end: Double, years: Double): Double? =
        if (start <= 0 || end < 0 || years <= 0) null else (end / start).pow(1 / years) - 1

    private fun pct(part: Double, whole: Double): Double = if (whole == 0.0) 0.0 else part / whole * 100

    // ---- Fixed deposit -----------------------------------------------------------------------

    data class FdResult(
        val tenureYears: Double,
        val simpleInterest: Boolean, // tenure under 6 months, or the user chose simple interest
        val ratePerPeriod: Double, // r / n (0 for simple interest)
        val periods: Double, // n * t (0 for simple interest)
        val growthFactor: Double, // (1 + r/n)^(n*t), or 1 + r*t for simple interest
        val maturityAmount: Double,
        val totalInterest: Double,
        val interestPerPayout: Double?, // payout FDs only
        val valueToday: Double,
        val interestToDate: Double,
        val daysToMaturity: Long // negative once matured
    )

    fun fixedDeposit(
        principal: Double,
        ratePct: Double,
        start: Long,
        maturity: Long,
        compounding: Compounding,
        payout: Frequency?,
        today: Long
    ): FdResult {
        val r = ratePct / 100
        val t = yearsBetween(start, maturity)
        val elapsed = yearsBetween(start, min(today, maturity))
        val daysToMaturity = daysBetween(today, maturity)

        if (payout != null) {
            // Interest is paid out as it falls due, so the deposit itself never grows.
            val total = principal * r * t
            return FdResult(
                tenureYears = t, simpleInterest = true, ratePerPeriod = 0.0, periods = 0.0,
                growthFactor = 1 + r * t, maturityAmount = principal, totalInterest = total,
                interestPerPayout = principal * r / payout.perYear, valueToday = principal,
                interestToDate = principal * r * elapsed, daysToMaturity = daysToMaturity
            )
        }

        // Indian banks pay simple interest on deposits shorter than 6 months.
        val simple = compounding == Compounding.SIMPLE || fullMonthsBetween(start, maturity) < 6
        if (simple) {
            val tSimple = if (compounding == Compounding.SIMPLE) t else daysBetween(start, maturity) / 365.0
            val elapsedSimple = if (compounding == Compounding.SIMPLE) elapsed else daysBetween(start, min(today, maturity)).coerceAtLeast(0) / 365.0
            val factor = 1 + r * tSimple
            val valueToday = principal * (1 + r * elapsedSimple)
            return FdResult(
                tenureYears = tSimple, simpleInterest = true, ratePerPeriod = 0.0, periods = 0.0,
                growthFactor = factor, maturityAmount = principal * factor, totalInterest = principal * (factor - 1),
                interestPerPayout = null, valueToday = valueToday, interestToDate = valueToday - principal,
                daysToMaturity = daysToMaturity
            )
        }

        val n = compounding.perYear
        val factor = (1 + r / n).pow(n * t)
        val valueToday = principal * (1 + r / n).pow(n * elapsed)
        return FdResult(
            tenureYears = t, simpleInterest = false, ratePerPeriod = r / n, periods = n * t,
            growthFactor = factor, maturityAmount = principal * factor, totalInterest = principal * (factor - 1),
            interestPerPayout = null, valueToday = valueToday, interestToDate = valueToday - principal,
            daysToMaturity = daysToMaturity
        )
    }

    // ---- Recurring deposit -------------------------------------------------------------------

    data class RdResult(
        val totalDeposit: Double,
        val maturityAmount: Double,
        val totalInterest: Double,
        val installmentsPaid: Int,
        val installmentsLeft: Int,
        val depositedSoFar: Double,
        val valueToday: Double,
        val nextInstallmentDate: Long?,
        val maturityDate: Long
    )

    /**
     * Banks compound RDs quarterly: instalment i (counted back from maturity) grows for i months,
     * so Maturity = sum over i = 1..N of R * (1 + r/4)^(4i/12). [installmentsPaid] defaults to the
     * instalments due by [today]; pass the investment-history count when months were missed.
     */
    fun recurringDeposit(
        installment: Double,
        ratePct: Double,
        tenureMonths: Int,
        start: Long,
        today: Long,
        installmentsPaid: Int? = null
    ): RdResult {
        val q = 1 + ratePct / 100 / 4
        fun grow(months: Int) = installment * q.pow(4.0 * months / 12)
        val maturity = (1..tenureMonths).sumOf { grow(it) }

        val monthsElapsed = min(fullMonthsBetween(start, today), tenureMonths)
        val due = if (startOfDay(today) < startOfDay(start)) 0 else min(monthsElapsed + 1, tenureMonths)
        val paid = (installmentsPaid ?: due).coerceIn(0, tenureMonths)
        // Instalment k (1-based) was paid k-1 months after the start.
        val valueToday = (1..paid).sumOf { k -> grow(max(monthsElapsed - (k - 1), 0)) }
        val maturityDate = addMonths(start, tenureMonths)

        return RdResult(
            totalDeposit = installment * tenureMonths,
            maturityAmount = maturity,
            totalInterest = maturity - installment * tenureMonths,
            installmentsPaid = paid,
            installmentsLeft = tenureMonths - paid,
            depositedSoFar = installment * paid,
            valueToday = valueToday,
            nextInstallmentDate = if (paid < tenureMonths) addMonths(start, paid) else null,
            maturityDate = maturityDate
        )
    }

    // ---- Stocks ------------------------------------------------------------------------------

    data class StockResult(
        val invested: Double,
        val currentValue: Double,
        val profit: Double,
        val profitPct: Double,
        val totalReturn: Double, // profit + dividends
        val totalReturnPct: Double,
        val daysHeld: Long,
        val isLongTerm: Boolean, // held more than 12 months (listed equity)
        val isPriceStale: Boolean // price older than 30 days
    )

    fun stock(
        quantity: Double,
        avgPrice: Double,
        currentPrice: Double,
        dividends: Double,
        buyDate: Long,
        priceDate: Long?,
        today: Long
    ): StockResult {
        val invested = quantity * avgPrice
        val current = quantity * currentPrice
        val profit = current - invested
        return StockResult(
            invested = invested,
            currentValue = current,
            profit = profit,
            profitPct = pct(profit, invested),
            totalReturn = profit + dividends,
            totalReturnPct = pct(profit + dividends, invested),
            daysHeld = daysBetween(buyDate, today).coerceAtLeast(0),
            isLongTerm = startOfDay(today) > addMonths(buyDate, 12),
            isPriceStale = priceDate != null && daysBetween(priceDate, today) > 30
        )
    }

    // ---- SIP / mutual funds ------------------------------------------------------------------

    data class MutualFundResult(
        val invested: Double,
        val currentValue: Double,
        val gain: Double,
        val gainPct: Double,
        val xirr: Double?,
        val installments: Int // INVEST entries
    )

    /** Invested comes from the investment history; XIRR uses its real dates, valued at [today]. */
    fun mutualFund(units: Double, nav: Double, entries: List<SavingsEntryEntity>, today: Long): MutualFundResult {
        val summary = summarizeEntries(entries)
        val current = units * nav
        val gain = current - summary.netInvested
        val flows = entries.map {
            when (EntryType.from(it.type)) {
                EntryType.INVEST -> CashFlow(it.date, -it.amount)
                EntryType.WITHDRAW, EntryType.INCOME -> CashFlow(it.date, it.amount)
            }
        } + CashFlow(today, current)
        return MutualFundResult(
            invested = summary.netInvested,
            currentValue = current,
            gain = gain,
            gainPct = pct(gain, summary.netInvested),
            xirr = xirr(flows),
            installments = entries.count { EntryType.from(it.type) == EntryType.INVEST }
        )
    }

    // ---- Gold and silver ---------------------------------------------------------------------

    data class MetalResult(
        val ratePerGramUsed: Double,
        val rateDerived: Boolean, // 22K/18K rate worked out from the 24K rate
        val value: Double,
        val gain: Double,
        val gainPct: Double
    )

    /**
     * Value = grams x rate for that purity. Jewellers quote 22K separately; when no 22K rate is
     * entered it is derived as 24K x 22/24. 18K is always derived.
     */
    fun gold(grams: Double, purity: GoldPurity, rate24PerGram: Double, rate22PerGram: Double?, purchaseAmount: Double): MetalResult {
        val (rate, derived) = when (purity) {
            GoldPurity.K24 -> rate24PerGram to false
            GoldPurity.K22 -> if (rate22PerGram != null && rate22PerGram > 0) rate22PerGram to false else rate24PerGram * purity.factor to true
            GoldPurity.K18 -> rate24PerGram * purity.factor to true
        }
        val value = grams * rate
        return MetalResult(rate, derived, value, value - purchaseAmount, pct(value - purchaseAmount, purchaseAmount))
    }

    /** Silver is quoted per kg: value = grams / 1000 x rate per kg x purity. */
    fun silver(grams: Double, purityPct: Double, ratePerKg: Double, purchaseAmount: Double): MetalResult {
        val ratePerGram = ratePerKg / 1000 * purityPct / 100
        val value = grams * ratePerGram
        return MetalResult(ratePerGram, true, value, value - purchaseAmount, pct(value - purchaseAmount, purchaseAmount))
    }

    data class SgbResult(
        val interestPerHalfYear: Double,
        val payoutsSoFar: Int,
        val interestReceived: Double,
        val maturityDate: Long
    )

    /** Sovereign Gold Bonds pay 2.5% a year on the issue price, half-yearly, and mature in 8 years. */
    fun sgbInterest(units: Double, issuePrice: Double, issueDate: Long, today: Long): SgbResult {
        val maturity = addMonths(issueDate, 96)
        val perHalfYear = units * issuePrice * 0.025 / 2
        val payouts = min(fullMonthsBetween(issueDate, today), 96) / 6
        return SgbResult(perHalfYear, payouts, perHalfYear * payouts, maturity)
    }

    // ---- Land & property ---------------------------------------------------------------------

    /** Every figure that needs a missing input (size, price or today's value) is null. */
    data class LandResult(
        val areaSqft: Double?,
        val totalCost: Double, // 0 when no price or costs were entered
        val gain: Double?,
        val gainPct: Double?,
        val yearsHeld: Double,
        val cagr: Double?,
        val pricePerSqftAtPurchase: Double?,
        val pricePerSqftNow: Double?,
        val netEquity: Double?, // current value - loan outstanding
        val rentalYieldPct: Double? // yearly rent / current value
    )

    fun land(d: SavingsDetails.Land, purchaseDate: Long, today: Long): LandResult {
        val sqft = d.area?.let { it * d.areaUnit.sqft }?.takeIf { it > 0 }
        val cost = (d.purchasePrice ?: 0.0) + d.stampDutyAndRegistration + d.brokerage + d.otherCosts
        val value = d.currentValue
        val years = yearsBetween(purchaseDate, today)
        val comparable = value != null && cost > 0
        return LandResult(
            areaSqft = sqft,
            totalCost = cost,
            gain = if (comparable) value!! - cost else null,
            gainPct = if (comparable) pct(value!! - cost, cost) else null,
            yearsHeld = years,
            cagr = if (comparable) cagr(cost, value!!, years) else null,
            pricePerSqftAtPurchase = if (sqft != null && d.purchasePrice != null) d.purchasePrice / sqft else null,
            pricePerSqftNow = if (sqft != null && value != null) value / sqft else null,
            netEquity = value?.let { it - d.loanOutstanding },
            rentalYieldPct = if (value != null && value > 0 && d.monthlyRent > 0) pct(d.monthlyRent * 12, value) else null
        )
    }

    // ---- Other schemes -----------------------------------------------------------------------

    data class SchemeResult(val yearsLeft: Double, val projectedMaturity: Double?)

    /**
     * PPF/EPF/NPS/SSY projection: today's balance compounds yearly and the yearly contribution is
     * added at the start of each year. Null projection without a maturity date or rate.
     */
    fun scheme(balance: Double, yearlyContribution: Double, ratePct: Double, maturity: Long?, today: Long): SchemeResult {
        if (maturity == null) return SchemeResult(0.0, null)
        val years = yearsBetween(today, maturity)
        if (ratePct <= 0) return SchemeResult(years, balance + yearlyContribution * years)
        val g = 1 + ratePct / 100
        val projected = balance * g.pow(years) + yearlyContribution * g * (g.pow(years) - 1) / (g - 1)
        return SchemeResult(years, projected)
    }

    data class InsuranceResult(
        val premiumsTotal: Int,
        val premiumsPaid: Int,
        val premiumsLeft: Int,
        val amountPaid: Double,
        val totalPayable: Double,
        val nextDueDate: Long?,
        val maturityDate: Long
    )

    fun insurance(premium: Double, frequency: Frequency, start: Long, termYears: Int, today: Long): InsuranceResult {
        val total = termYears * frequency.perYear
        val paid = if (startOfDay(today) < startOfDay(start)) 0 else min(fullMonthsBetween(start, today) / frequency.months + 1, total)
        return InsuranceResult(
            premiumsTotal = total,
            premiumsPaid = paid,
            premiumsLeft = total - paid,
            amountPaid = premium * paid,
            totalPayable = premium * total,
            nextDueDate = if (paid < total) addMonths(start, paid * frequency.months) else null,
            maturityDate = addMonths(start, termYears * 12)
        )
    }

    data class ChitResult(
        val amountPaid: Double,
        val monthsLeft: Int,
        val amountLeft: Double,
        val netCost: Double // paid - dividends received
    )

    fun chit(d: SavingsDetails.Chit): ChitResult {
        val paid = d.monthlyInstallment * d.monthsPaid
        val left = (d.totalMonths - d.monthsPaid).coerceAtLeast(0)
        return ChitResult(paid, left, d.monthlyInstallment * left, paid - d.dividendsReceived)
    }

    data class LoanResult(val interestDue: Double, val outstanding: Double)

    /** Simple interest on money lent, from [lentOn] to [today]. */
    fun loanGiven(d: SavingsDetails.LoanGiven, lentOn: Long, today: Long): LoanResult {
        val interest = d.amount * d.ratePct / 100 * daysBetween(lentOn, today).coerceAtLeast(0) / 365.0
        return LoanResult(interest, d.amount + interest - d.receivedBack)
    }
}
