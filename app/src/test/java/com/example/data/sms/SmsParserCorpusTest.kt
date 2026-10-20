package com.example.data.sms

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Base64

/**
 * Optional check of the parser against a private export of real SMS, compared with what the user
 * kept or dismissed in the app. Skipped unless SMS_CORPUS points at a file with one tab-separated
 * line per SMS: `kept(true|false)<TAB>type(DEBIT|CREDIT)<TAB>base64(body)`. The corpus never goes
 * into the repo; run with:
 *   SMS_CORPUS=/path/corpus.tsv ./gradlew testDebugUnitTest --tests '*SmsParserCorpusTest*' -i
 */
class SmsParserCorpusTest {

    @Test
    fun reportAgreementWithUserDecisions() {
        val path = System.getenv("SMS_CORPUS")
        assumeTrue("SMS_CORPUS not set", !path.isNullOrBlank() && File(path).exists())

        var keptAccepted = 0
        var keptRejected = 0
        var dismissedAccepted = 0
        var dismissedRejected = 0
        var typeMismatches = 0
        val keptButRejected = mutableListOf<String>()
        val dismissedButAccepted = mutableListOf<String>()

        File(path!!).forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val (keptField, expectedType, encodedBody) = line.split("\t")
            val body = String(Base64.getDecoder().decode(encodedBody))
            val parsed = SmsParser.parseMessage(body)
            val kept = keptField.toBoolean()
            when {
                kept && parsed != null -> {
                    keptAccepted++
                    if (parsed.type != expectedType) typeMismatches++
                }
                kept -> {
                    keptRejected++
                    keptButRejected += mask(body)
                }
                parsed != null -> {
                    dismissedAccepted++
                    dismissedButAccepted += mask(body)
                }
                else -> dismissedRejected++
            }
        }

        println("kept: accepted=$keptAccepted rejected=$keptRejected (type mismatches=$typeMismatches)")
        println("dismissed: accepted=$dismissedAccepted rejected=$dismissedRejected")
        println("-- kept but rejected:")
        keptButRejected.distinct().forEach { println("  $it") }
        println("-- dismissed but accepted:")
        dismissedButAccepted.distinct().forEach { println("  $it") }
    }

    // Digits masked and line breaks flattened so the report can be read without exposing numbers.
    private fun mask(body: String) = body.replace(Regex("[0-9]"), "#").replace(Regex("\\s+"), " ").take(160)
}
