package com.hayat.incometracker

import android.content.Context
import android.provider.Telephony

/**
 * Reads the SMS inbox that already exists on the phone (READ_SMS), so
 * transactions received before the app was ever installed still show up
 * once the person grants permission and taps "Scan SMS history".
 */
object SmsHistoryScanner {

    data class Result(val imported: Int, val unmatchedFromKnownSenders: Int, val scanned: Int)

    fun scan(context: Context): Result {
        var imported = 0
        var unmatched = 0
        var scanned = 0

        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DATE + " ASC"
        ) ?: return Result(0, 0, 0)

        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val address = it.getString(addressIdx).orEmpty()
                val body = it.getString(bodyIdx).orEmpty()
                val date = it.getLong(dateIdx)
                if (body.isBlank()) continue

                val source = SenderPrefs.classify(context, address) ?: continue
                scanned++

                val txn = SmsParsers.parse(source, body, date)
                if (txn == null) {
                    UnmatchedSmsStore.add(context, source, body)
                    unmatched++
                    continue
                }
                if (TransactionStore.addIfNew(context, txn)) imported++
            }
        }

        return Result(imported, unmatched, scanned)
    }

    /**
     * Returns the distinct sender addresses seen in the inbox, most-frequent
     * first, so the person can spot the real Telebirr/CBE shortcode without
     * needing Logcat or Android Studio and just tap "Add" in Settings.
     */
    fun distinctSenders(context: Context, limit: Int = 30): List<String> {
        val counts = LinkedHashMap<String, Int>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            null,
            null,
            Telephony.Sms.DATE + " DESC"
        ) ?: return emptyList()

        cursor.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            while (it.moveToNext()) {
                val address = it.getString(addressIdx)?.trim().orEmpty()
                if (address.isEmpty()) continue
                counts[address] = (counts[address] ?: 0) + 1
            }
        }

        return counts.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }
}
