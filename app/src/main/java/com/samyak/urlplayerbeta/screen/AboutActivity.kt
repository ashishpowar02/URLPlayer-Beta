package com.samyak.urlplayerbeta.screen

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samyak.urlplayerbeta.AdManage.Helper
import com.samyak.urlplayerbeta.R
import com.samyak.urlplayerbeta.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAboutBinding
    private lateinit var adHelper: Helper
    
    companion object {
        private const val TAG = "AboutActivity"
        private const val CLICK_THRESHOLD = 2
        private const val MAX_AD_ATTEMPTS = 5
        private const val RETRY_DELAY = 5000L
        private const val CLICK_COOLDOWN = 1000L
    }

    private var adLoadAttempts = 0
    private var isAdLoading = false
    private var lastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUI()
        initializeAds()
        setupClickListeners()
    }

    private fun initializeUI() {
        setupToolbar()
        setupVersionInfo()
    }

    private fun initializeAds() {
        if (!::adHelper.isInitialized) {
            adHelper = Helper(this, binding)
            loadBannerAd()
            preloadInterstitialAd()
        }
    }

    private fun loadBannerAd() {
        if (!isAdLoading && adLoadAttempts < MAX_AD_ATTEMPTS) {
            isAdLoading = true
            adLoadAttempts++
            
            adHelper.loadBannerAd(R.id.banner_container) { result ->
                isAdLoading = false
                when (result) {
                    is Helper.AdResult.Success -> {
                        Log.d(TAG, "Banner ad loaded successfully")
                        adLoadAttempts = 0 // Reset attempts on success
                    }
                    is Helper.AdResult.Error -> {
                        Log.e(TAG, "Banner ad failed to load: ${result.message}")
                        retryLoadingBannerAd()
                    }
                }
            }
        }
    }

    private fun retryLoadingBannerAd() {
        if (adLoadAttempts < MAX_AD_ATTEMPTS) {
            binding.root.postDelayed({
                loadBannerAd()
            }, RETRY_DELAY)
        }
    }

    private fun preloadInterstitialAd() {
        if (!isAdLoading) {
            adHelper.preloadAds()
        }
    }

    private fun setupClickListeners() {
        binding.developerInfo.setOnClickListener {
            handleAdClick { showInterstitialAd() }
        }

        binding.appDescription.setOnClickListener {
            handleAdClick { showInterstitialAd() }
        }
    }

    private fun handleAdClick(showAd: () -> Unit) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= CLICK_COOLDOWN) {
            lastClickTime = currentTime
            showAd()
        }
    }

    private fun showInterstitialAd() {
        if (!adHelper.isAnyAdReady()) {
            preloadInterstitialAd()
            return
        }

        adHelper.showCounterInterstitialAd(
            threshold = CLICK_THRESHOLD,
            onAdShown = {
                Log.d(TAG, "Interstitial ad shown successfully")
                // Preload next ad
                preloadInterstitialAd()
            },
            onAdNotShown = { message ->
                Log.d(TAG, "Interstitial ad not shown: $message")
                if (message.contains("not loaded")) {
                    preloadInterstitialAd()
                }
            }
        )
    }

    private fun setupVersionInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
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

    override fun onResume() {
        super.onResume()
        if (!adHelper.isAnyAdReady() && !isAdLoading) {
            adLoadAttempts = 0 // Reset attempts on resume
            loadBannerAd()
            preloadInterstitialAd()
        }
    }

    override fun onPause() {
        super.onPause()
        isAdLoading = false // Reset loading state
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onDestroy() {
        if (::adHelper.isInitialized) {
            adHelper.destroy()
        }
        super.onDestroy()
    }
} 