package com.samyak.urlplayerbeta

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.initialization.AdapterStatus
import com.samyak.urlplayerbeta.databinding.ActivityMainBinding
import com.samyak.urlplayerbeta.screen.HomeActivity
import com.samyak.urlplayerbeta.AdManage.Helper
import com.facebook.shimmer.ShimmerFrameLayout
import com.samyak.urlplayerbeta.screen.AboutActivity
import com.samyak.urlplayerbeta.AppUpdate.InAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adHelper: Helper
    private var bannerAd: AdView? = null
    private var adLoadJob: Job? = null
    private val adScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var adRetryCount = 0
    
    // Add install state listener for app updates
    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            // Show a snackbar or notification that the update has been downloaded
            Toast.makeText(
                this,
                "Update downloaded. Restart to install.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Register the launcher for app updates
    private val updateResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            Log.d(TAG, "Update flow failed! Result code: ${result.resultCode}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize In-App Update Manager
        InAppUpdateManager.init(this, updateResultLauncher)
        InAppUpdateManager.registerListener(installStateUpdatedListener)

        // Initialize Mobile Ads SDK with enhanced error handling
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            var isAnyAdapterReady = false
            
            for ((adapterClass, status) in statusMap) {
                when (status.initializationState) {
                    AdapterStatus.State.NOT_READY -> {
                        Log.e(TAG, "Adapter: $adapterClass is not ready.")
                    }
                    AdapterStatus.State.READY -> {
                        Log.d(TAG, "Adapter: $adapterClass is ready.")
                        isAnyAdapterReady = true
                    }
                }
            }
            
            if (isAnyAdapterReady) {
                loadBannerAdWithTimeout()
            } else {
                Log.e(TAG, "No ad adapters are ready")
                hideAdContainers()
            }
        }

        setupToolbar()
        setupClickListeners()
        setupNavigationDrawer()
        setupAds()
    }

    private fun setupAds() {
        adHelper = Helper(this, binding)
        adHelper.preloadAds()
    }

    private fun loadBannerAdWithTimeout() {
        // Cancel any existing job
        adLoadJob?.cancel()
        
        // Start shimmer effect
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.bannerAdContainer.visibility = View.GONE
        
        adLoadJob = adScope.launch {
            try {
                // Set a timeout for ad loading
                withTimeout(10000) { // 10 seconds timeout
                    loadBannerAd()
                    // Wait for ad to either load or fail
                    suspendCancellableCoroutine<Unit> { continuation ->
                        bannerAd?.adListener = object : AdListener() {
                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.e(TAG, "Banner ad failed to load: ${error.message}")
                                if (continuation.isActive) continuation.resume(Unit) {}
                                
                                // Check if the error is due to network connectivity
                                // Network error code is 2, as per AdMob documentation
                                if (error.code == 2) {
                                    Log.d(TAG, "Network error detected, will retry when network is available")
                                    // Hide shimmer temporarily but will retry when network is back
                                    binding.shimmerViewContainer.stopShimmer()
                                    binding.shimmerViewContainer.visibility = View.GONE
                                } else {
                                    // Retry with exponential backoff for other errors
                                    adRetryCount++
                                    Log.d(TAG, "Retrying banner ad load (attempt $adRetryCount)")
                                    adScope.launch {
                                        // Exponential backoff for retries
                                        val delayTime = minOf(1000L * (1 shl minOf(adRetryCount, 6)), 30000L)
                                        delay(delayTime) // Wait with increasing delay, max 30 seconds
                                        loadBannerAdWithTimeout()
                                    }
                                }
                            }
                            
                            override fun onAdLoaded() {
                                Log.d(TAG, "Banner ad loaded successfully")
                                adRetryCount = 0 // Reset retry count on success
                                // Stop shimmer and show banner ad
                                binding.shimmerViewContainer.stopShimmer()
                                binding.shimmerViewContainer.visibility = View.GONE
                                binding.bannerAdContainer.visibility = View.VISIBLE
                                if (continuation.isActive) continuation.resume(Unit) {}
                            }
                            
                            override fun onAdImpression() {
                                // Log impression for analytics
                                Log.d(TAG, "Banner ad impression recorded")
                            }
                            
                            override fun onAdClicked() {
                                // Log click for analytics
                                Log.d(TAG, "Banner ad was clicked")
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Banner ad load timed out")
                // Even on timeout, retry loading the ad
                adRetryCount++
                Log.d(TAG, "Retrying banner ad load after timeout (attempt $adRetryCount)")
                adScope.launch {
                    val delayTime = minOf(1000L * (1 shl minOf(adRetryCount, 6)), 30000L)
                    delay(delayTime)
                    loadBannerAdWithTimeout()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in ad loading coroutine: ${e.message}")
                // Retry on other exceptions too
                adRetryCount++
                Log.d(TAG, "Retrying banner ad load after error (attempt $adRetryCount)")
                adScope.launch {
                    val delayTime = minOf(1000L * (1 shl minOf(adRetryCount, 6)), 30000L)
                    delay(delayTime)
                    loadBannerAdWithTimeout()
                }
            }
        }
    }

    private fun hideAdContainers() {
        binding.shimmerViewContainer.stopShimmer()
        binding.shimmerViewContainer.visibility = View.GONE
        binding.bannerAdContainer.visibility = View.GONE
    }

    private fun loadBannerAd() {
        try {
            val adView = AdView(this)
            adView.adUnitId = getString(R.string.admob_banner_id)
            
            // Get the optimal ad size based on screen dimensions
            val adSize = getAdSize()
            adView.setAdSize(adSize)
            
            Log.d(TAG, "Loading banner ad with size: ${adSize.width}x${adSize.height}")
            
            binding.bannerAdContainer.removeAllViews()
            binding.bannerAdContainer.addView(adView)

            // Build ad request with smart targeting options
            val adRequest = AdRequest.Builder()
                .setHttpTimeoutMillis(15000) // Increase timeout for slow networks
                .build()
                
            adView.loadAd(adRequest)
            bannerAd = adView
        } catch (e: Exception) {
            Log.e(TAG, "Error creating banner ad: ${e.message}")
            hideAdContainers()
        }
    }

    private fun getAdSize(): AdSize {
        // Determine the screen width to use for the ad width.
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = binding.bannerAdContainer.width.toFloat()

        // If the ad container width isn't available, default to the full screen width
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
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
            adHelper.showCounterInterstitialAd(
                threshold = 1,
                onAdShown = {
                    startActivity(Intent(this, HomeActivity::class.java))
                },
                onAdNotShown = {
                    startActivity(Intent(this, HomeActivity::class.java))
                }
            )
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
                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
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

    override fun onResume() {
        super.onResume()
        binding.shimmerViewContainer.startShimmer()
        if (::adHelper.isInitialized && !adHelper.isAnyAdReady()) {
            adHelper.preloadAds()
        }
        
        // Check if banner ad needs to be reloaded
        if (bannerAd == null || binding.bannerAdContainer.visibility != View.VISIBLE) {
            adRetryCount = 0 // Reset retry count
            loadBannerAdWithTimeout()
        } else {
            // Check if the ad has been showing for a long time and might need refreshing
            adScope.launch {
                delay(300000) // 5 minutes
                if (isActive && !isFinishing) {
                    Log.d(TAG, "Refreshing banner ad after 5 minutes")
                    loadBannerAdWithTimeout()
                }
            }
        }
    }

    override fun onPause() {
        binding.shimmerViewContainer.stopShimmer()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bannerAd?.destroy()
        if (::adHelper.isInitialized) {
            adHelper.destroy()
        }
        
        // Unregister the update listener
        InAppUpdateManager.unregisterListener(installStateUpdatedListener)
        
        // Cancel all coroutines
        adLoadJob?.cancel()
        adScope.cancel()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}