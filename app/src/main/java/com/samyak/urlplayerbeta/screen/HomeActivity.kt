package com.samyak.urlplayerbeta.screen

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.samyak.urlplayerbeta.AdManage.loadBannerAd
import com.samyak.urlplayerbeta.AdManage.showInterstitialAd
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

    companion object {
        private const val TAG = "HomeActivity"
        private const val UPDATE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bannerAdContainer.loadBannerAd()
        // Initialize components
        setupToolbar()
        setupRecyclerView()
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
        showInterstitialAd(customCode = {
            val url = video.url.lowercase()
            // Check if this is a playlist file using more comprehensive pattern matching
            if (url.endsWith(".m3u") || 
                url.endsWith(".m3u8") && url.contains("playlist") ||
                url.contains("playlist") || 
                url.contains("/list/") ||
                url.contains("channel")) {
                startPlaylistActivity(video)
            } else {
                startPlayerActivity(video)
            }
        })
    }

    private fun startPlayerActivity(video: Videos) {
        Intent(this, PlayerActivity::class.java).also { intent ->
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            intent.putExtra("TITLE", video.name)
            startActivity(intent)
        }
    }

    private fun startPlaylistActivity(video: Videos) {
        Intent(this, PlaylistActivity::class.java).also { intent ->
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            intent.putExtra("TITLE", video.name)
            startActivity(intent)
        }
    }

    private fun launchUpdateActivity(video: Videos) {
        startUpdateActivity(video)
    }

    private fun startUpdateActivity(video: Videos) {
        Intent(this, UpdateActivity::class.java).also { intent ->
            intent.putExtra("TITLE", video.name)
            intent.putExtra("URL", video.url)
            intent.putExtra("USER_AGENT", video.userAgent)
            startActivityForResult(intent, UPDATE_REQUEST_CODE)
        }
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
        loadSavedChannels()
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