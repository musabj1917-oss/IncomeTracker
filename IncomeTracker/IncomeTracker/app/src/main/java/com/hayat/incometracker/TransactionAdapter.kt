package com.hayat.incometracker

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(private var items: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val badge: TextView = view.findViewById(R.id.sourceBadge)
        val source: TextView = view.findViewById(R.id.sourceText)
        val sender: TextView = view.findViewById(R.id.senderText)
        val date: TextView = view.findViewById(R.id.dateText)
        val amount: TextView = view.findViewById(R.id.amountText)
    }

    fun updateItems(newItems: List<Transaction>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val txn = items[position]
        val fmt = NumberFormat.getInstance(Locale.US).apply { maximumFractionDigits = 2 }
        holder.source.text = txn.source.uppercase()
        holder.sender.text = txn.senderOrPayer.ifBlank { txn.source }
        holder.date.text = txn.dateText
        holder.amount.text = "+ ETB ${fmt.format(txn.amount)}"

        holder.badge.text = if (txn.source == "Telebirr") "T" else "C"
        val badgeColor = if (txn.source == "Telebirr") R.color.gold else R.color.gold_deep
        val bg = holder.badge.background.mutate() as GradientDrawable
        bg.setColor(ContextCompat.getColor(holder.itemView.context, badgeColor))
    }

    override fun getItemCount(): Int = items.size
}
