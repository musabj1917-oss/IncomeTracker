package com.hayat.incometracker

import android.content.Context
import org.json.JSONArray

/**
 * Very small local store. No server, no database dependency — just
 * SharedPreferences holding a JSON array. Fine for a personal income log.
 */
object TransactionStore {
    private const val PREFS = "income_tracker_prefs"
    private const val KEY_TXNS = "transactions"

    fun getAll(context: Context): List<Transaction> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TXNS, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Transaction>()
        for (i in 0 until arr.length()) {
            list.add(Transaction.fromJson(arr.getJSONObject(i)))
        }
        // newest first
        return list.sortedByDescending { it.receivedAtMillis }
    }

    /** Returns true if it was added, false if it was a duplicate (same txnId). */
    fun addIfNew(context: Context, txn: Transaction): Boolean {
        val existing = getAll(context)
        if (existing.any { it.txnId == txn.txnId && txn.txnId.isNotBlank() }) {
            return false
        }
        val updated = existing + txn
        save(context, updated)
        return true
    }

    fun clearAll(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, list: List<Transaction>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TXNS, arr.toString()).apply()
    }

    fun totalIncome(context: Context): Double = getAll(context).sumOf { it.amount }
}
