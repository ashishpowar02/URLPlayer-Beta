package com.samyak.urlplayerbeta

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.samyak.urlplayerbeta.databinding.ActivityMainBinding
import com.samyak.urlplayerbeta.screen.HomeActivity

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        setupToolbar()
        setupClickListeners()
        setupNavigationDrawer()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
            title = getString(R.string.app_name)
        }
        binding.toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
    }

    private fun setupClickListeners() {
        binding.openHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }

    private fun setupNavigationDrawer() {
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_rate -> {
                    rateApp()
                    true
                }
                R.id.nav_share -> {
                    shareApp()
                    true
                }
                R.id.nav_privacy -> {
                    openPrivacyPolicy()
                    true
                }
                R.id.nav_contact -> {
                    contactUs()
                    true
                }
                else -> false
            }
        }
    }

    private fun rateApp() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shareApp() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,
                "Watch your favorite videos with ${getString(R.string.app_name)}!\n" +
                        "Download now: http://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_message)))
    }

    private fun openPrivacyPolicy() {
        val privacyUrl = "https://your-privacy-policy-url.com"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_privacy_policy), Toast.LENGTH_SHORT).show()
        }
    }

    private fun contactUs() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:arrowwouldpro@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject))
        }
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.contact_us)))
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_no_email), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}