package com.hayat.incometracker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hayat.incometracker.databinding.FragmentHomeBinding
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshData()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshData() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionAdapter(emptyList())
        binding.recentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recentRecyclerView.adapter = adapter

        binding.grantAccessButton.setOnClickListener {
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
        }

        binding.scanHistoryButton.setOnClickListener { runHistoryScan() }

        binding.seeAllText.setOnClickListener {
            (activity as? MainActivity)?.navigateTo(R.id.nav_history)
        }

        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
            binding.swipeRefresh.isRefreshing = false
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

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun runHistoryScan() {
        if (!hasSmsPermission()) {
            Toast.makeText(requireContext(), "Allow SMS access first", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
            return
        }
        val ctx = requireContext().applicationContext
        Toast.makeText(requireContext(), "Scanning SMS history…", Toast.LENGTH_SHORT).show()
        Thread {
            val result = SmsHistoryScanner.scan(ctx)
            Handler(Looper.getMainLooper()).post {
                if (_binding == null) return@post
                refreshData()
                Toast.makeText(
                    requireContext(),
                    "Imported ${result.imported} transaction(s) from ${result.scanned} matching SMS",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()
    }

    private fun refreshData() {
        val binding = _binding ?: return
        val all = TransactionStore.getAll(requireContext())
        adapter.updateItems(all.take(5))
        binding.emptyText.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        binding.recentRecyclerView.visibility = if (all.isEmpty()) View.GONE else View.VISIBLE

        val fmt = NumberFormat.getInstance(Locale.US).apply { maximumFractionDigits = 2 }
        binding.totalIncomeText.text = "ETB ${fmt.format(TransactionStore.totalIncome(requireContext()))}"

        binding.statusText.text = if (hasSmsPermission()) {
            "SMS access granted — listening for Telebirr & CBE"
        } else {
            "SMS access not granted yet — tap below to allow it"
        }
        binding.grantAccessButton.visibility = if (hasSmsPermission()) View.GONE else View.VISIBLE
    }
}
