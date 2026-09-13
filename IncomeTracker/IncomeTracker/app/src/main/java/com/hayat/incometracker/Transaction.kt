package com.hayat.incometracker

import org.json.JSONObject

data class Transaction(
    val source: String,        // "Telebirr" or "CBE"
    val amount: Double,
    val senderOrPayer: String,
    val txnId: String,
    val dateText: String,      // as printed in the notification
    val balanceAfter: Double?,
    val receivedAtMillis: Long // when our app captured it
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("source", source)
        put("amount", amount)
        put("senderOrPayer", senderOrPayer)
        put("txnId", txnId)
        put("dateText", dateText)
        put("balanceAfter", balanceAfter ?: JSONObject.NULL)
        put("receivedAtMillis", receivedAtMillis)
    }

    companion object {
        fun fromJson(o: JSONObject): Transaction = Transaction(
            source = o.getString("source"),
            amount = o.getDouble("amount"),
            senderOrPayer = o.getString("senderOrPayer"),
            txnId = o.getString("txnId"),
            dateText = o.getString("dateText"),
            balanceAfter = if (o.isNull("balanceAfter")) null else o.getDouble("balanceAfter"),
            receivedAtMillis = o.getLong("receivedAtMillis")
        )
    }
}
