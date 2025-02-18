package com.samyak.urlplayerbeta.screen

import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.samyak.urlplayerbeta.R

class URLActivity : AppCompatActivity() {
    private lateinit var titleEditText: EditText
    private lateinit var urlEditText: EditText
    private lateinit var userAgentEditText: EditText
    private lateinit var titleLayout: TextInputLayout
    private lateinit var urlLayout: TextInputLayout

    companion object {
        private val SUPPORTED_URL_PATTERNS = listOf(
            ".m3u8",  // HLS streams
            ".m3u",   // Playlist format
            "rtmp://", // RTMP streams
            "rtsp://", // RTSP streams
            ".mp4",   // Direct video files
            "http://",// HTTP streams
            "https://", // HTTPS streams
            "udp://", // UDP streams
            "rtp://", // RTP streams
            "mms://", // MMS streams
            "ts",     // Transport streams
            "srt://"  // SRT streams
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
            urlLayout.error = getString(R.string.error_invalid_url)
            isValid = false
        }

        return isValid
    }

    private fun isValidStreamUrl(url: String): Boolean {
        // First check if it's a valid URL format
        if (!Patterns.WEB_URL.matcher(url).matches() && 
            !url.startsWith("rtmp://") && 
            !url.startsWith("rtsp://") &&
            !url.startsWith("udp://") &&
            !url.startsWith("rtp://") &&
            !url.startsWith("mms://") &&
            !url.startsWith("srt://")) {
            return false
        }

        // Check if URL contains any of the supported patterns
        return SUPPORTED_URL_PATTERNS.any { pattern ->
            url.lowercase().contains(pattern.lowercase())
        }
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

            // Add new channel with URL type detection
            val urlType = detectUrlType(url)
            val newLinks = currentLinks.toMutableSet()
            newLinks.add("$title###$url###$urlType${if (userAgent.isNotEmpty()) "###$userAgent" else ""}")
            
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

    private fun detectUrlType(url: String): String {
        return when {
            url.contains(".m3u8") -> "HLS"
            url.contains(".m3u") -> "M3U"
            url.startsWith("rtmp://") -> "RTMP"
            url.startsWith("rtsp://") -> "RTSP"
            url.contains(".mp4") -> "MP4"
            url.startsWith("udp://") -> "UDP"
            url.startsWith("rtp://") -> "RTP"
            url.startsWith("mms://") -> "MMS"
            url.contains(".ts") -> "TS"
            url.startsWith("srt://") -> "SRT"
            else -> "HTTP"
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