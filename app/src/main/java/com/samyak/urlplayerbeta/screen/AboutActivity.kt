package com.samyak.urlplayerbeta.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.samyak.urlplayerbeta.AdManage.loadBannerAd
import com.samyak.urlplayerbeta.R
import com.samyak.urlplayerbeta.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAboutBinding
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appPackageName by lazy { packageName }
    
    companion object {
        private const val TAG = "AboutActivity"
        private const val UPDATE_REQUEST_CODE = 500
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bannerContainer.loadBannerAd()
        initializeUI()
        setupClickListeners()
    }

    private fun initializeUI() {
        setupToolbar()
        setupVersionInfo()
        binding.updateStatus.text = getString(R.string.checking_updates)
        
        // Check for updates when the activity starts
        checkForUpdates()
    }

    private fun setupClickListeners() {
        binding.checkUpdatesContainer.setOnClickListener {
            binding.updateStatus.text = getString(R.string.checking_updates)
            binding.updateProgressBar.visibility = View.VISIBLE
            checkForUpdates()
        }

        binding.whatIsAppContainer.setOnClickListener {
            openInfoActivity()
        }
        

        
//        binding.officialWebsiteContainer.setOnClickListener {
//            openOfficialWebsite()
//        }
    }

    private fun checkForUpdates() {
        binding.updateProgressBar.visibility = View.VISIBLE
        
        // Create an app update manager and check for updates
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    // Update is available
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatus.text = getString(R.string.update_available)
                    binding.updateStatus.setTextColor(ContextCompat.getColor(this, R.color.Red))
                    
                    // Add click listener to start the update
                    binding.updateStatus.setOnClickListener {
                        startUpdateFlow(appUpdateInfo)
                    }
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    // No update available
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatus.text = getString(R.string.latest_version)
                    binding.updateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                    binding.updateStatus.setOnClickListener(null)
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Update is already in progress
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatus.text = getString(R.string.update_in_progress)
                    binding.updateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                    binding.updateStatus.setOnClickListener {
                        startUpdateFlow(appUpdateInfo)
                    }
                }
                else -> {
                    // Update status unknown
                    binding.updateProgressBar.visibility = View.GONE
                    binding.updateStatus.text = getString(R.string.check_update_failed)
                    binding.updateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                    
                    // Add click listener to open Play Store manually
                    binding.updateStatus.setOnClickListener {
                        openStoreForUpdate()
                    }
                }
            }
        }.addOnFailureListener { e ->
            // Failed to check for updates
            Log.e(TAG, "Error checking for updates", e)
            binding.updateProgressBar.visibility = View.GONE
            binding.updateStatus.text = getString(R.string.check_update_failed)
            binding.updateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            
            // Add click listener to open Play Store manually
            binding.updateStatus.setOnClickListener {
                openStoreForUpdate()
            }
        }
    }
    
    private fun startUpdateFlow(appUpdateInfo: AppUpdateInfo) {
        try {
            // Start an immediate update if available
            if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            } else {
                // Fallback to opening the store
                openStoreForUpdate()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting update flow", e)
            openStoreForUpdate()
        }
    }
    
    private fun openStoreForUpdate() {
        try {
            // Try to open the app page in the Play Store app
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: Exception) {
            // If Play Store app is not installed, open in browser
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.play_store_not_found), Toast.LENGTH_LONG).show()
                Log.e(TAG, "Could not open Play Store", e)
            }
        }
    }

    private fun openInfoActivity() {
        val intent = Intent(this, InfoActivity::class.java)
        startActivity(intent)
    }



    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(appPackageName, 0)
            binding.appVersion.text = getString(R.string.version_format, packageInfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting package info", e)
            binding.appVersion.text = getString(R.string.version_format, "Unknown")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.about)
            
            binding.toolbar.navigationIcon?.setTint(
                ContextCompat.getColor(this@AboutActivity, android.R.color.white)
            )
        }
        
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(this, android.R.color.white)
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // Update flow failed or was cancelled
                Log.e(TAG, "Update flow failed or was cancelled")
                
                // Check for updates again to refresh the status
                checkForUpdates()
            }
        }
    }
} 