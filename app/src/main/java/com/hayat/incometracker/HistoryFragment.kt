package com.hayat.incometracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hayat.incometracker.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter
    private var currentFilter: String? = null // null = All

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshData()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionAdapter(emptyList())
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecyclerView.adapter = adapter

        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipTelebirr -> "Telebirr"
                R.id.chipCbe -> "CBE"
                else -> null
            }
            refreshData()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SmsReceiver.ACTION_NEW_TRANSACTION)
        ContextCompat.registerReceiver(
            requireContext(), refreshReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refreshData()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(refreshReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshData() {
        val binding = _binding ?: return
        val all = TransactionStore.getAll(requireContext())
        val filtered = currentFilter?.let { f -> all.filter { it.source == f } } ?: all
        adapter.updateItems(filtered)
        binding.historyEmptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.historyRecyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}
