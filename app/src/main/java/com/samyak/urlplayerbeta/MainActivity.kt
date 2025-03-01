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

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adHelper: Helper
    private var bannerAd: AdView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Mobile Ads SDK with enhanced error handling
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                when (status.initializationState) {
                    AdapterStatus.State.NOT_READY -> {
                        Log.e(TAG, "Adapter: $adapterClass is not ready.")
                    }
                    AdapterStatus.State.READY -> {
                        Log.d(TAG, "Adapter: $adapterClass is ready.")
                        loadBannerAd()
                    }
                }
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

    private fun loadBannerAd() {
        try {
            // Start shimmer effect
            binding.shimmerViewContainer.startShimmer()
            binding.shimmerViewContainer.visibility = View.VISIBLE
            binding.bannerAdContainer.visibility = View.GONE
            
            val adView = AdView(this)
            adView.adUnitId = getString(R.string.admob_banner_id)
            adView.setAdSize(getAdSize())
            
            binding.bannerAdContainer.removeAllViews()
            binding.bannerAdContainer.addView(adView)

            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)

            bannerAd = adView

            adView.adListener = object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Banner ad failed to load: ${error.message}")
                    // Stop shimmer and hide ad container
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.bannerAdContainer.visibility = View.GONE
                }
                
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully")
                    // Stop shimmer and show banner ad
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.bannerAdContainer.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner ad: ${e.message}")
            // Stop shimmer and hide containers
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.bannerAdContainer.visibility = View.GONE
        }
    }

    private fun getAdSize(): AdSize {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = binding.bannerAdContainer.width.toFloat()

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
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}