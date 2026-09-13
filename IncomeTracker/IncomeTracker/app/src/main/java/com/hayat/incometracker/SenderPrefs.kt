package com.hayat.incometracker

import android.content.Context

/**
 * Which SMS sender IDs count as "Telebirr" or "CBE". Editable from the Settings
 * page so nobody has to touch code (or Android Studio / Logcat) to fix a wrong
 * shortcode later — that was the biggest pain point with the old notification
 * based approach.
 */
object SenderPrefs {
    private const val PREFS = "income_tracker_prefs"
    private const val KEY_TELEBIRR_SENDERS = "telebirr_senders"
    private const val KEY_CBE_SENDERS = "cbe_senders"

    // Telebirr's confirmation SMS come from the short code "127".
    // CBE's exact sender ID varies by SIM/region — "CBE" is the common one,
    // edit it from Settings > SMS Senders once you see the real one.
    private val DEFAULT_TELEBIRR = setOf("127")
    private val DEFAULT_CBE = setOf("CBE")

    fun telebirrSenders(context: Context): Set<String> = getSet(context, KEY_TELEBIRR_SENDERS, DEFAULT_TELEBIRR)

    fun cbeSenders(context: Context): Set<String> = getSet(context, KEY_CBE_SENDERS, DEFAULT_CBE)

    fun setTelebirrSenders(context: Context, senders: Set<String>) =
        saveSet(context, KEY_TELEBIRR_SENDERS, senders)

    fun setCbeSenders(context: Context, senders: Set<String>) =
        saveSet(context, KEY_CBE_SENDERS, senders)

    /** Returns "Telebirr", "CBE", or null if this address doesn't match either list. */
    fun classify(context: Context, address: String): String? {
        val normalized = normalize(address)
        if (telebirrSenders(context).any { normalize(it) == normalized }) return "Telebirr"
        if (cbeSenders(context).any { normalize(it) == normalized }) return "CBE"
        return null
    }

    private fun normalize(address: String): String =
        address.trim().removePrefix("+251").removePrefix("0").lowercase()

    private fun getSet(context: Context, key: String, default: Set<String>): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(key, null) ?: return default
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet().ifEmpty { default }
    }

    private fun saveSet(context: Context, key: String, senders: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(key, senders.joinToString(",")).apply()
    }
}
