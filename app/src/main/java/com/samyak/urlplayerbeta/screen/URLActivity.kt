package com.samyak.urlplayerbeta.screen

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.samyak.urlplayerbeta.R

import com.samyak.urlplayerbeta.models.Videos

class URLActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var userAgentEditText: EditText
    private lateinit var titleLayout: TextInputLayout
    private lateinit var urlLayout: TextInputLayout

    companion object {
        private val SUPPORTED_VIDEO_EXTENSIONS = listOf(
            ".m3u8",  // HLS streams
            ".mp4",   // MP4 videos
            ".avi",   // AVI videos
            ".mkv",   // MKV videos
            ".m3u",   // Playlist format
            ".ts",    // Transport streams
            ".mov",   // QuickTime videos
            ".webm",  // WebM videos
            ".mpd"    // DASH streams
        )

        private val SUPPORTED_PROTOCOLS = listOf(
            "http://",  // HTTP
            "https://", // HTTPS
            "rtmp://",  // RTMP
            "rtsp://",  // RTSP
            "udp://",   // UDP
            "rtp://",   // RTP
            "mms://",   // MMS
            "srt://"    // SRT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_urlactivity)

        setupToolbar()
        initializeViews()
        setupClickListeners()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = getString(R.string.add_url)
        }
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white))
        
        // Set navigation icon color to white
        toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.white))
    }

    private fun initializeViews() {
        // Initialize EditTexts
        titleEditText = findViewById(R.id.titleEditText)
        urlEditText = findViewById(R.id.urlEditText)
        userAgentEditText = findViewById(R.id.userAgentEditText)

        // Initialize TextInputLayouts
        titleLayout = titleEditText.parent.parent as TextInputLayout
        urlLayout = urlEditText.parent.parent as TextInputLayout

        // Clear errors on text change
        titleEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) titleLayout.error = null
        }
        urlEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) urlLayout.error = null
        }
    }

    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            if (validateInputs()) {
                saveChannelDetails()
            }
        }
    }

    private fun validateInputs(): Boolean {
        val title = titleEditText.text.toString().trim()
        val url = urlEditText.text.toString().trim()
        var isValid = true

        // Validate title
        if (title.isEmpty()) {
            titleLayout.error = getString(R.string.error_title_required)
            isValid = false
        } else if (title.length < 3) {
            titleLayout.error = getString(R.string.error_title_too_short)
            isValid = false
        }

        // Validate URL
        if (url.isEmpty()) {
            urlLayout.error = getString(R.string.error_url_required)
            isValid = false
        } else if (!isValidStreamUrl(url)) {
            showUrlError()
            isValid = false
        }

        return isValid
    }

    /**
     * Validate whether a string is a valid URL.
     */
    private fun isValidUrl(url: String): Boolean {
        return SUPPORTED_PROTOCOLS.any { protocol ->
            url.lowercase().startsWith(protocol)
        }
    }

    /**
     * Validate whether a string is a valid stream URL.
     */
    private fun isValidStreamUrl(url: String): Boolean {
        if (!isValidUrl(url)) return false

        val lowercaseUrl = url.lowercase()
        
        // Check for supported file extensions
        if (SUPPORTED_VIDEO_EXTENSIONS.any { ext -> lowercaseUrl.endsWith(ext) }) {
            return true
        }

        // Check for streaming keywords in the URL
        return lowercaseUrl.contains("stream") || 
               lowercaseUrl.contains("live") || 
               lowercaseUrl.contains("video") ||
               lowercaseUrl.contains("play")
    }

    private fun detectUrlType(url: String): String {
        val lowercaseUrl = url.lowercase()
        return when {
            lowercaseUrl.endsWith(".m3u8") -> "HLS"
            lowercaseUrl.endsWith(".mp4") -> "MP4"
            lowercaseUrl.endsWith(".avi") -> "AVI"
            lowercaseUrl.endsWith(".mkv") -> "MKV"
            lowercaseUrl.endsWith(".m3u") -> "M3U"
            lowercaseUrl.endsWith(".mpd") -> "DASH"
            lowercaseUrl.endsWith(".ts") -> "TS"
            lowercaseUrl.endsWith(".mov") -> "MOV"
            lowercaseUrl.endsWith(".webm") -> "WEBM"
            lowercaseUrl.startsWith("rtmp://") -> "RTMP"
            lowercaseUrl.startsWith("rtsp://") -> "RTSP"
            lowercaseUrl.startsWith("udp://") -> "UDP"
            lowercaseUrl.startsWith("rtp://") -> "RTP"
            lowercaseUrl.startsWith("mms://") -> "MMS"
            lowercaseUrl.startsWith("srt://") -> "SRT"
            else -> "HTTP"
        }
    }

    private fun showUrlError() {
        urlLayout.error = getString(R.string.error_invalid_url)
        urlLayout.isErrorEnabled = true
    }

    private fun saveChannelDetails() {
        try {
            val title = titleEditText.text.toString().trim()
            val url = urlEditText.text.toString().trim()
            val userAgent = userAgentEditText.text.toString().trim()

            // Get existing channels
            val sharedPreferences = getSharedPreferences("M3U8Links", MODE_PRIVATE)
            val currentLinks = sharedPreferences.getStringSet("links", mutableSetOf()) ?: mutableSetOf()
            
            // Check for duplicate titles
            if (currentLinks.any { it.split("###")[0] == title }) {
                titleLayout.error = getString(R.string.error_title_exists)
                return
            }

            // Create Videos object
            val video = Videos(
                name = title,
                url = url,
                userAgent = if (userAgent.isNotEmpty()) userAgent else null
            )

            // Add new channel with URL type detection
            val urlType = detectUrlType(url)
            val newLinks = currentLinks.toMutableSet()
            
            // Format: title###url###urlType###userAgent
            val channelData = buildString {
                append("${video.name}###${video.url}###$urlType")
                if (!video.userAgent.isNullOrEmpty()) {
                    append("###${video.userAgent}")
                }
            }
            
            newLinks.add(channelData)
            
            // Save to SharedPreferences
            sharedPreferences.edit().apply {
                putStringSet("links", newLinks)
                apply()
            }

            showSuccessAndFinish()
        } catch (e: Exception) {
            showError(e.message ?: getString(R.string.error_saving_channel))
        }
    }

    private fun showSuccessAndFinish() {
        Toast.makeText(this, getString(R.string.success_channel_saved), Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}