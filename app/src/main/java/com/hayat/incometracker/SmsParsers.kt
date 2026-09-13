package com.hayat.incometracker

import java.text.NumberFormat
import java.util.Locale

object SmsParsers {

    /**
     * Matches Telebirr "money received" SMS like:
     *
     * "Dear Hayat
     * You have received ETB 50.00 from hayat rahmeto(2519****8938) 100504 on 11/09/2026 21:59:18.
     * Your transaction number is DIB6NHE3H2. Your current E-Money Account balance is ETB 105.50.
     * Thank you for using telebirr
     * Ethio telecom"
     *
     * Only matches RECEIVED (income) messages — sent/paid-out messages are ignored
     * so the total only counts money coming in.
     */
    private val telebirrReceivedRegex = Regex(
        "received ETB\\s*([\\d,]+\\.?\\d*)\\s*from\\s*([^(]+?)\\s*\\(.*?on\\s*([\\d/]+\\s+[\\d:]+).*?" +
            "transaction number is\\s*([A-Za-z0-9]+).*?balance is ETB\\s*([\\d,]+\\.?\\d*)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    fun parseTelebirr(text: String, capturedAtMillis: Long): Transaction? {
        val match = telebirrReceivedRegex.find(text) ?: return null
        val (amountStr, sender, date, txnId, balanceStr) = match.destructured
        val amount = parseAmount(amountStr) ?: return null
        val balance = parseAmount(balanceStr)
        return Transaction(
            source = "Telebirr",
            amount = amount,
            senderOrPayer = sender.trim(),
            txnId = txnId.trim(),
            dateText = date.trim(),
            balanceAfter = balance,
            receivedAtMillis = capturedAtMillis
        )
    }

    /**
     * Best-effort CBE "credited" SMS pattern, e.g.:
     * "Dear customer your Account 1000xxxxxxx has been Credited with ETB 500.00
     *  from HAYAT RAHMETO. Your Current Balance is ETB 2,345.00. Thank you for
     *  Banking with CBE!"
     *
     * CBE's exact wording drifts more than Telebirr's, so treat this as a
     * starting point — if it doesn't match your real SMS, open Settings ->
     * "Unmatched SMS" to see the raw text and adjust the regex below to fit.
     */
    private val cbeCreditedRegex = Regex(
        "credited with ETB\\s*([\\d,]+\\.?\\d*)\\s*from\\s*([^.]+?)\\.\\s*.*?" +
            "(?:Current\\s*)?Balance is ETB\\s*([\\d,]+\\.?\\d*)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    fun parseCbe(text: String, capturedAtMillis: Long): Transaction? {
        val match = cbeCreditedRegex.find(text) ?: return null
        val (amountStr, sender, balanceStr) = match.destructured
        val amount = parseAmount(amountStr) ?: return null
        return Transaction(
            source = "CBE",
            amount = amount,
            senderOrPayer = sender.trim(),
            txnId = "",
            dateText = "",
            balanceAfter = parseAmount(balanceStr),
            receivedAtMillis = capturedAtMillis
        )
    }

    fun parse(source: String, text: String, capturedAtMillis: Long): Transaction? = when (source) {
        "Telebirr" -> parseTelebirr(text, capturedAtMillis)
        "CBE" -> parseCbe(text, capturedAtMillis)
        else -> null
    }

    private fun parseAmount(raw: String): Double? {
        return try {
            NumberFormat.getInstance(Locale.US).parse(raw.replace(",", ""))?.toDouble()
        } catch (e: Exception) {
            null
        }
    }
}
