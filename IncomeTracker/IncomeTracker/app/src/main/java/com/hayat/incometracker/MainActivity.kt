package com.hayat.incometracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hayat.incometracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment()); true
                }
                R.id.nav_history -> {
                    showFragment(HistoryFragment()); true
                }
                R.id.nav_settings -> {
                    showFragment(SettingsFragment()); true
                }
                else -> false
            }
        }
    }

    /** Lets a fragment (e.g. Home's "See all") switch tabs programmatically. */
    fun navigateTo(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
