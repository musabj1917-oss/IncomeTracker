package com.hayat.incometracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

/**
 * Fires for every incoming SMS. Telebirr and CBE don't reliably post an app
 * notification for a transaction — the actual confirmation is the SMS itself,
 * sent from a short code (Telebirr: 127). Reading that directly is far more
 * reliable than watching for a notification that may never show.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        const val ACTION_NEW_TRANSACTION = "com.hayat.incometracker.NEW_TRANSACTION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val byAddress = messages.groupBy { it.originatingAddress.orEmpty() }

        for ((address, parts) in byAddress) {
            val body = parts.joinToString("") { it.messageBody.orEmpty() }
            handleMessage(context, address, body, System.currentTimeMillis())
        }
    }

    private fun handleMessage(context: Context, address: String, body: String, atMillis: Long) {
        if (body.isBlank()) return
        val source = SenderPrefs.classify(context, address) ?: return
        Log.d(TAG, "SMS from recognized sender ($source, $address)")

        val txn = SmsParsers.parse(source, body, atMillis)
        if (txn == null) {
            UnmatchedSmsStore.add(context, source, body)
            Log.d(TAG, "Did not match a known pattern — saved to Unmatched SMS for review")
            return
        }

        val added = TransactionStore.addIfNew(context, txn)
        if (added) {
            Log.d(TAG, "Recorded income: ${txn.amount} from ${txn.source}")
            context.sendBroadcast(Intent(ACTION_NEW_TRANSACTION))
        }
    }
}
