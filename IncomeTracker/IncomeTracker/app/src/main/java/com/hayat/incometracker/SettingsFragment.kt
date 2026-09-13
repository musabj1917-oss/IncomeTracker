package com.hayat.incometracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hayat.incometracker.databinding.FragmentSettingsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.telebirrSendersInput.setText(
            SenderPrefs.telebirrSenders(requireContext()).joinToString(", ")
        )
        binding.cbeSendersInput.setText(
            SenderPrefs.cbeSenders(requireContext()).joinToString(", ")
        )

        binding.saveSendersButton.setOnClickListener { saveSenders() }
        binding.detectSendersButton.setOnClickListener { detectSenders() }
        binding.clearDataButton.setOnClickListener { confirmClearData() }

        refreshUnmatchedSms()
    }

    override fun onResume() {
        super.onResume()
        refreshUnmatchedSms()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun saveSenders() {
        val telebirr = splitCsv(binding.telebirrSendersInput.text.toString())
        val cbe = splitCsv(binding.cbeSendersInput.text.toString())
        if (telebirr.isEmpty() || cbe.isEmpty()) {
            Toast.makeText(requireContext(), "Keep at least one sender per source", Toast.LENGTH_SHORT).show()
            return
        }
        SenderPrefs.setTelebirrSenders(requireContext(), telebirr)
        SenderPrefs.setCbeSenders(requireContext(), cbe)
        Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun splitCsv(raw: String): Set<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    private fun hasReadSms(): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun detectSenders() {
        if (!hasReadSms()) {
            Toast.makeText(requireContext(), "Allow SMS access on the Home tab first", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS))
            return
        }
        val ctx = requireContext().applicationContext
        Thread {
            val senders = SmsHistoryScanner.distinctSenders(ctx)
            Handler(Looper.getMainLooper()).post {
                if (_binding == null) return@post
                binding.detectedSendersText.visibility = View.VISIBLE
                binding.detectedSendersText.text = if (senders.isEmpty()) {
                    "No SMS found on this phone."
                } else {
                    "Seen in your inbox:\n" + senders.joinToString("\n") { "• $it" } +
                        "\n\nType the real one into Telebirr/CBE senders above, then Save."
                }
            }
        }.start()
    }

    private fun refreshUnmatchedSms() {
        val entries = UnmatchedSmsStore.getAll(requireContext())
        binding.unmatchedSmsText.text = if (entries.isEmpty()) {
            "None yet."
        } else {
            val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
            entries.joinToString("\n\n") { e ->
                "[${e.source} • ${fmt.format(Date(e.atMillis))}]\n${e.body}"
            }
        }
    }

    private fun confirmClearData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear all transactions?")
            .setMessage("This removes every recorded transaction from this device. It can't be undone.")
            .setPositiveButton("Clear") { _, _ ->
                TransactionStore.clearAll(requireContext())
                Toast.makeText(requireContext(), "Cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
