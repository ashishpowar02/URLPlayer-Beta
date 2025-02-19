package com.samyak.urlplayerbeta

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
import com.samyak.urlplayerbeta.adapters.ChannelAdapter
import com.samyak.urlplayerbeta.models.Videos
import com.samyak.urlplayerbeta.screen.PlayerActivity
import com.samyak.urlplayerbeta.screen.URLActivity
import com.samyak.urlplayerbeta.screen.UpdateActivity
import com.samyak.urlplayerbeta.utils.ChannelItemDecoration

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: ChannelAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var channelList: MutableList<Videos>

    companion object {
        private const val UPDATE_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "M3U8 PlayList"
        }

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(ChannelItemDecoration(resources.getDimensionPixelSize(R.dimen.item_spacing)))
            setHasFixedSize(true)
        }
        channelList = mutableListOf()
        
        // Initialize adapter with all required callbacks
        adapter = ChannelAdapter(
            onPlayClick = { video ->
                Intent(this, PlayerActivity::class.java).apply {
                    putExtra("URL", video.url)
                    putExtra("USER_AGENT", video.userAgent)
                    startActivity(this)
                }
            },
            onEditClick = { video ->
                Intent(this, UpdateActivity::class.java).apply {
                    putExtra("TITLE", video.name)
                    putExtra("URL", video.url)
                    putExtra("USER_AGENT", video.userAgent)
                    startActivityForResult(this, UPDATE_REQUEST_CODE)
                }
            },
            onError = { message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter
        
        // Load saved channels
        loadSavedChannels()

        // Setup FAB
        findViewById<FloatingActionButton>(R.id.add_Url).setOnClickListener {
            startActivity(Intent(this, URLActivity::class.java))
        }

        // Set status bar color
        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = getColor(R.color.black)
        }
    }

    private fun loadSavedChannels() {
        try {
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val links = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            
            channelList.clear()
            links.forEach { link ->
                link.split("###").let { parts ->
                    when (parts.size) {
                        2 -> channelList.add(Videos(parts[0], parts[1], ""))
                        3 -> channelList.add(Videos(parts[0], parts[1], parts[2]))
                    }
                }
            }
            
            // Sort channels by name
            channelList.sortBy { it.name }
            
            // Update adapter with new list
            adapter.updateItems(channelList)
            
            // Show empty state if needed
            updateEmptyState()
        } catch (e: Exception) {
//            Toast.makeText(this, getString(R.string.error_loading_channels), Toast.LENGTH_SHORT).show()
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