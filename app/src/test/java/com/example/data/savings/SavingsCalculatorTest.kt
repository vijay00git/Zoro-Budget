package com.example.data.savings

import com.example.data.local.entity.SavingsEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** The worked examples in docs/SAVINGS_PLAN.md, plus the edge cases around them. */
class SavingsCalculatorTest {

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, day)
    }.timeInMillis

    private val today = date(2026, 10, 20)

    @Test
    fun fdCompoundsQuarterly() {
        val fd = SavingsCalculator.fixedDeposit(
            100_000.0, 7.0, date(2025, 10, 20), date(2027, 10, 20), Compounding.QUARTERLY, null, today
        )
        assertEquals(2.0, fd.tenureYears, 1e-9)
        assertEquals(0.0175, fd.ratePerPeriod, 1e-9)
        assertEquals(8.0, fd.periods, 1e-9)
        assertEquals(1.148882, fd.growthFactor, 1e-6)
        assertEquals(114_888.18, fd.maturityAmount, 0.01)
        assertEquals(14_888.18, fd.totalInterest, 0.01)
        assertEquals(107_185.90, fd.valueToday, 0.01) // exactly 1 year in
        assertFalse(fd.simpleInterest)
    }

    @Test
    fun fdShorterThanSixMonthsEarnsSimpleInterest() {
        val fd = SavingsCalculator.fixedDeposit(
            100_000.0, 6.0, date(2026, 1, 1), date(2026, 4, 1), Compounding.QUARTERLY, null, today
        )
        assertTrue(fd.simpleInterest)
        assertEquals(100_000 * 0.06 * 90 / 365.0, fd.totalInterest, 0.01)
        assertEquals(fd.maturityAmount, fd.valueToday, 0.01) // already matured
        assertTrue(fd.daysToMaturity < 0)
    }

    @Test
    fun payoutFdKeepsPrincipalAndPaysInterestOut() {
        val fd = SavingsCalculator.fixedDeposit(
            100_000.0, 7.2, date(2025, 10, 20), date(2028, 10, 20), Compounding.QUARTERLY, Frequency.MONTHLY, today
        )
        assertEquals(100_000.0, fd.maturityAmount, 0.001)
        assertEquals(600.0, fd.interestPerPayout!!, 0.001)
        assertEquals(21_600.0, fd.totalInterest, 0.01)
        assertEquals(7_200.0, fd.interestToDate, 0.01)
    }

    @Test
    fun rdCompoundsEachInstalmentQuarterly() {
        val rd = SavingsCalculator.recurringDeposit(5_000.0, 6.5, 12, date(2025, 10, 20), today)
        assertEquals(60_000.0, rd.totalDeposit, 0.001)
        assertEquals(62_143.23, rd.maturityAmount, 0.01)
        assertEquals(2_143.23, rd.totalInterest, 0.01)
        assertEquals(12, rd.installmentsPaid)
        // On the maturity date every instalment has grown its full term.
        assertEquals(rd.maturityAmount, rd.valueToday, 0.01)
        assertNull(rd.nextInstallmentDate)
    }

    @Test
    fun rdHalfwayCountsPaidInstalmentsAndMissedMonths() {
        val rd = SavingsCalculator.recurringDeposit(5_000.0, 6.5, 12, date(2026, 5, 5), today)
        assertEquals(6, rd.installmentsPaid) // May..Oct
        assertEquals(30_000.0, rd.depositedSoFar, 0.001)
        assertEquals(date(2026, 11, 5), rd.nextInstallmentDate)

        val missedOne = SavingsCalculator.recurringDeposit(5_000.0, 6.5, 12, date(2026, 5, 5), today, installmentsPaid = 5)
        assertEquals(25_000.0, missedOne.depositedSoFar, 0.001)
    }

    @Test
    fun stockProfitAndHoldingPeriod() {
        val s = SavingsCalculator.stock(50.0, 1_200.0, 1_450.0, 300.0, date(2025, 6, 1), date(2026, 8, 12), today)
        assertEquals(60_000.0, s.invested, 0.001)
        assertEquals(72_500.0, s.currentValue, 0.001)
        assertEquals(12_500.0, s.profit, 0.001)
        assertEquals(20.8333, s.profitPct, 0.0001)
        assertEquals(12_800.0, s.totalReturn, 0.001)
        assertTrue(s.isLongTerm)
        assertTrue(s.isPriceStale) // price from 12 Aug is over 30 days old
    }

    @Test
    fun sipScheduleInvestedAndXirr() {
        val dates = SavingsCalculator.monthlySchedule(date(2024, 1, 10), 10, today)
        assertEquals(34, dates.size)
        assertEquals(date(2026, 10, 10), dates.last())

        val entries = dates.map { SavingsEntryEntity(itemId = 1, date = it, amount = 5_000.0) }
        val mf = SavingsCalculator.mutualFund(2_150.432, 92.15, entries, today)
        assertEquals(170_000.0, mf.invested, 0.001)
        assertEquals(198_162.31, mf.currentValue, 0.01)
        assertEquals(28_162.31, mf.gain, 0.01)
        assertEquals(16.57, mf.gainPct, 0.01)
        assertEquals(0.1125, mf.xirr!!, 0.0001)
    }

    @Test
    fun scheduleClampsToShortMonths() {
        val dates = SavingsCalculator.monthlySchedule(date(2026, 1, 31), 31, date(2026, 4, 30))
        assertEquals(listOf(date(2026, 1, 31), date(2026, 2, 28), date(2026, 3, 31), date(2026, 4, 30)), dates)
    }

    @Test
    fun goldUsesThe22kRateOrDerivesIt() {
        val derived = SavingsCalculator.gold(20.0, GoldPurity.K22, 10_000.0, null, 150_000.0)
        assertTrue(derived.rateDerived)
        assertEquals(183_333.33, derived.value, 0.01)
        assertEquals(33_333.33, derived.gain, 0.01)

        val quoted = SavingsCalculator.gold(20.0, GoldPurity.K22, 10_000.0, 9_200.0, 150_000.0)
        assertFalse(quoted.rateDerived)
        assertEquals(184_000.0, quoted.value, 0.001)
    }

    @Test
    fun silverIsQuotedPerKg() {
        val s = SavingsCalculator.silver(250.0, 99.9, 120_000.0, 25_000.0)
        assertEquals(29_970.0, s.value, 0.001)
        assertEquals(4_970.0, s.gain, 0.001)
    }

    @Test
    fun sgbPaysHalfYearlyInterest() {
        val sgb = SavingsCalculator.sgbInterest(10.0, 5_000.0, date(2023, 3, 15), today)
        assertEquals(625.0, sgb.interestPerHalfYear, 0.001)
        assertEquals(7, sgb.payoutsSoFar) // 43 full months
        assertEquals(date(2031, 3, 15), sgb.maturityDate)
    }

    @Test
    fun landCostGainCagrAndArea() {
        val plot = SavingsDetails.Land(
            area = 5.0, areaUnit = AreaUnit.CENT, purchasePrice = 1_200_000.0,
            stampDutyAndRegistration = 108_000.0, otherCosts = 20_000.0, currentValue = 2_000_000.0
        )
        val r = SavingsCalculator.land(plot, date(2019, 6, 15), today)
        assertEquals(2_178.0, r.areaSqft!!, 0.001)
        assertEquals(1_328_000.0, r.totalCost, 0.001)
        assertEquals(672_000.0, r.gain!!, 0.001)
        assertEquals(550.96, r.pricePerSqftAtPurchase!!, 0.01)
        assertEquals(7.35, r.yearsHeld, 0.01)
        assertEquals(0.0573, r.cagr!!, 0.0001)
    }

    @Test
    fun landWithOnlyASizeRecordedStillWorks() {
        val r = SavingsCalculator.land(SavingsDetails.Land(area = 2.0, areaUnit = AreaUnit.GROUND), date(2019, 6, 15), today)
        assertEquals(4_800.0, r.areaSqft!!, 0.001)
        assertEquals(0.0, r.totalCost, 0.001)
        assertNull(r.gain)
        assertNull(r.cagr)
        assertNull(r.pricePerSqftNow)
    }

    @Test
    fun entrySummaryGroupsByFinancialYear() {
        val entries = listOf(
            SavingsEntryEntity(itemId = 1, date = date(2025, 3, 31), amount = 1_000.0),
            SavingsEntryEntity(itemId = 1, date = date(2025, 4, 1), amount = 2_000.0),
            SavingsEntryEntity(itemId = 1, date = date(2026, 1, 15), amount = 3_000.0),
            SavingsEntryEntity(itemId = 1, date = date(2026, 2, 1), type = "WITHDRAW", amount = 500.0),
            SavingsEntryEntity(itemId = 1, date = date(2026, 3, 1), type = "INCOME", amount = 120.0)
        )
        val s = SavingsCalculator.summarizeEntries(entries)
        assertEquals(6_000.0, s.totalInvested, 0.001)
        assertEquals(5_500.0, s.netInvested, 0.001)
        assertEquals(120.0, s.totalIncome, 0.001)
        assertEquals(mapOf(2024 to 1_000.0, 2025 to 5_000.0), s.investedByFinancialYear)
    }

    @Test
    fun insuranceChitSchemeAndLoan() {
        val policy = SavingsCalculator.insurance(25_000.0, Frequency.YEARLY, date(2020, 3, 1), 15, today)
        assertEquals(7, policy.premiumsPaid) // 2020..2026
        assertEquals(8, policy.premiumsLeft)
        assertEquals(375_000.0, policy.totalPayable, 0.001)
        assertEquals(date(2027, 3, 1), policy.nextDueDate)

        val chit = SavingsCalculator.chit(SavingsDetails.Chit(100_000.0, 5_000.0, 20, 8, dividendsReceived = 2_400.0))
        assertEquals(40_000.0, chit.amountPaid, 0.001)
        assertEquals(12, chit.monthsLeft)
        assertEquals(37_600.0, chit.netCost, 0.001)

        val ppf = SavingsCalculator.scheme(100_000.0, 0.0, 7.1, date(2028, 10, 20), today)
        assertEquals(100_000 * 1.071 * 1.071, ppf.projectedMaturity!!, 0.01)

        val loan = SavingsCalculator.loanGiven(SavingsDetails.LoanGiven("Ravi", 50_000.0, 12.0, 10_000.0), date(2025, 10, 20), today)
        assertEquals(6_000.0, loan.interestDue, 0.01)
        assertEquals(46_000.0, loan.outstanding, 0.01)
    }

    @Test
    fun xirrNeedsMoneyInAndOut() {
        assertNull(SavingsCalculator.xirr(listOf(SavingsCalculator.CashFlow(today, -100.0))))
        assertNotNull(SavingsCalculator.xirr(listOf(SavingsCalculator.CashFlow(date(2025, 10, 20), -100.0), SavingsCalculator.CashFlow(today, 110.0))))
    }
}
