package com.example.ui.savings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.SavingsEntryEntity
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.Compounding
import com.example.data.savings.EntryType
import com.example.data.savings.GoldForm
import com.example.data.savings.MarketRates
import com.example.data.savings.SavingsCalculator
import com.example.data.savings.SavingsDetails
import com.example.ui.components.formatMoney
import java.util.Locale

/** One line of a calculation: what is worked out, how (with the numbers filled in), and the result. */
data class CalcLine(val label: String, val value: String, val detail: String? = null, val emphasize: Boolean = false)

/** Shows each step of a record's calculation with the user's own numbers. */
@Composable
fun SavingsCalculationCard(item: SavingsItemEntity, details: SavingsDetails?, entries: List<SavingsEntryEntity>, rates: MarketRates, currencySymbol: String, today: Long) {
    val lines = calculationLines(item, details, entries, rates, currencySymbol, today)
    if (lines.isEmpty()) return
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.savings_calc_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            lines.forEachIndexed { i, line ->
                if (i > 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = MaterialTheme.colorScheme.outlineVariant)
                LabelValueRow(label = "${i + 1}. ${line.label}", value = line.value, detail = line.detail, emphasize = line.emphasize)
            }
            calculationNote(details, today)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun num(value: Double, decimals: Int) = String.format(Locale.getDefault(), "%.${decimals}f", value)

@Composable
private fun calculationNote(details: SavingsDetails?, today: Long): String? = when (details) {
    is SavingsDetails.Fd -> stringResource(R.string.savings_calc_note_tds)
    is SavingsDetails.Rd -> stringResource(R.string.savings_calc_note_rd)
    is SavingsDetails.Stock -> if (details.priceDate != null && SavingsCalculator.daysBetween(details.priceDate, today) > 30) {
        stringResource(R.string.savings_calc_note_stale_price, formatDay(details.priceDate))
    } else {
        null
    }
    is SavingsDetails.MutualFund -> stringResource(R.string.savings_calc_note_xirr)
    is SavingsDetails.Gold -> if (details.form == GoldForm.SGB) null else stringResource(R.string.savings_calc_note_making)
    is SavingsDetails.Land -> stringResource(R.string.savings_calc_note_land)
    is SavingsDetails.Scheme -> stringResource(R.string.savings_calc_note_scheme)
    is SavingsDetails.Chit -> if (details.prizeAmount > 0) stringResource(R.string.savings_calc_note_chit_prize) else null
    else -> null
}

@Composable
private fun calculationLines(item: SavingsItemEntity, details: SavingsDetails?, entries: List<SavingsEntryEntity>, rates: MarketRates, cs: String, today: Long): List<CalcLine> {
    val money = { v: Double -> formatMoney(cs, v) }
    val signed = { v: Double -> formatSignedMoney(cs, v) }
    val invested = SavingsCalculator.summarizeEntries(entries).netInvested
    return when (details) {
        is SavingsDetails.Fd -> {
            val maturity = item.maturityDate ?: return emptyList()
            val r = SavingsCalculator.fixedDeposit(details.principal, details.ratePct, item.startDate, maturity, details.compounding, details.payout, today)
            val elapsed = SavingsCalculator.yearsBetween(item.startDate, minOf(today, maturity))
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_tenure), stringResource(R.string.savings_calc_years, num(r.tenureYears, 2)), "${formatDay(item.startDate)} → ${formatDay(maturity)}"))
                when {
                    details.payout != null -> {
                        add(CalcLine(stringResource(R.string.savings_calc_interest_per_payout), money(r.interestPerPayout ?: 0.0), "${money(details.principal)} × ${formatPlain(details.ratePct)}% ÷ ${details.payout.perYear}"))
                        add(CalcLine(stringResource(R.string.savings_calc_total_interest_simple), money(r.totalInterest), "${money(details.principal)} × ${formatPlain(details.ratePct)}% × ${num(r.tenureYears, 2)}"))
                        add(CalcLine(stringResource(R.string.savings_calc_interest_so_far), money(r.interestToDate), stringResource(R.string.savings_calc_after_years, num(elapsed, 2))))
                        add(CalcLine(stringResource(R.string.savings_calc_maturity_principal), money(r.maturityAmount), emphasize = true))
                    }
                    r.simpleInterest -> {
                        add(CalcLine(stringResource(R.string.savings_calc_simple_interest), money(r.totalInterest), "${money(details.principal)} × ${formatPlain(details.ratePct)}% × ${num(r.tenureYears, 4)}"))
                        add(CalcLine(stringResource(R.string.savings_calc_maturity), money(r.maturityAmount), "${money(details.principal)} + ${money(r.totalInterest)}", emphasize = true))
                        add(CalcLine(stringResource(R.string.savings_calc_value_today), money(r.valueToday), stringResource(R.string.savings_calc_after_years, num(elapsed, 2))))
                    }
                    else -> {
                        val n = details.compounding.perYear
                        add(CalcLine(stringResource(R.string.savings_calc_rate_per_period), num(r.ratePerPeriod * 100, 4) + "%", "${formatPlain(details.ratePct)}% ÷ $n"))
                        add(CalcLine(stringResource(R.string.savings_calc_periods), num(r.periods, 2), "$n × ${num(r.tenureYears, 2)}"))
                        add(CalcLine(stringResource(R.string.savings_calc_growth), num(r.growthFactor, 6), "(1 + ${num(r.ratePerPeriod, 4)})^${num(r.periods, 2)}"))
                        add(CalcLine(stringResource(R.string.savings_calc_maturity), money(r.maturityAmount), "${money(details.principal)} × ${num(r.growthFactor, 6)}", emphasize = true))
                        add(CalcLine(stringResource(R.string.savings_calc_interest_at_maturity), money(r.totalInterest), "${money(r.maturityAmount)} − ${money(details.principal)}"))
                        add(CalcLine(stringResource(R.string.savings_calc_value_today), money(r.valueToday), stringResource(R.string.savings_calc_after_years, num(elapsed, 2))))
                        add(CalcLine(stringResource(R.string.savings_calc_interest_so_far), money(r.interestToDate)))
                    }
                }
                add(maturityLine(r.daysToMaturity, maturity))
            }
        }
        is SavingsDetails.Rd -> {
            val paidFromHistory = entries.count { EntryType.from(it.type) == EntryType.INVEST }.takeIf { entries.isNotEmpty() }
            val r = SavingsCalculator.recurringDeposit(details.installment, details.ratePct, details.tenureMonths, item.startDate, today, paidFromHistory)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_total_deposit), money(r.totalDeposit), "${money(details.installment)} × ${details.tenureMonths}"))
                add(
                    CalcLine(
                        stringResource(R.string.savings_calc_maturity), money(r.maturityAmount),
                        stringResource(R.string.savings_calc_rd_formula, money(details.installment), formatPlain(details.ratePct), details.tenureMonths), emphasize = true
                    )
                )
                add(CalcLine(stringResource(R.string.savings_calc_interest_at_maturity), money(r.totalInterest), "${money(r.maturityAmount)} − ${money(r.totalDeposit)}"))
                add(CalcLine(stringResource(R.string.savings_calc_installments_paid), stringResource(R.string.savings_calc_x_of_y, r.installmentsPaid, details.tenureMonths)))
                add(CalcLine(stringResource(R.string.savings_calc_deposited_so_far), money(r.depositedSoFar), "${money(details.installment)} × ${r.installmentsPaid}"))
                add(CalcLine(stringResource(R.string.savings_calc_value_today), money(r.valueToday)))
                r.nextInstallmentDate?.let { add(CalcLine(stringResource(R.string.savings_calc_next_installment), formatDay(it))) }
                add(maturityLine(SavingsCalculator.daysBetween(today, r.maturityDate), r.maturityDate))
            }
        }
        is SavingsDetails.Stock -> {
            val r = SavingsCalculator.stock(details.quantity, details.avgPrice, details.currentPrice, details.dividends, item.startDate, details.priceDate, today)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_invested_qty), money(r.invested), "${formatPlain(details.quantity)} × ${money(details.avgPrice)}"))
                add(
                    CalcLine(
                        stringResource(R.string.savings_calc_value_qty), money(r.currentValue),
                        "${formatPlain(details.quantity)} × ${money(details.currentPrice)}" + (details.priceDate?.let { " · " + formatDay(it) } ?: ""), emphasize = true
                    )
                )
                add(CalcLine(stringResource(R.string.savings_calc_profit), signed(r.profit), "${money(r.currentValue)} − ${money(r.invested)} · ${formatPct(r.profitPct)}"))
                if (details.dividends > 0) {
                    add(CalcLine(stringResource(R.string.savings_calc_dividends), money(details.dividends)))
                    add(CalcLine(stringResource(R.string.savings_calc_total_return), signed(r.totalReturn), formatPct(r.totalReturnPct)))
                }
                add(
                    CalcLine(
                        stringResource(R.string.savings_calc_held_for), pluralStringResource(R.plurals.savings_days, r.daysHeld.toInt(), r.daysHeld.toInt()),
                        stringResource(if (r.isLongTerm) R.string.savings_calc_long_term else R.string.savings_calc_short_term)
                    )
                )
            }
        }
        is SavingsDetails.MutualFund -> {
            val r = SavingsCalculator.mutualFund(details.units, details.nav, entries, today)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_invested_history), money(r.invested), pluralStringResource(R.plurals.savings_entries, r.installments, r.installments)))
                add(
                    CalcLine(
                        stringResource(R.string.savings_calc_value_units), money(r.currentValue),
                        "${formatPlain(details.units)} × ${money(details.nav)}" + (details.navDate?.let { " · " + formatDay(it) } ?: ""), emphasize = true
                    )
                )
                add(CalcLine(stringResource(R.string.savings_calc_gain), signed(r.gain), "${money(r.currentValue)} − ${money(r.invested)} · ${formatPct(r.gainPct)}"))
                add(CalcLine(stringResource(R.string.savings_calc_xirr), r.xirr?.let { formatPct(it * 100) } ?: "—"))
                if (details.fundType == "ELSS") {
                    entries.filter { EntryType.from(it.type) == EntryType.INVEST }.maxOfOrNull { it.date }?.let {
                        add(CalcLine(stringResource(R.string.savings_calc_elss_lock), formatDay(SavingsCalculator.addMonths(it, 36)), stringResource(R.string.savings_calc_elss_lock_detail)))
                    }
                }
            }
        }
        is SavingsDetails.Gold -> buildList {
            val grams = details.grams
            val paid = details.purchaseAmount
            details.quantity?.let { add(CalcLine(stringResource(R.string.savings_calc_quantity), pluralStringResource(R.plurals.savings_pieces, it, it))) }
            if (details.form == GoldForm.SGB) {
                val rate = rates.gold24PerGram
                if (grams != null && rate != null) {
                    add(CalcLine(stringResource(R.string.savings_calc_value_grams), money(grams * rate), "${formatPlain(grams)} × ${money(rate)} (24K)", emphasize = true))
                    if (paid != null) add(CalcLine(stringResource(R.string.savings_calc_gain), signed(grams * rate - paid), "${money(grams * rate)} − ${money(paid)}"))
                } else if (grams != null) {
                    add(missingRate(R.string.savings_calc_missing_gold))
                }
                val issue = details.sgbIssuePrice
                if (grams != null && issue != null) {
                    val sgb = SavingsCalculator.sgbInterest(grams, issue, item.startDate, today)
                    add(CalcLine(stringResource(R.string.savings_calc_sgb_interest), money(sgb.interestPerHalfYear), "${formatPlain(grams)} × ${money(issue)} × 2.5% ÷ 2"))
                    add(CalcLine(stringResource(R.string.savings_calc_sgb_received), money(sgb.interestReceived), stringResource(R.string.savings_calc_payouts, sgb.payoutsSoFar)))
                    add(CalcLine(stringResource(R.string.savings_calc_matures), formatDay(sgb.maturityDate)))
                }
            } else {
                val rate24 = rates.gold24PerGram
                when {
                    grams == null -> if (paid != null) add(purchaseLine(details, money))
                    rate24 == null -> {
                        add(CalcLine(stringResource(R.string.savings_calc_weight), "${formatPlain(grams)} g · ${details.purity.carats}K"))
                        add(missingRate(R.string.savings_calc_missing_gold))
                        if (paid != null) add(purchaseLine(details, money))
                    }
                    else -> {
                        val r = SavingsCalculator.gold(grams, details.purity, rate24, rates.gold22PerGram, paid ?: 0.0)
                        add(
                            CalcLine(
                                stringResource(R.string.savings_calc_rate_used, "${details.purity.carats}K"), money(r.ratePerGramUsed) + "/g",
                                if (r.rateDerived) "${money(rate24)} × ${details.purity.carats} ÷ 24" else stringResource(R.string.savings_calc_rate_entered)
                            )
                        )
                        add(CalcLine(stringResource(R.string.savings_calc_value_grams), money(r.value), "${formatPlain(grams)} g × ${money(r.ratePerGramUsed)}", emphasize = true))
                        if (paid != null) {
                            add(purchaseLine(details, money))
                            add(CalcLine(stringResource(R.string.savings_calc_gain), signed(r.gain), "${money(r.value)} − ${money(paid)} · ${formatPct(r.gainPct)}"))
                        }
                    }
                }
            }
        }
        is SavingsDetails.Silver -> buildList {
            val grams = details.grams
            val paid = details.purchaseAmount
            val rate = rates.silverPerKg
            details.quantity?.let { add(CalcLine(stringResource(R.string.savings_calc_quantity), pluralStringResource(R.plurals.savings_pieces, it, it))) }
            when {
                grams == null -> if (paid != null) add(CalcLine(stringResource(R.string.savings_calc_purchase), money(paid)))
                rate == null -> {
                    add(CalcLine(stringResource(R.string.savings_calc_weight), "${formatPlain(grams)} g · ${formatPlain(details.purityPct)}%"))
                    add(missingRate(R.string.savings_calc_missing_silver))
                    if (paid != null) add(CalcLine(stringResource(R.string.savings_calc_purchase), money(paid)))
                }
                else -> {
                    val r = SavingsCalculator.silver(grams, details.purityPct, rate, paid ?: 0.0)
                    add(CalcLine(stringResource(R.string.savings_calc_silver_rate), money(r.ratePerGramUsed) + "/g", "${money(rate)} ÷ 1000 × ${formatPlain(details.purityPct)}%"))
                    add(CalcLine(stringResource(R.string.savings_calc_value_grams), money(r.value), "${formatPlain(grams)} g × ${money(r.ratePerGramUsed)}", emphasize = true))
                    if (paid != null) {
                        add(CalcLine(stringResource(R.string.savings_calc_purchase), money(paid)))
                        add(CalcLine(stringResource(R.string.savings_calc_gain), signed(r.gain), "${money(r.value)} − ${money(paid)} · ${formatPct(r.gainPct)}"))
                    }
                }
            }
        }
        is SavingsDetails.Land -> {
            val r = SavingsCalculator.land(details, item.startDate, today)
            val value = details.currentValue
            buildList {
                val area = details.area
                if (area != null && r.areaSqft != null) {
                    add(CalcLine(stringResource(R.string.savings_calc_area), "${formatPlain(Math.round(r.areaSqft * 100) / 100.0)} sq ft", "${formatPlain(area)} ${stringResource(details.areaUnit.label())} × ${formatPlain(details.areaUnit.sqft)}"))
                }
                if (r.totalCost > 0) {
                    add(
                        CalcLine(
                            stringResource(R.string.savings_calc_total_cost), money(r.totalCost),
                            listOfNotNull(details.purchasePrice, details.stampDutyAndRegistration, details.brokerage, details.otherCosts).filter { it > 0 }.joinToString(" + ") { money(it) }
                        )
                    )
                }
                if (value != null) {
                    add(
                        CalcLine(
                            stringResource(R.string.savings_calc_current_estimate), money(value),
                            stringResource(valueSourceLabel(details.valueSource)) + (details.valueDate?.let { " · " + formatDay(it) } ?: ""), emphasize = true
                        )
                    )
                }
                if (r.gain != null) add(CalcLine(stringResource(R.string.savings_calc_gain), signed(r.gain), "${money(value!!)} − ${money(r.totalCost)} · ${formatPct(r.gainPct ?: 0.0)}"))
                add(CalcLine(stringResource(R.string.savings_calc_years_held), stringResource(R.string.savings_calc_years, num(r.yearsHeld, 2))))
                r.cagr?.let {
                    add(CalcLine(stringResource(R.string.savings_calc_cagr), formatPct(it * 100), "(${money(value!!)} ÷ ${money(r.totalCost)})^(1 ÷ ${num(r.yearsHeld, 2)}) − 1"))
                }
                if (r.pricePerSqftAtPurchase != null || r.pricePerSqftNow != null) {
                    add(
                        CalcLine(
                            stringResource(R.string.savings_calc_price_sqft),
                            "${r.pricePerSqftAtPurchase?.let { money(it) } ?: "—"} → ${r.pricePerSqftNow?.let { money(it) } ?: "—"}",
                            stringResource(R.string.savings_calc_price_sqft_detail)
                        )
                    )
                }
                if (details.loanOutstanding > 0 && r.netEquity != null) add(CalcLine(stringResource(R.string.savings_calc_net_equity), money(r.netEquity), "${money(value!!)} − ${money(details.loanOutstanding)}"))
                r.rentalYieldPct?.let { add(CalcLine(stringResource(R.string.savings_calc_rental_yield), formatPct(it), "${money(details.monthlyRent)} × 12 ÷ ${money(value!!)}")) }
            }
        }
        is SavingsDetails.Scheme -> {
            val r = SavingsCalculator.scheme(details.balance, details.yearlyContribution, details.ratePct, item.maturityDate, today)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_balance), money(details.balance), details.balanceDate?.let { formatDay(it) }, emphasize = true))
                if (item.maturityDate != null) add(CalcLine(stringResource(R.string.savings_calc_years_left), stringResource(R.string.savings_calc_years, num(r.yearsLeft, 2)), formatDay(item.maturityDate)))
                r.projectedMaturity?.let {
                    add(
                        CalcLine(
                            stringResource(R.string.savings_calc_projected), money(it),
                            stringResource(R.string.savings_calc_projected_detail, money(details.balance), formatPlain(details.ratePct), money(details.yearlyContribution))
                        )
                    )
                }
            }
        }
        is SavingsDetails.Insurance -> {
            val r = SavingsCalculator.insurance(details.premium, details.premiumFrequency, item.startDate, details.termYears, today)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_premiums_paid), stringResource(R.string.savings_calc_x_of_y, r.premiumsPaid, r.premiumsTotal)))
                add(CalcLine(stringResource(R.string.savings_calc_amount_paid), money(r.amountPaid), "${money(details.premium)} × ${r.premiumsPaid}", emphasize = true))
                add(CalcLine(stringResource(R.string.savings_calc_total_payable), money(r.totalPayable), "${money(details.premium)} × ${r.premiumsTotal}"))
                r.nextDueDate?.let { add(CalcLine(stringResource(R.string.savings_calc_next_premium), formatDay(it))) }
                if (details.sumAssured > 0) add(CalcLine(stringResource(R.string.savings_field_sum_assured), money(details.sumAssured)))
                if (details.bonusSoFar > 0) add(CalcLine(stringResource(R.string.savings_field_bonus), money(details.bonusSoFar)))
                add(CalcLine(stringResource(R.string.savings_calc_matures), formatDay(r.maturityDate)))
            }
        }
        is SavingsDetails.Chit -> {
            val r = SavingsCalculator.chit(details)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_amount_paid), money(r.amountPaid), "${money(details.monthlyInstallment)} × ${details.monthsPaid}", emphasize = true))
                add(CalcLine(stringResource(R.string.savings_calc_months_left), r.monthsLeft.toString(), money(r.amountLeft)))
                if (details.dividendsReceived > 0) add(CalcLine(stringResource(R.string.savings_field_chit_dividends), money(details.dividendsReceived)))
                add(CalcLine(stringResource(R.string.savings_calc_net_cost), money(r.netCost), "${money(r.amountPaid)} − ${money(details.dividendsReceived)}"))
                if (details.prizeAmount > 0) add(CalcLine(stringResource(R.string.savings_field_chit_prize), money(details.prizeAmount)))
            }
        }
        is SavingsDetails.Bond -> {
            val maturity = item.maturityDate ?: return emptyList()
            val r = SavingsCalculator.fixedDeposit(details.faceValue, details.couponPct, item.startDate, maturity, Compounding.SIMPLE, details.payout, today)
            buildList {
                add(CalcLine(stringResource(R.string.savings_calc_interest_per_payout), money(r.interestPerPayout ?: 0.0), "${money(details.faceValue)} × ${formatPlain(details.couponPct)}% ÷ ${details.payout.perYear}"))
                add(CalcLine(stringResource(R.string.savings_calc_total_interest_simple), money(r.totalInterest), "${money(details.faceValue)} × ${formatPlain(details.couponPct)}% × ${num(r.tenureYears, 2)}"))
                add(CalcLine(stringResource(R.string.savings_calc_interest_so_far), money(r.interestToDate)))
                add(CalcLine(stringResource(R.string.savings_calc_maturity_principal), money(details.faceValue), emphasize = true))
                add(maturityLine(r.daysToMaturity, maturity))
            }
        }
        is SavingsDetails.LoanGiven -> {
            val r = SavingsCalculator.loanGiven(details, item.startDate, today)
            val days = SavingsCalculator.daysBetween(item.startDate, today).coerceAtLeast(0)
            buildList {
                add(CalcLine(stringResource(R.string.savings_field_loan_amount), money(details.amount)))
                if (details.ratePct > 0) add(CalcLine(stringResource(R.string.savings_calc_interest_due), money(r.interestDue), "${money(details.amount)} × ${formatPlain(details.ratePct)}% × $days ÷ 365"))
                if (details.receivedBack > 0) add(CalcLine(stringResource(R.string.savings_field_received_back), money(details.receivedBack)))
                add(CalcLine(stringResource(R.string.savings_calc_outstanding), money(r.outstanding), "${money(details.amount)} + ${money(r.interestDue)} − ${money(details.receivedBack)}", emphasize = true))
            }
        }
        is SavingsDetails.Generic -> listOf(
            CalcLine(stringResource(R.string.savings_calc_invested_history), money(invested)),
            CalcLine(stringResource(R.string.savings_field_value_now), money(details.currentValue), emphasize = true),
            CalcLine(stringResource(R.string.savings_calc_gain), signed(details.currentValue - invested), "${money(details.currentValue)} − ${money(invested)}")
        )
        null -> emptyList()
    }
}

@Composable
private fun missingRate(detail: Int) = CalcLine(stringResource(R.string.savings_calc_no_rate), "—", stringResource(detail))

@Composable
private fun purchaseLine(details: SavingsDetails.Gold, money: (Double) -> String): CalcLine {
    val parts = listOfNotNull(
        details.makingCharges.takeIf { it > 0 }?.let { stringResource(R.string.savings_calc_making, money(it)) },
        details.gst.takeIf { it > 0 }?.let { stringResource(R.string.savings_calc_gst, money(it)) }
    )
    return CalcLine(stringResource(R.string.savings_calc_purchase), money(details.purchaseAmount ?: 0.0), parts.joinToString(", ").ifBlank { null })
}

@Composable
private fun maturityLine(daysToMaturity: Long, maturity: Long): CalcLine =
    if (daysToMaturity >= 0) {
        CalcLine(stringResource(R.string.savings_calc_days_to_maturity), pluralStringResource(R.plurals.savings_days, daysToMaturity.toInt(), daysToMaturity.toInt()), formatDay(maturity))
    } else {
        CalcLine(stringResource(R.string.savings_calc_matured), formatDay(maturity))
    }
