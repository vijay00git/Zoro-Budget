package com.example.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.entity.SavingsItemEntity
import com.example.data.savings.AreaUnit
import com.example.data.savings.Compounding
import com.example.data.savings.Frequency
import com.example.data.savings.GoldForm
import com.example.data.savings.GoldPurity
import com.example.data.savings.OtherKind
import com.example.data.savings.PropertyType
import com.example.data.savings.SavingsCalculator
import com.example.data.savings.SavingsDetails
import com.example.data.savings.SavingsSection
import com.example.data.savings.SavingsStatus
import com.example.data.savings.SilverForm
import java.util.Calendar

/**
 * Full-screen form to add a savings record or edit one. The fields every section shares come
 * first, then the section's own inputs. [existing] is null when adding.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsFormDialog(
    section: SavingsSection,
    existing: SavingsItemEntity?,
    holders: List<String>,
    defaultHolder: String?,
    onDismiss: () -> Unit,
    onSave: (SavingsItemEntity, SavingsDetails) -> Unit
) {
    val today = remember { SavingsCalculator.startOfDay(System.currentTimeMillis()) }
    val old = remember(existing) { existing?.let { SavingsDetails.fromJson(it.detailsJson) } }

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var holder by remember { mutableStateOf(existing?.holder ?: defaultHolder ?: holders.firstOrNull() ?: "") }
    var institution by remember { mutableStateOf(existing?.institution ?: "") }
    var refNo by remember { mutableStateOf(existing?.refNo ?: "") }
    var nominee by remember { mutableStateOf(existing?.nominee ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate ?: today) }
    var maturityDate by remember { mutableStateOf(existing?.maturityDate) }
    var status by remember { mutableStateOf(SavingsStatus.from(existing?.status)) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    // FD
    val oldFd = old as? SavingsDetails.Fd
    var principal by remember { mutableStateOf(oldFd?.principal?.let { formatPlain(it) } ?: "") }
    var compounding by remember { mutableStateOf(oldFd?.compounding ?: Compounding.QUARTERLY) }
    var payout by remember { mutableStateOf(oldFd?.payout) }
    var autoRenew by remember { mutableStateOf(oldFd?.autoRenew ?: false) }

    // RD
    val oldRd = old as? SavingsDetails.Rd
    var installment by remember { mutableStateOf(oldRd?.installment?.let { formatPlain(it) } ?: "") }
    var tenureMonths by remember { mutableStateOf(oldRd?.tenureMonths?.toString() ?: "12") }
    var installmentDay by remember { mutableStateOf(oldRd?.installmentDay?.toString() ?: "") }

    // Interest, coupon or lending rate, shared by the sections that have one
    var rate by remember {
        mutableStateOf(
            when (old) {
                is SavingsDetails.Fd -> old.ratePct
                is SavingsDetails.Rd -> old.ratePct
                is SavingsDetails.Scheme -> old.ratePct
                is SavingsDetails.Bond -> old.couponPct
                is SavingsDetails.LoanGiven -> old.ratePct
                else -> null
            }?.let { formatPlain(it) } ?: ""
        )
    }

    // Gold and silver
    val oldGold = old as? SavingsDetails.Gold
    val oldSilver = old as? SavingsDetails.Silver
    var goldForm by remember { mutableStateOf(oldGold?.form ?: GoldForm.JEWELLERY) }
    var goldPurity by remember { mutableStateOf(oldGold?.purity ?: GoldPurity.K22) }
    var silverForm by remember { mutableStateOf(oldSilver?.form ?: SilverForm.ARTICLE) }
    var silverPurity by remember { mutableStateOf(formatPlain(oldSilver?.purityPct ?: 99.9)) }
    var grams by remember { mutableStateOf((oldGold?.grams ?: oldSilver?.grams)?.let { formatPlain(it) } ?: "") }
    var purchaseAmount by remember { mutableStateOf((oldGold?.purchaseAmount ?: oldSilver?.purchaseAmount)?.let { formatPlain(it) } ?: "") }
    var makingCharges by remember { mutableStateOf(oldGold?.makingCharges?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var gst by remember { mutableStateOf(oldGold?.gst?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var billNo by remember { mutableStateOf(oldGold?.billNo ?: "") }
    var sgbIssuePrice by remember { mutableStateOf(oldGold?.sgbIssuePrice?.let { formatPlain(it) } ?: "") }
    var pieces by remember { mutableStateOf(((old as? SavingsDetails.Gold)?.quantity ?: (old as? SavingsDetails.Silver)?.quantity)?.toString() ?: "") }
    val isSgb = section == SavingsSection.GOLD && goldForm == GoldForm.SGB

    // Stocks
    val oldStock = old as? SavingsDetails.Stock
    var symbol by remember { mutableStateOf(oldStock?.symbol ?: "") }
    var exchange by remember { mutableStateOf(oldStock?.exchange ?: "NSE") }
    var quantity by remember { mutableStateOf(oldStock?.quantity?.let { formatPlain(it) } ?: "") }
    var avgPrice by remember { mutableStateOf(oldStock?.avgPrice?.let { formatPlain(it) } ?: "") }
    var currentPrice by remember { mutableStateOf(oldStock?.currentPrice?.let { formatPlain(it) } ?: "") }
    var priceDate by remember { mutableStateOf(oldStock?.priceDate ?: today) }
    var dividends by remember { mutableStateOf(oldStock?.dividends?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var sector by remember { mutableStateOf(oldStock?.sector ?: "") }

    // SIP / mutual funds
    val oldMf = old as? SavingsDetails.MutualFund
    var mfPlan by remember { mutableStateOf(oldMf?.plan ?: "DIRECT") }
    var mfType by remember { mutableStateOf(oldMf?.fundType ?: "EQUITY") }
    var sipAmount by remember { mutableStateOf(oldMf?.sipAmount?.let { formatPlain(it) } ?: "") }
    var sipDay by remember { mutableStateOf(oldMf?.sipDay?.toString() ?: "") }
    var sipStopped by remember { mutableStateOf(oldMf?.sipStoppedDate) }
    var lumpSum by remember { mutableStateOf(oldMf?.lumpSum?.let { formatPlain(it) } ?: "") }
    var units by remember { mutableStateOf(oldMf?.units?.let { formatPlain(it) } ?: "") }
    var nav by remember { mutableStateOf(oldMf?.nav?.let { formatPlain(it) } ?: "") }
    var navDate by remember { mutableStateOf(oldMf?.navDate ?: today) }

    // Land & property
    val oldLand = old as? SavingsDetails.Land
    var propertyType by remember { mutableStateOf(oldLand?.propertyType ?: PropertyType.PLOT) }
    var address by remember { mutableStateOf(oldLand?.address ?: "") }
    var surveyNo by remember { mutableStateOf(oldLand?.surveyNo ?: "") }
    var pattaNo by remember { mutableStateOf(oldLand?.pattaNo ?: "") }
    var area by remember { mutableStateOf(oldLand?.area?.let { formatPlain(it) } ?: "") }
    var areaUnit by remember { mutableStateOf(oldLand?.areaUnit ?: AreaUnit.SQFT) }
    var landPrice by remember { mutableStateOf(oldLand?.purchasePrice?.let { formatPlain(it) } ?: "") }
    var stampDuty by remember { mutableStateOf(oldLand?.stampDutyAndRegistration?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var brokerage by remember { mutableStateOf(oldLand?.brokerage?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var otherCosts by remember { mutableStateOf(oldLand?.otherCosts?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var landValue by remember { mutableStateOf(oldLand?.currentValue?.let { formatPlain(it) } ?: "") }
    var valueDate by remember { mutableStateOf(oldLand?.valueDate ?: today) }
    var valueSource by remember { mutableStateOf(oldLand?.valueSource ?: "OWN_ESTIMATE") }
    var monthlyRent by remember { mutableStateOf(oldLand?.monthlyRent?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var loanOutstanding by remember { mutableStateOf(oldLand?.loanOutstanding?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }

    // Other schemes
    var otherKind by remember { mutableStateOf(OtherKind.entries.firstOrNull { it.name == existing?.subType } ?: OtherKind.PPF) }
    val oldScheme = old as? SavingsDetails.Scheme
    val oldInsurance = old as? SavingsDetails.Insurance
    val oldChit = old as? SavingsDetails.Chit
    val oldBond = old as? SavingsDetails.Bond
    val oldLoan = old as? SavingsDetails.LoanGiven
    val oldGeneric = old as? SavingsDetails.Generic
    var balance by remember { mutableStateOf(oldScheme?.balance?.let { formatPlain(it) } ?: "") }
    var balanceDate by remember { mutableStateOf(oldScheme?.balanceDate ?: today) }
    var yearlyContribution by remember { mutableStateOf(oldScheme?.yearlyContribution?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var premium by remember { mutableStateOf(oldInsurance?.premium?.let { formatPlain(it) } ?: "") }
    var premiumFrequency by remember { mutableStateOf(oldInsurance?.premiumFrequency ?: Frequency.YEARLY) }
    var termYears by remember { mutableStateOf(oldInsurance?.termYears?.toString() ?: "") }
    var sumAssured by remember { mutableStateOf(oldInsurance?.sumAssured?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var bonus by remember { mutableStateOf(oldInsurance?.bonusSoFar?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var chitValue by remember { mutableStateOf(oldChit?.chitValue?.let { formatPlain(it) } ?: "") }
    var chitInstallment by remember { mutableStateOf(oldChit?.monthlyInstallment?.let { formatPlain(it) } ?: "") }
    var chitMonths by remember { mutableStateOf(oldChit?.totalMonths?.toString() ?: "") }
    var chitPaid by remember { mutableStateOf(oldChit?.monthsPaid?.toString() ?: "") }
    var chitPrize by remember { mutableStateOf(oldChit?.prizeAmount?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var chitDividends by remember { mutableStateOf(oldChit?.dividendsReceived?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var faceValue by remember { mutableStateOf(oldBond?.faceValue?.let { formatPlain(it) } ?: "") }
    var bondPayout by remember { mutableStateOf(oldBond?.payout ?: Frequency.YEARLY) }
    var person by remember { mutableStateOf(oldLoan?.person ?: "") }
    var loanAmount by remember { mutableStateOf(oldLoan?.amount?.let { formatPlain(it) } ?: "") }
    var receivedBack by remember { mutableStateOf(oldLoan?.receivedBack?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var genericInvested by remember { mutableStateOf(oldGeneric?.invested?.takeIf { it > 0 }?.let { formatPlain(it) } ?: "") }
    var genericValue by remember { mutableStateOf(oldGeneric?.currentValue?.let { formatPlain(it) } ?: "") }
    val isSchemeKind = otherKind in listOf(OtherKind.PPF, OtherKind.EPF, OtherKind.NPS, OtherKind.SUKANYA)

    val rateValue = parseAmount(rate)
    val details: SavingsDetails? = when (section) {
        SavingsSection.FD -> {
            val p = parseAmount(principal)
            if (p != null && p > 0 && rateValue != null && rateValue >= 0) SavingsDetails.Fd(p, rateValue, compounding, payout, autoRenew) else null
        }
        SavingsSection.RD -> {
            val r = parseAmount(installment)
            val months = tenureMonths.toIntOrNull()
            val day = installmentDay.toIntOrNull() ?: Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.DAY_OF_MONTH)
            if (r != null && r > 0 && rateValue != null && rateValue >= 0 && months != null && months > 0 && day in 1..31) {
                SavingsDetails.Rd(r, rateValue, months, day)
            } else {
                null
            }
        }
        // Weight, price and pieces are all optional; only a value typed in but unreadable blocks saving.
        SavingsSection.GOLD -> if (listOf(grams, purchaseAmount, makingCharges, gst, sgbIssuePrice).any { unreadable(it) } || unreadableCount(pieces)) {
            null
        } else {
            SavingsDetails.Gold(
                form = goldForm, grams = positive(grams), purity = if (isSgb) GoldPurity.K24 else goldPurity, purchaseAmount = positive(purchaseAmount),
                makingCharges = parseAmount(makingCharges) ?: 0.0, gst = parseAmount(gst) ?: 0.0,
                billNo = billNo.trim().ifBlank { null }, sgbIssuePrice = if (isSgb) positive(sgbIssuePrice) else null,
                quantity = pieces.toIntOrNull()?.takeIf { it > 0 }
            )
        }
        SavingsSection.STOCK -> {
            val q = parseAmount(quantity)
            val avg = parseAmount(avgPrice)
            val cur = parseAmount(currentPrice)
            if (q != null && q > 0 && avg != null && avg > 0 && cur != null && cur >= 0) {
                SavingsDetails.Stock(
                    symbol.trim().uppercase().ifBlank { null }, exchange, q, avg, cur, priceDate,
                    parseAmount(dividends) ?: 0.0, sector.trim().ifBlank { null }
                )
            } else {
                null
            }
        }
        SavingsSection.MUTUAL_FUND -> {
            val u = parseAmount(units)
            val n = parseAmount(nav)
            val sip = parseAmount(sipAmount)?.takeIf { it > 0 }
            val day = sipDay.toIntOrNull() ?: Calendar.getInstance().apply { timeInMillis = startDate }.get(Calendar.DAY_OF_MONTH)
            val lump = parseAmount(lumpSum)?.takeIf { it > 0 }
            if (u != null && u >= 0 && n != null && n >= 0 && (sip != null || lump != null) && day in 1..31) {
                SavingsDetails.MutualFund(
                    amc = null, plan = mfPlan, fundType = mfType, sipAmount = sip, sipDay = if (sip != null) day else null,
                    sipStoppedDate = if (sip != null) sipStopped else null, lumpSum = lump, units = u, nav = n, navDate = navDate
                )
            } else {
                null
            }
        }
        // Size, price and value are all optional for land.
        SavingsSection.LAND -> if (listOf(area, landPrice, landValue, stampDuty, brokerage, otherCosts, monthlyRent, loanOutstanding).any { unreadable(it) }) {
            null
        } else {
            SavingsDetails.Land(
                propertyType, address.trim().ifBlank { null }, surveyNo.trim().ifBlank { null }, pattaNo.trim().ifBlank { null },
                positive(area), areaUnit, positive(landPrice), parseAmount(stampDuty) ?: 0.0, parseAmount(brokerage) ?: 0.0, parseAmount(otherCosts) ?: 0.0,
                parseAmount(landValue)?.takeIf { it >= 0 }, valueDate, valueSource, parseAmount(monthlyRent) ?: 0.0, parseAmount(loanOutstanding) ?: 0.0
            )
        }
        SavingsSection.OTHER -> when {
            isSchemeKind -> parseAmount(balance)?.takeIf { it >= 0 }?.let {
                SavingsDetails.Scheme(it, balanceDate, parseAmount(yearlyContribution) ?: 0.0, rateValue ?: 0.0)
            }
            otherKind == OtherKind.INSURANCE -> {
                val pr = parseAmount(premium)
                val term = termYears.toIntOrNull()
                if (pr != null && pr > 0 && term != null && term > 0) {
                    SavingsDetails.Insurance(pr, premiumFrequency, term, parseAmount(sumAssured) ?: 0.0, parseAmount(bonus) ?: 0.0)
                } else {
                    null
                }
            }
            otherKind == OtherKind.CHIT -> {
                val v = parseAmount(chitValue)
                val inst = parseAmount(chitInstallment)
                val months = chitMonths.toIntOrNull()
                val paid = chitPaid.toIntOrNull() ?: 0
                if (v != null && v > 0 && inst != null && inst > 0 && months != null && months > 0 && paid in 0..months) {
                    SavingsDetails.Chit(v, inst, months, paid, parseAmount(chitPrize) ?: 0.0, parseAmount(chitDividends) ?: 0.0)
                } else {
                    null
                }
            }
            otherKind == OtherKind.BOND -> {
                val fv = parseAmount(faceValue)
                if (fv != null && fv > 0 && rateValue != null && rateValue >= 0) SavingsDetails.Bond(fv, rateValue, bondPayout) else null
            }
            otherKind == OtherKind.LOAN_GIVEN -> {
                val amt = parseAmount(loanAmount)
                if (person.isNotBlank() && amt != null && amt > 0) {
                    SavingsDetails.LoanGiven(person.trim(), amt, rateValue ?: 0.0, parseAmount(receivedBack) ?: 0.0)
                } else {
                    null
                }
            }
            else -> parseAmount(genericValue)?.takeIf { it >= 0 }?.let { SavingsDetails.Generic(parseAmount(genericInvested) ?: 0.0, it) }
        }
        SavingsSection.SILVER -> {
            val purity = parseAmount(silverPurity) ?: 99.9
            if (listOf(grams, purchaseAmount, silverPurity).any { unreadable(it) } || unreadableCount(pieces) || purity <= 0 || purity > 100) {
                null
            } else {
                SavingsDetails.Silver(silverForm, positive(grams), purity, positive(purchaseAmount), pieces.toIntOrNull()?.takeIf { it > 0 })
            }
        }
        else -> null
    }
    val effectiveMaturity = when (section) {
        SavingsSection.RD -> (details as? SavingsDetails.Rd)?.let { SavingsCalculator.addMonths(startDate, it.tenureMonths) }
        SavingsSection.GOLD -> if (isSgb) SavingsCalculator.addMonths(startDate, 96) else null
        SavingsSection.OTHER -> when (otherKind) {
            OtherKind.INSURANCE -> termYears.toIntOrNull()?.let { SavingsCalculator.addMonths(startDate, it * 12) }
            OtherKind.CHIT -> chitMonths.toIntOrNull()?.let { SavingsCalculator.addMonths(startDate, it) }
            OtherKind.CRYPTO, OtherKind.OTHER -> null
            else -> maturityDate
        }
        SavingsSection.FD -> maturityDate
        else -> null
    }
    val needsMaturity = section == SavingsSection.FD || (section == SavingsSection.OTHER && otherKind == OtherKind.BOND)
    val maturityOk = !needsMaturity || (maturityDate != null && maturityDate!! > startDate)
    val canSave = name.isNotBlank() && holder.isNotBlank() && details != null && maturityOk

    // Back or a tap outside must not silently throw away what was typed.
    val fields = listOf(
        name, holder, institution, refNo, nominee, startDate, maturityDate, status, notes, principal, compounding, payout, autoRenew, installment, tenureMonths, installmentDay, rate,
        goldForm, goldPurity, silverForm, silverPurity, grams, purchaseAmount, makingCharges, gst, billNo, sgbIssuePrice, pieces,
        symbol, exchange, quantity, avgPrice, currentPrice, priceDate, dividends, sector,
        mfPlan, mfType, sipAmount, sipDay, sipStopped, lumpSum, units, nav, navDate,
        propertyType, address, surveyNo, pattaNo, area, areaUnit, landPrice, stampDuty, brokerage, otherCosts, landValue, valueDate,
        valueSource, monthlyRent, loanOutstanding, otherKind, balance, balanceDate, yearlyContribution, premium, premiumFrequency,
        termYears, sumAssured, bonus, chitValue, chitInstallment, chitMonths, chitPaid, chitPrize, chitDividends, faceValue,
        bondPayout, person, loanAmount, receivedBack, genericInvested, genericValue
    )
    val initialFields = remember { fields }
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = { if (fields != initialFields) confirmDiscard = true else onDismiss() }

    Dialog(onDismissRequest = requestClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().imePadding()) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = requestClose) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.savings_cancel)) }
                    Text(
                        text = stringResource(if (existing == null) R.string.savings_form_add_title else R.string.savings_form_edit_title, stringResource(section.style().title)),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        enabled = canSave,
                        onClick = {
                            val item = (existing ?: SavingsItemEntity(section = section.name, name = "", holder = "", startDate = startDate)).copy(
                                name = name.trim(),
                                holder = holder,
                                institution = institution.trim().ifBlank { null },
                                refNo = refNo.trim().ifBlank { null },
                                nominee = nominee.trim().ifBlank { null },
                                startDate = startDate,
                                subType = when (section) {
                                    SavingsSection.OTHER -> otherKind.name
                                    SavingsSection.GOLD -> goldForm.name
                                    SavingsSection.SILVER -> silverForm.name
                                    else -> null
                                },
                                maturityDate = effectiveMaturity,
                                status = status.name,
                                notes = notes.trim().ifBlank { null }
                            )
                            onSave(item, details!!)
                        },
                        modifier = Modifier.padding(end = 8.dp).testTag("savings_form_save")
                    ) { Text(stringResource(R.string.savings_save)) }
                }
                HorizontalDivider()

                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text(stringResource(R.string.savings_field_name)) },
                        placeholder = { Text(stringResource(section.nameHint())) },
                        modifier = Modifier.fillMaxWidth().testTag("savings_form_name")
                    )
                    OutlinedTextField(
                        value = holder, onValueChange = { holder = it }, singleLine = true,
                        label = { Text(stringResource(R.string.savings_field_holder)) },
                        modifier = Modifier.fillMaxWidth().testTag("savings_form_holder")
                    )
                    if (holders.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            holders.forEach { h -> FilterChip(selected = h.equals(holder.trim(), ignoreCase = true), onClick = { holder = h }, label = { Text(h) }) }
                        }
                    }

                    SectionHeading(R.string.savings_form_heading_amounts)
                    when (section) {
                        SavingsSection.FD -> {
                            NumberField(principal, { principal = it }, R.string.savings_field_principal, isError = principal.isNotBlank() && parseAmount(principal) == null)
                            NumberField(rate, { rate = it }, R.string.savings_field_rate, suffix = "%")
                            DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                            Text(stringResource(R.string.savings_field_tenure_quick), style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(6, 12, 24, 36, 60).forEach { months ->
                                    val target = SavingsCalculator.addMonths(startDate, months)
                                    FilterChip(
                                        selected = maturityDate == target,
                                        onClick = { maturityDate = target },
                                        label = { Text(if (months < 12) stringResource(R.string.savings_months_short, months) else stringResource(R.string.savings_years_short, months / 12)) }
                                    )
                                }
                            }
                            DateField(R.string.savings_field_maturity_date, maturityDate, { maturityDate = it })
                            if (!maturityOk && maturityDate != null) {
                                Text(stringResource(R.string.savings_error_maturity), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(stringResource(R.string.savings_field_interest), style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = payout == null, onClick = { payout = null }, label = { Text(stringResource(R.string.savings_payout_cumulative)) })
                                Frequency.entries.forEach { f ->
                                    FilterChip(selected = payout == f, onClick = { payout = f }, label = { Text(stringResource(R.string.savings_payout_paid_out, stringResource(f.label()))) })
                                }
                            }
                            if (payout == null) {
                                Text(stringResource(R.string.savings_field_compounding), style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Compounding.entries.forEach { c ->
                                        FilterChip(selected = compounding == c, onClick = { compounding = c }, label = { Text(stringResource(c.label()).replaceFirstChar { it.uppercase() }) })
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.savings_field_auto_renew), modifier = Modifier.weight(1f))
                                Switch(checked = autoRenew, onCheckedChange = { autoRenew = it })
                            }
                        }
                        SavingsSection.RD -> {
                            NumberField(installment, { installment = it }, R.string.savings_field_installment, isError = installment.isNotBlank() && parseAmount(installment) == null)
                            NumberField(rate, { rate = it }, R.string.savings_field_rate, suffix = "%")
                            DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                            NumberField(tenureMonths, { tenureMonths = it }, R.string.savings_field_tenure_months, decimal = false)
                            NumberField(installmentDay, { installmentDay = it }, R.string.savings_field_installment_day, decimal = false)
                            effectiveMaturity?.let {
                                Text(stringResource(R.string.savings_matures_on, formatDay(it)), style = MaterialTheme.typography.bodyMedium)
                            }
                            if (existing == null) {
                                Text(stringResource(R.string.savings_rd_history_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        SavingsSection.GOLD -> {
                            Text(stringResource(R.string.savings_field_form), style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GoldForm.entries.forEach { f -> FilterChip(selected = goldForm == f, onClick = { goldForm = f }, label = { Text(stringResource(f.label())) }) }
                            }
                            Text(stringResource(R.string.savings_optional_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!isSgb) NumberField(pieces, { pieces = it }, R.string.savings_field_quantity_pieces_optional, decimal = false, isError = unreadableCount(pieces))
                            NumberField(grams, { grams = it }, if (isSgb) R.string.savings_field_sgb_units else R.string.savings_field_grams_optional, suffix = if (isSgb) null else "g", isError = unreadable(grams))
                            if (!isSgb) {
                                Text(stringResource(R.string.savings_field_purity), style = MaterialTheme.typography.labelMedium)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GoldPurity.entries.forEach { p -> FilterChip(selected = goldPurity == p, onClick = { goldPurity = p }, label = { Text("${p.carats}K") }) }
                                }
                            } else {
                                NumberField(sgbIssuePrice, { sgbIssuePrice = it }, R.string.savings_field_sgb_issue_price)
                            }
                            NumberField(purchaseAmount, { purchaseAmount = it }, R.string.savings_field_purchase_amount_optional, isError = unreadable(purchaseAmount))
                            if (!isSgb) {
                                NumberField(makingCharges, { makingCharges = it }, R.string.savings_field_making_charges)
                                NumberField(gst, { gst = it }, R.string.savings_field_gst)
                            }
                            DateField(if (isSgb) R.string.savings_field_issue_date else R.string.savings_field_purchase_date, startDate, { startDate = it })
                            if (isSgb) {
                                effectiveMaturity?.let { Text(stringResource(R.string.savings_matures_on, formatDay(it)), style = MaterialTheme.typography.bodyMedium) }
                            } else {
                                OutlinedTextField(billNo, { billNo = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_bill_no)) }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                        SavingsSection.STOCK -> {
                            OutlinedTextField(symbol, { symbol = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_symbol)) }, modifier = Modifier.fillMaxWidth())
                            ChipRow(listOf("NSE", "BSE"), exchange, { exchange = it }) { it }
                            NumberField(quantity, { quantity = it }, R.string.savings_field_quantity)
                            NumberField(avgPrice, { avgPrice = it }, R.string.savings_field_avg_price)
                            DateField(R.string.savings_field_buy_date, startDate, { startDate = it })
                            NumberField(currentPrice, { currentPrice = it }, R.string.savings_field_current_price)
                            DateField(R.string.savings_field_price_date, priceDate, { priceDate = it })
                            NumberField(dividends, { dividends = it }, R.string.savings_field_dividends)
                            OutlinedTextField(sector, { sector = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_sector)) }, modifier = Modifier.fillMaxWidth())
                        }
                        SavingsSection.MUTUAL_FUND -> {
                            ChipRow(listOf("DIRECT", "REGULAR"), mfPlan, { mfPlan = it }) { stringResource(if (it == "DIRECT") R.string.savings_mf_direct else R.string.savings_mf_regular) }
                            ChipRow(listOf("EQUITY", "DEBT", "HYBRID", "ELSS"), mfType, { mfType = it }) { stringResource(mfTypeLabel(it)) }
                            DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                            NumberField(sipAmount, { sipAmount = it }, R.string.savings_field_sip_amount)
                            NumberField(sipDay, { sipDay = it }, R.string.savings_field_sip_day, decimal = false)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.savings_field_sip_stopped), modifier = Modifier.weight(1f))
                                Switch(checked = sipStopped != null, onCheckedChange = { sipStopped = if (it) today else null })
                            }
                            sipStopped?.let { DateField(R.string.savings_field_sip_stopped_date, it, { d -> sipStopped = d }) }
                            NumberField(lumpSum, { lumpSum = it }, R.string.savings_field_lump_sum)
                            NumberField(units, { units = it }, R.string.savings_field_units)
                            NumberField(nav, { nav = it }, R.string.savings_field_nav)
                            DateField(R.string.savings_field_nav_date, navDate, { navDate = it })
                            if (existing == null) {
                                Text(stringResource(R.string.savings_mf_history_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        SavingsSection.LAND -> {
                            ChipRow(PropertyType.entries, propertyType, { propertyType = it }) { stringResource(it.label()) }
                            Text(stringResource(R.string.savings_optional_hint_land), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            NumberField(area, { area = it }, R.string.savings_field_area_optional, isError = unreadable(area))
                            ChipRow(AreaUnit.entries, areaUnit, { areaUnit = it }) { stringResource(it.label()) }
                            DateField(R.string.savings_field_purchase_date, startDate, { startDate = it })
                            NumberField(landPrice, { landPrice = it }, R.string.savings_field_land_price_optional, isError = unreadable(landPrice))
                            NumberField(stampDuty, { stampDuty = it }, R.string.savings_field_stamp_duty)
                            NumberField(brokerage, { brokerage = it }, R.string.savings_field_brokerage)
                            NumberField(otherCosts, { otherCosts = it }, R.string.savings_field_other_costs)
                            NumberField(landValue, { landValue = it }, R.string.savings_field_land_value_optional, isError = unreadable(landValue))
                            DateField(R.string.savings_field_value_date, valueDate, { valueDate = it })
                            ChipRow(listOf("OWN_ESTIMATE", "GUIDELINE", "VALUER"), valueSource, { valueSource = it }) { stringResource(valueSourceLabel(it)) }
                            OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.savings_field_address)) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(surveyNo, { surveyNo = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_survey_no)) }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(pattaNo, { pattaNo = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_patta_no)) }, modifier = Modifier.fillMaxWidth())
                            NumberField(monthlyRent, { monthlyRent = it }, R.string.savings_field_monthly_rent)
                            NumberField(loanOutstanding, { loanOutstanding = it }, R.string.savings_field_loan_outstanding)
                        }
                        SavingsSection.OTHER -> {
                            if (existing == null) ChipRow(OtherKind.entries, otherKind, { otherKind = it }) { stringResource(it.label()) }
                            when {
                                isSchemeKind -> {
                                    NumberField(balance, { balance = it }, R.string.savings_field_balance)
                                    DateField(R.string.savings_field_balance_date, balanceDate, { balanceDate = it })
                                    NumberField(yearlyContribution, { yearlyContribution = it }, R.string.savings_field_yearly_contribution)
                                    NumberField(rate, { rate = it }, R.string.savings_field_rate, suffix = "%")
                                    DateField(R.string.savings_field_open_date, startDate, { startDate = it })
                                    val years = when (otherKind) {
                                        OtherKind.PPF -> 15
                                        OtherKind.SUKANYA -> 21
                                        else -> null
                                    }
                                    if (years != null) {
                                        val target = SavingsCalculator.addMonths(startDate, years * 12)
                                        FilterChip(
                                            selected = maturityDate == target, onClick = { maturityDate = target },
                                            label = { Text(stringResource(R.string.savings_years_short, years)) }
                                        )
                                    }
                                    DateField(R.string.savings_field_maturity_date, maturityDate, { maturityDate = it })
                                }
                                otherKind == OtherKind.INSURANCE -> {
                                    NumberField(premium, { premium = it }, R.string.savings_field_premium)
                                    ChipRow(Frequency.entries, premiumFrequency, { premiumFrequency = it }) { stringResource(it.label()).replaceFirstChar { c -> c.uppercase() } }
                                    NumberField(termYears, { termYears = it }, R.string.savings_field_term_years, decimal = false)
                                    DateField(R.string.savings_field_policy_start, startDate, { startDate = it })
                                    NumberField(sumAssured, { sumAssured = it }, R.string.savings_field_sum_assured)
                                    NumberField(bonus, { bonus = it }, R.string.savings_field_bonus)
                                }
                                otherKind == OtherKind.CHIT -> {
                                    NumberField(chitValue, { chitValue = it }, R.string.savings_field_chit_value)
                                    NumberField(chitInstallment, { chitInstallment = it }, R.string.savings_field_installment)
                                    NumberField(chitMonths, { chitMonths = it }, R.string.savings_field_chit_months, decimal = false)
                                    NumberField(chitPaid, { chitPaid = it }, R.string.savings_field_chit_paid, decimal = false)
                                    DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                                    NumberField(chitPrize, { chitPrize = it }, R.string.savings_field_chit_prize)
                                    NumberField(chitDividends, { chitDividends = it }, R.string.savings_field_chit_dividends)
                                }
                                otherKind == OtherKind.BOND -> {
                                    NumberField(faceValue, { faceValue = it }, R.string.savings_field_face_value)
                                    NumberField(rate, { rate = it }, R.string.savings_field_coupon, suffix = "%")
                                    ChipRow(Frequency.entries, bondPayout, { bondPayout = it }) { stringResource(R.string.savings_payout_paid_out, stringResource(it.label())) }
                                    DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                                    DateField(R.string.savings_field_maturity_date, maturityDate, { maturityDate = it })
                                }
                                otherKind == OtherKind.LOAN_GIVEN -> {
                                    OutlinedTextField(person, { person = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_person)) }, modifier = Modifier.fillMaxWidth())
                                    NumberField(loanAmount, { loanAmount = it }, R.string.savings_field_loan_amount)
                                    NumberField(rate, { rate = it }, R.string.savings_field_loan_rate, suffix = "%")
                                    DateField(R.string.savings_field_lent_on, startDate, { startDate = it })
                                    NumberField(receivedBack, { receivedBack = it }, R.string.savings_field_received_back)
                                    DateField(R.string.savings_field_due_date, maturityDate, { maturityDate = it })
                                }
                                else -> {
                                    NumberField(genericInvested, { genericInvested = it }, R.string.savings_field_amount_invested)
                                    NumberField(genericValue, { genericValue = it }, R.string.savings_field_value_now)
                                    DateField(R.string.savings_field_start_date, startDate, { startDate = it })
                                }
                            }
                        }
                        SavingsSection.SILVER -> {
                            Text(stringResource(R.string.savings_field_form), style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SilverForm.entries.forEach { f -> FilterChip(selected = silverForm == f, onClick = { silverForm = f }, label = { Text(stringResource(f.label())) }) }
                            }
                            Text(stringResource(R.string.savings_optional_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            NumberField(pieces, { pieces = it }, R.string.savings_field_quantity_pieces_optional, decimal = false, isError = unreadableCount(pieces))
                            NumberField(grams, { grams = it }, R.string.savings_field_grams_optional, suffix = "g", isError = unreadable(grams))
                            Text(stringResource(R.string.savings_field_purity), style = MaterialTheme.typography.labelMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("99.9", "92.5").forEach { p -> FilterChip(selected = silverPurity == p, onClick = { silverPurity = p }, label = { Text("$p%") }) }
                            }
                            NumberField(silverPurity, { silverPurity = it }, R.string.savings_field_purity_pct, suffix = "%")
                            NumberField(purchaseAmount, { purchaseAmount = it }, R.string.savings_field_purchase_amount_optional, isError = unreadable(purchaseAmount))
                            DateField(R.string.savings_field_purchase_date, startDate, { startDate = it })
                        }
                        else -> Unit
                    }

                    SectionHeading(R.string.savings_form_heading_more)
                    OutlinedTextField(institution, { institution = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_institution)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(refNo, { refNo = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_ref_no)) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(nominee, { nominee = it }, singleLine = true, label = { Text(stringResource(R.string.savings_field_nominee)) }, modifier = Modifier.fillMaxWidth())
                    if (existing != null) {
                        Text(stringResource(R.string.savings_field_status), style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SavingsStatus.entries.forEach { s -> FilterChip(selected = status == s, onClick = { status = s }, label = { Text(stringResource(s.label())) }) }
                        }
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.savings_field_notes)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirmDiscard) {
        DiscardDialog(onKeepEditing = { confirmDiscard = false }, onDiscard = {
            confirmDiscard = false
            onDismiss()
        })
    }
}

@Composable
private fun DiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(R.string.savings_discard_title)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.savings_discard)) } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.savings_keep_editing)) } }
    )
}

@Composable
private fun SectionHeading(label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/** True when something was typed that is not a number (blank is fine: the field is optional). */
private fun unreadable(text: String): Boolean = text.isNotBlank() && parseAmount(text) == null

private fun unreadableCount(text: String): Boolean = text.isNotBlank() && text.trim().toIntOrNull() == null

/** The typed number when it is above zero, otherwise null ("not entered"). */
private fun positive(text: String): Double? = parseAmount(text)?.takeIf { it > 0 }

/** Single-choice chips for a small set of options. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipRow(options: List<T>, selected: T, onSelect: (T) -> Unit, label: @Composable (T) -> String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { o -> FilterChip(selected = o == selected, onClick = { onSelect(o) }, label = { Text(label(o)) }) }
    }
}

private fun SavingsSection.nameHint(): Int = when (this) {
    SavingsSection.FD -> R.string.savings_field_name_hint_fd
    SavingsSection.RD -> R.string.savings_field_name_hint_rd
    SavingsSection.STOCK -> R.string.savings_field_name_hint_stock
    SavingsSection.MUTUAL_FUND -> R.string.savings_field_name_hint_mf
    SavingsSection.GOLD -> R.string.savings_field_name_hint_gold
    SavingsSection.SILVER -> R.string.savings_field_name_hint_silver
    SavingsSection.LAND -> R.string.savings_field_name_hint_land
    SavingsSection.OTHER -> R.string.savings_field_name_hint_other
}

fun mfTypeLabel(type: String?): Int = when (type) {
    "DEBT" -> R.string.savings_mf_debt
    "HYBRID" -> R.string.savings_mf_hybrid
    "ELSS" -> R.string.savings_mf_elss
    else -> R.string.savings_mf_equity
}

fun valueSourceLabel(source: String?): Int = when (source) {
    "GUIDELINE" -> R.string.savings_value_guideline
    "VALUER" -> R.string.savings_value_valuer
    else -> R.string.savings_value_own
}

fun PropertyType.label(): Int = when (this) {
    PropertyType.PLOT -> R.string.savings_property_plot
    PropertyType.HOUSE -> R.string.savings_property_house
    PropertyType.FLAT -> R.string.savings_property_flat
    PropertyType.AGRICULTURAL -> R.string.savings_property_agricultural
    PropertyType.COMMERCIAL -> R.string.savings_property_commercial
}

fun AreaUnit.label(): Int = when (this) {
    AreaUnit.SQFT -> R.string.savings_area_sqft
    AreaUnit.SQM -> R.string.savings_area_sqm
    AreaUnit.CENT -> R.string.savings_area_cent
    AreaUnit.GROUND -> R.string.savings_area_ground
    AreaUnit.ACRE -> R.string.savings_area_acre
    AreaUnit.HECTARE -> R.string.savings_area_hectare
}

fun OtherKind.label(): Int = when (this) {
    OtherKind.PPF -> R.string.savings_other_ppf
    OtherKind.EPF -> R.string.savings_other_epf
    OtherKind.NPS -> R.string.savings_other_nps
    OtherKind.SUKANYA -> R.string.savings_other_sukanya
    OtherKind.INSURANCE -> R.string.savings_other_insurance
    OtherKind.CHIT -> R.string.savings_other_chit
    OtherKind.BOND -> R.string.savings_other_bond
    OtherKind.LOAN_GIVEN -> R.string.savings_other_loan
    OtherKind.CRYPTO -> R.string.savings_other_crypto
    OtherKind.OTHER -> R.string.savings_other_other
}

fun GoldForm.label(): Int = when (this) {
    GoldForm.JEWELLERY -> R.string.savings_gold_jewellery
    GoldForm.COIN -> R.string.savings_metal_coin
    GoldForm.BAR -> R.string.savings_metal_bar
    GoldForm.DIGITAL -> R.string.savings_gold_digital
    GoldForm.SGB -> R.string.savings_gold_sgb
}

fun SilverForm.label(): Int = when (this) {
    SilverForm.ARTICLE -> R.string.savings_silver_article
    SilverForm.COIN -> R.string.savings_metal_coin
    SilverForm.BAR -> R.string.savings_metal_bar
}

fun Frequency.label(): Int = when (this) {
    Frequency.MONTHLY -> R.string.savings_freq_monthly
    Frequency.QUARTERLY -> R.string.savings_freq_quarterly
    Frequency.HALF_YEARLY -> R.string.savings_freq_half_yearly
    Frequency.YEARLY -> R.string.savings_freq_yearly
}

fun Compounding.label(): Int = when (this) {
    Compounding.MONTHLY -> R.string.savings_freq_monthly
    Compounding.QUARTERLY -> R.string.savings_freq_quarterly
    Compounding.HALF_YEARLY -> R.string.savings_freq_half_yearly
    Compounding.YEARLY -> R.string.savings_freq_yearly
    Compounding.SIMPLE -> R.string.savings_compounding_simple
}
