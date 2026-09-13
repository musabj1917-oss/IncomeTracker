package com.hayat.incometracker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * When an SMS comes from a recognized sender (Telebirr/CBE) but the regex
 * doesn't match it, we keep the raw text here instead of silently dropping it.
 * Settings ▸ "Unmatched SMS" shows these so the wording can be copied and the
 * regex fixed, without needing a computer or Logcat.
 */
object UnmatchedSmsStore {
    private const val PREFS = "income_tracker_prefs"
    private const val KEY = "unmatched_sms"
    private const val MAX_KEPT = 20

    data class Entry(val source: String, val body: String, val atMillis: Long)

    fun add(context: Context, source: String, body: String) {
        val list = getAll(context).toMutableList()
        list.add(0, Entry(source, body, System.currentTimeMillis()))
        while (list.size > MAX_KEPT) list.removeAt(list.size - 1)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("source", it.source)
                put("body", it.body)
                put("atMillis", it.atMillis)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun getAll(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(raw)
        val list = mutableListOf<Entry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Entry(o.getString("source"), o.getString("body"), o.getLong("atMillis")))
        }
        return list
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
