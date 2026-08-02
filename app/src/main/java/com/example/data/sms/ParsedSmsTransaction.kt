package com.example.data.sms

data class ParsedSmsTransaction(
    val amount: Double,
    val type: String, // "DEBIT" or "CREDIT"
    val accountLast4: String?,
    val merchantOrSender: String?,
    val suggestedCategory: String,
    val rawMessage: String,
    val senderAddress: String? = null, // e.g. "VD-HDFCBK-T", "AD-HDFCBK-S", "BV-INDBNK-S", "BZ-INDBNK-S"
    val timestamp: Long = System.currentTimeMillis(),
    val isParsedSuccessfully: Boolean = true
)
