package com.example.data.savings

import org.json.JSONObject

enum class SavingsSection {
    FD, RD, STOCK, MUTUAL_FUND, GOLD, SILVER, LAND, OTHER;

    companion object {
        fun from(value: String?): SavingsSection = entries.firstOrNull { it.name == value } ?: OTHER
    }
}

enum class SavingsStatus {
    ACTIVE, MATURED, CLOSED, SOLD;

    companion object {
        fun from(value: String?): SavingsStatus = entries.firstOrNull { it.name == value } ?: ACTIVE
    }
}

enum class EntryType {
    INVEST, WITHDRAW, INCOME;

    companion object {
        fun from(value: String?): EntryType = entries.firstOrNull { it.name == value } ?: INVEST
    }
}

/** How often FD interest compounds. [perYear] is 0 for simple interest. */
enum class Compounding(val perYear: Int) {
    MONTHLY(12), QUARTERLY(4), HALF_YEARLY(2), YEARLY(1), SIMPLE(0);

    companion object {
        fun from(value: String?): Compounding = entries.firstOrNull { it.name == value } ?: QUARTERLY
    }
}

/** How often money is paid out or paid in: FD/bond interest payouts, insurance premiums. */
enum class Frequency(val perYear: Int) {
    MONTHLY(12), QUARTERLY(4), HALF_YEARLY(2), YEARLY(1);

    val months: Int get() = 12 / perYear

    companion object {
        fun from(value: String?): Frequency? = entries.firstOrNull { it.name == value }
    }
}

enum class GoldForm { JEWELLERY, COIN, BAR, DIGITAL, SGB }

enum class SilverForm { ARTICLE, COIN, BAR }

/** Gold purity in carats; [factor] is the share of pure gold. */
enum class GoldPurity(val carats: Int) {
    K24(24), K22(22), K18(18);

    val factor: Double get() = carats / 24.0

    companion object {
        fun from(value: String?): GoldPurity = entries.firstOrNull { it.name == value } ?: K22
    }
}

/** Area units used for land in India, with their size in square feet. */
enum class AreaUnit(val sqft: Double) {
    SQFT(1.0), SQM(10.7639), CENT(435.6), GROUND(2400.0), ACRE(43_560.0), HECTARE(107_639.1);

    companion object {
        fun from(value: String?): AreaUnit = entries.firstOrNull { it.name == value } ?: SQFT
    }
}

enum class PropertyType { PLOT, HOUSE, FLAT, AGRICULTURAL, COMMERCIAL }

/** The kinds of record under "Other schemes"; stored in SavingsItemEntity.subType. */
enum class OtherKind { PPF, EPF, NPS, SUKANYA, INSURANCE, CHIT, BOND, LOAN_GIVEN, CRYPTO, OTHER }

/** Keys for SavingsSnapshotEntity.key. */
object SnapshotKeys {
    const val ITEM_VALUE = "ITEM_VALUE"
    const val GOLD_24K = "GOLD_24K" // per gram
    const val GOLD_22K = "GOLD_22K" // per gram, optional
    const val SILVER_KG = "SILVER_KG" // per kg
}

/**
 * The section-specific inputs of a savings record, stored as JSON in
 * SavingsItemEntity.detailsJson. Money is in the app currency; rates are % per year.
 */
sealed class SavingsDetails {
    data class Fd(
        val principal: Double,
        val ratePct: Double,
        val compounding: Compounding = Compounding.QUARTERLY,
        val payout: Frequency? = null, // null = cumulative
        val autoRenew: Boolean = false
    ) : SavingsDetails()

    data class Rd(
        val installment: Double,
        val ratePct: Double,
        val tenureMonths: Int,
        val installmentDay: Int
    ) : SavingsDetails()

    data class Stock(
        val symbol: String? = null,
        val exchange: String? = null,
        val quantity: Double,
        val avgPrice: Double,
        val currentPrice: Double,
        val priceDate: Long? = null,
        val dividends: Double = 0.0,
        val sector: String? = null
    ) : SavingsDetails()

    data class MutualFund(
        val amc: String? = null,
        val plan: String? = null, // DIRECT / REGULAR
        val fundType: String? = null, // EQUITY / DEBT / HYBRID / ELSS
        val sipAmount: Double? = null,
        val sipDay: Int? = null,
        val sipStoppedDate: Long? = null,
        val lumpSum: Double? = null, // one-time amount invested on the start date
        val units: Double,
        val nav: Double,
        val navDate: Long? = null
    ) : SavingsDetails()

    data class Gold(
        val form: GoldForm = GoldForm.JEWELLERY,
        val grams: Double, // for SGB: units, 1 unit = 1 g
        val purity: GoldPurity = GoldPurity.K22,
        val purchaseAmount: Double,
        val makingCharges: Double = 0.0,
        val gst: Double = 0.0,
        val billNo: String? = null,
        val sgbIssuePrice: Double? = null
    ) : SavingsDetails()

    data class Silver(
        val form: SilverForm = SilverForm.ARTICLE,
        val grams: Double,
        val purityPct: Double = 99.9,
        val purchaseAmount: Double
    ) : SavingsDetails()

    data class Land(
        val propertyType: PropertyType = PropertyType.PLOT,
        val address: String? = null,
        val surveyNo: String? = null,
        val pattaNo: String? = null,
        val area: Double,
        val areaUnit: AreaUnit = AreaUnit.SQFT,
        val purchasePrice: Double,
        val stampDutyAndRegistration: Double = 0.0,
        val brokerage: Double = 0.0,
        val otherCosts: Double = 0.0,
        val currentValue: Double,
        val valueDate: Long? = null,
        val valueSource: String? = null, // OWN_ESTIMATE / GUIDELINE / VALUER
        val monthlyRent: Double = 0.0,
        val loanOutstanding: Double = 0.0
    ) : SavingsDetails()

    /** PPF, EPF, NPS, Sukanya Samriddhi: a balance that grows with yearly contributions. */
    data class Scheme(
        val balance: Double,
        val balanceDate: Long? = null,
        val yearlyContribution: Double = 0.0,
        val ratePct: Double = 0.0
    ) : SavingsDetails()

    data class Insurance(
        val premium: Double,
        val premiumFrequency: Frequency = Frequency.YEARLY,
        val termYears: Int,
        val sumAssured: Double = 0.0,
        val bonusSoFar: Double = 0.0
    ) : SavingsDetails()

    data class Chit(
        val chitValue: Double,
        val monthlyInstallment: Double,
        val totalMonths: Int,
        val monthsPaid: Int,
        val prizeAmount: Double = 0.0,
        val dividendsReceived: Double = 0.0
    ) : SavingsDetails()

    data class Bond(
        val faceValue: Double,
        val couponPct: Double,
        val payout: Frequency = Frequency.YEARLY
    ) : SavingsDetails()

    data class LoanGiven(
        val person: String,
        val amount: Double,
        val ratePct: Double = 0.0,
        val receivedBack: Double = 0.0
    ) : SavingsDetails()

    /** Crypto, foreign stocks or anything else: just what went in and what it is worth. */
    data class Generic(
        val invested: Double = 0.0,
        val currentValue: Double
    ) : SavingsDetails()

    fun toJson(): String {
        val o = JSONObject()
        when (this) {
            is Fd -> o.put("kind", "FD").put("principal", principal).put("ratePct", ratePct)
                .put("compounding", compounding.name).putOpt("payout", payout?.name).put("autoRenew", autoRenew)
            is Rd -> o.put("kind", "RD").put("installment", installment).put("ratePct", ratePct)
                .put("tenureMonths", tenureMonths).put("installmentDay", installmentDay)
            is Stock -> o.put("kind", "STOCK").putOpt("symbol", symbol).putOpt("exchange", exchange)
                .put("quantity", quantity).put("avgPrice", avgPrice).put("currentPrice", currentPrice)
                .putOpt("priceDate", priceDate).put("dividends", dividends).putOpt("sector", sector)
            is MutualFund -> o.put("kind", "MUTUAL_FUND").putOpt("amc", amc).putOpt("plan", plan)
                .putOpt("fundType", fundType).putOpt("sipAmount", sipAmount).putOpt("sipDay", sipDay)
                .putOpt("sipStoppedDate", sipStoppedDate).putOpt("lumpSum", lumpSum).put("units", units).put("nav", nav).putOpt("navDate", navDate)
            is Gold -> o.put("kind", "GOLD").put("form", form.name).put("grams", grams).put("purity", purity.name)
                .put("purchaseAmount", purchaseAmount).put("makingCharges", makingCharges).put("gst", gst)
                .putOpt("billNo", billNo).putOpt("sgbIssuePrice", sgbIssuePrice)
            is Silver -> o.put("kind", "SILVER").put("form", form.name).put("grams", grams)
                .put("purityPct", purityPct).put("purchaseAmount", purchaseAmount)
            is Land -> o.put("kind", "LAND").put("propertyType", propertyType.name).putOpt("address", address)
                .putOpt("surveyNo", surveyNo).putOpt("pattaNo", pattaNo).put("area", area).put("areaUnit", areaUnit.name)
                .put("purchasePrice", purchasePrice).put("stampDutyAndRegistration", stampDutyAndRegistration)
                .put("brokerage", brokerage).put("otherCosts", otherCosts).put("currentValue", currentValue)
                .putOpt("valueDate", valueDate).putOpt("valueSource", valueSource).put("monthlyRent", monthlyRent)
                .put("loanOutstanding", loanOutstanding)
            is Scheme -> o.put("kind", "SCHEME").put("balance", balance).putOpt("balanceDate", balanceDate)
                .put("yearlyContribution", yearlyContribution).put("ratePct", ratePct)
            is Insurance -> o.put("kind", "INSURANCE").put("premium", premium).put("premiumFrequency", premiumFrequency.name)
                .put("termYears", termYears).put("sumAssured", sumAssured).put("bonusSoFar", bonusSoFar)
            is Chit -> o.put("kind", "CHIT").put("chitValue", chitValue).put("monthlyInstallment", monthlyInstallment)
                .put("totalMonths", totalMonths).put("monthsPaid", monthsPaid).put("prizeAmount", prizeAmount)
                .put("dividendsReceived", dividendsReceived)
            is Bond -> o.put("kind", "BOND").put("faceValue", faceValue).put("couponPct", couponPct).put("payout", payout.name)
            is LoanGiven -> o.put("kind", "LOAN_GIVEN").put("person", person).put("amount", amount)
                .put("ratePct", ratePct).put("receivedBack", receivedBack)
            is Generic -> o.put("kind", "GENERIC").put("invested", invested).put("currentValue", currentValue)
        }
        return o.toString()
    }

    companion object {
        /** Parses [json] written by [toJson]; null when it is empty or unreadable. */
        fun fromJson(json: String?): SavingsDetails? {
            if (json.isNullOrBlank()) return null
            return try {
                val o = JSONObject(json)
                when (o.optString("kind")) {
                    "FD" -> Fd(
                        principal = o.getDouble("principal"), ratePct = o.getDouble("ratePct"),
                        compounding = Compounding.from(o.optStringOrNull("compounding")),
                        payout = Frequency.from(o.optStringOrNull("payout")), autoRenew = o.optBoolean("autoRenew")
                    )
                    "RD" -> Rd(
                        installment = o.getDouble("installment"), ratePct = o.getDouble("ratePct"),
                        tenureMonths = o.getInt("tenureMonths"), installmentDay = o.getInt("installmentDay")
                    )
                    "STOCK" -> Stock(
                        symbol = o.optStringOrNull("symbol"), exchange = o.optStringOrNull("exchange"),
                        quantity = o.getDouble("quantity"), avgPrice = o.getDouble("avgPrice"),
                        currentPrice = o.getDouble("currentPrice"), priceDate = o.optLongOrNull("priceDate"),
                        dividends = o.optDouble("dividends", 0.0), sector = o.optStringOrNull("sector")
                    )
                    "MUTUAL_FUND" -> MutualFund(
                        amc = o.optStringOrNull("amc"), plan = o.optStringOrNull("plan"),
                        fundType = o.optStringOrNull("fundType"), sipAmount = o.optDoubleOrNull("sipAmount"),
                        sipDay = o.optIntOrNull("sipDay"), sipStoppedDate = o.optLongOrNull("sipStoppedDate"),
                        lumpSum = o.optDoubleOrNull("lumpSum"), units = o.getDouble("units"), nav = o.getDouble("nav"), navDate = o.optLongOrNull("navDate")
                    )
                    "GOLD" -> Gold(
                        form = enumOr(o.optStringOrNull("form"), GoldForm.JEWELLERY), grams = o.getDouble("grams"),
                        purity = GoldPurity.from(o.optStringOrNull("purity")), purchaseAmount = o.getDouble("purchaseAmount"),
                        makingCharges = o.optDouble("makingCharges", 0.0), gst = o.optDouble("gst", 0.0),
                        billNo = o.optStringOrNull("billNo"), sgbIssuePrice = o.optDoubleOrNull("sgbIssuePrice")
                    )
                    "SILVER" -> Silver(
                        form = enumOr(o.optStringOrNull("form"), SilverForm.ARTICLE), grams = o.getDouble("grams"),
                        purityPct = o.optDouble("purityPct", 99.9), purchaseAmount = o.getDouble("purchaseAmount")
                    )
                    "LAND" -> Land(
                        propertyType = enumOr(o.optStringOrNull("propertyType"), PropertyType.PLOT),
                        address = o.optStringOrNull("address"), surveyNo = o.optStringOrNull("surveyNo"),
                        pattaNo = o.optStringOrNull("pattaNo"), area = o.getDouble("area"),
                        areaUnit = AreaUnit.from(o.optStringOrNull("areaUnit")), purchasePrice = o.getDouble("purchasePrice"),
                        stampDutyAndRegistration = o.optDouble("stampDutyAndRegistration", 0.0),
                        brokerage = o.optDouble("brokerage", 0.0), otherCosts = o.optDouble("otherCosts", 0.0),
                        currentValue = o.getDouble("currentValue"), valueDate = o.optLongOrNull("valueDate"),
                        valueSource = o.optStringOrNull("valueSource"), monthlyRent = o.optDouble("monthlyRent", 0.0),
                        loanOutstanding = o.optDouble("loanOutstanding", 0.0)
                    )
                    "SCHEME" -> Scheme(
                        balance = o.getDouble("balance"), balanceDate = o.optLongOrNull("balanceDate"),
                        yearlyContribution = o.optDouble("yearlyContribution", 0.0), ratePct = o.optDouble("ratePct", 0.0)
                    )
                    "INSURANCE" -> Insurance(
                        premium = o.getDouble("premium"),
                        premiumFrequency = Frequency.from(o.optStringOrNull("premiumFrequency")) ?: Frequency.YEARLY,
                        termYears = o.getInt("termYears"), sumAssured = o.optDouble("sumAssured", 0.0),
                        bonusSoFar = o.optDouble("bonusSoFar", 0.0)
                    )
                    "CHIT" -> Chit(
                        chitValue = o.getDouble("chitValue"), monthlyInstallment = o.getDouble("monthlyInstallment"),
                        totalMonths = o.getInt("totalMonths"), monthsPaid = o.getInt("monthsPaid"),
                        prizeAmount = o.optDouble("prizeAmount", 0.0), dividendsReceived = o.optDouble("dividendsReceived", 0.0)
                    )
                    "BOND" -> Bond(
                        faceValue = o.getDouble("faceValue"), couponPct = o.getDouble("couponPct"),
                        payout = Frequency.from(o.optStringOrNull("payout")) ?: Frequency.YEARLY
                    )
                    "LOAN_GIVEN" -> LoanGiven(
                        person = o.getString("person"), amount = o.getDouble("amount"),
                        ratePct = o.optDouble("ratePct", 0.0), receivedBack = o.optDouble("receivedBack", 0.0)
                    )
                    "GENERIC" -> Generic(invested = o.optDouble("invested", 0.0), currentValue = o.getDouble("currentValue"))
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

        private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
            enumValues<T>().firstOrNull { it.name == value } ?: default

        private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)
        private fun JSONObject.optLongOrNull(name: String): Long? = if (isNull(name)) null else optLong(name)
        private fun JSONObject.optIntOrNull(name: String): Int? = if (isNull(name)) null else optInt(name)
        private fun JSONObject.optDoubleOrNull(name: String): Double? = if (isNull(name)) null else optDouble(name)
    }
}
