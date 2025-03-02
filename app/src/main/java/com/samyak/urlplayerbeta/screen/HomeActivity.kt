package com.samyak.urlplayerbeta.screen

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.initialization.AdapterStatus
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.samyak.urlplayerbeta.AdManage.Helper
import com.samyak.urlplayerbeta.R
import com.samyak.urlplayerbeta.adapters.ChannelAdapter
import com.samyak.urlplayerbeta.databinding.ActivityHomeBinding
import com.samyak.urlplayerbeta.models.Videos
import com.samyak.urlplayerbeta.utils.ChannelItemDecoration

class HomeActivity : AppCompatActivity() {
    private lateinit var adapter: ChannelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var channelList: MutableList<Videos>
    private lateinit var binding: ActivityHomeBinding
    private lateinit var adHelper: Helper
    private var bannerAd: AdView? = null

    companion object {
        private const val TAG = "HomeActivity"
        private const val UPDATE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize components first
        setupToolbar()
        setupRecyclerView()
        setupAds()

        // Then initialize ads asynchronously
        initializeAds()
    }

    private fun initializeAds() {
        MobileAds.initialize(this) { initializationStatus ->
            val statusMap = initializationStatus.adapterStatusMap
            for ((adapterClass, status) in statusMap) {
                when (status.initializationState) {
                    AdapterStatus.State.NOT_READY -> {
                        Log.e(TAG, "Adapter: $adapterClass is not ready.")
                    }
                    AdapterStatus.State.READY -> {
                        Log.d(TAG, "Adapter: $adapterClass is ready.")
                        // Load banner ad on main thread
                        runOnUiThread { loadBannerAd() }
                    }
                }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "URL Player Beta"
        }
    }

    private fun setupRecyclerView() {
        recyclerView = binding.recyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            addItemDecoration(ChannelItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
            setHasFixedSize(true)
            // Enable recycling of views
            recycledViewPool.setMaxRecycledViews(0, 10)
        }
        
        channelList = mutableListOf()
        initializeAdapter()
        
        // Setup FAB
        binding.addUrl.setOnClickListener {
            startActivity(Intent(this, URLActivity::class.java))
        }

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.Red)
        }
    }

    private fun initializeAdapter() {
        adapter = ChannelAdapter(
            onPlayClick = { video ->
                launchPlayerActivity(video)
            },
            onEditClick = { video ->
                launchUpdateActivity(video)
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter
    }

    private fun launchPlayerActivity(video: Videos) {
        // Show interstitial ad before playing
        if (adHelper.getAdStatus().interstitialAdReady) {
            adHelper.showInterstitialAd { result ->
                when (result) {
                    is Helper.AdResult.Success -> {
                        // Ad shown successfully, proceed with launch
                        startPlayerActivity(video)
                    }
                    is Helper.AdResult.Error -> {
                        // Ad failed to show, proceed directly
                        startPlayerActivity(video)
                    }
                }
            }
        } else {
            // No ad ready, proceed directly
            startPlayerActivity(video)
            // Preload for next time
            adHelper.preloadAds()
        }
    }

    private fun startPlayerActivity(video: Videos) {
        Intent(this, PlayerActivity::class.java).also { intent ->
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            startActivity(intent)
        }
    }

    private fun launchUpdateActivity(video: Videos) {
        // Show interstitial ad before editing
        if (adHelper.getAdStatus().interstitialAdReady) {
            adHelper.showInterstitialAd { result ->
                when (result) {
                    is Helper.AdResult.Success -> {
                        // Ad shown successfully, proceed with launch
                        startUpdateActivity(video)
                    }
                    is Helper.AdResult.Error -> {
                        // Ad failed to show, proceed directly
                        startUpdateActivity(video)
                    }
                }
            }
        } else {
            // No ad ready, proceed directly
            startUpdateActivity(video)
            // Preload for next time
            adHelper.preloadAds()
        }
    }

    private fun startUpdateActivity(video: Videos) {
        Intent(this, UpdateActivity::class.java).also { intent ->
            intent.putExtra("TITLE", video.name)
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            startActivityForResult(intent, UPDATE_REQUEST_CODE)
        }
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
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.bannerAdContainer.visibility = View.GONE
                }
                
                override fun onAdLoaded() {
                    Log.d(TAG, "Banner ad loaded successfully")
                    binding.shimmerViewContainer.stopShimmer()
                    binding.shimmerViewContainer.visibility = View.GONE
                    binding.bannerAdContainer.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading banner ad: ${e.message}")
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

    private fun loadSavedChannels() {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val links = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            
            // Process channels in background
            Thread {
                val newChannelList = mutableListOf<Videos>()
                
                links.forEach { link ->
                    link.split("###").let { parts ->
                        when {
                            parts.size >= 4 -> {
                                newChannelList.add(Videos(
                                    name = parts[0],
                                    url = parts[1],
                                    userAgent = parts[3]
                                ))
                            }
                            parts.size == 3 -> {
                                newChannelList.add(Videos(
                                    name = parts[0],
                                    url = parts[1],
                                    userAgent = null
                                ))
                            }
                            parts.size == 2 -> {
                                newChannelList.add(Videos(
                                    name = parts[0],
                                    url = parts[1],
                                    userAgent = null
                                ))
                            }
                        }
                    }
                }
                
                // Sort channels by name
                newChannelList.sortBy { it.name }
                
                // Update UI on main thread
                runOnUiThread {
                    channelList.clear()
                    channelList.addAll(newChannelList)
                    adapter.updateItems(channelList)
                    updateEmptyState()
                }
            }.start()
            
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.error_loading_channels), Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState() {
        if (channelList.isEmpty()) {
            // Show empty state view
            findViewById<View>(R.id.empty_state_view)?.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            // Show recycler view
            findViewById<View>(R.id.empty_state_view)?.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        binding.shimmerViewContainer.startShimmer()
        loadSavedChannels()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE && resultCode == RESULT_OK) {
            loadSavedChannels()
        }
    }
}