package com.samyak.urlplayerbeta.screen

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.samyak.urlplayerbeta.R

class PlayerActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var imageViewFullScreen: ImageView
    private lateinit var imageViewLock: ImageView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout
    
    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullScreen = false
    private var isLock = false
    private var url: String? = null
    private var userAgent: String? = null

    companion object {
        private const val INCREMENT_MILLIS = 5000L
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        // Keep screen on while playing video
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get URL from intent
        url = intent.getStringExtra("URL")
        userAgent = intent.getStringExtra("USER_AGENT")

        if (url == null) {
            showError(getString(R.string.error_playback))
            finish()
            return
        }

        initializeViews()
        setupPlayer()
        setupLockScreen()
        setupFullScreen()
    }

    private fun initializeViews() {
        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        imageViewFullScreen = findViewById(R.id.imageViewFullScreen)
        imageViewLock = findViewById(R.id.imageViewLock)
        linearLayoutControlUp = findViewById(R.id.linearLayoutControlUp)
        linearLayoutControlBottom = findViewById(R.id.linearLayoutControlBottom)

        findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupPlayer() {
        try {
            player = ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(INCREMENT_MILLIS)
                .setSeekForwardIncrementMs(INCREMENT_MILLIS)
                .build()
                .also { exoPlayer ->
                    playerView.player = exoPlayer

                    val dataSourceFactory = DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent ?: Util.getUserAgent(this, "URLPlayerBeta"))
                        .setAllowCrossProtocolRedirects(true)

                    // Create appropriate media source based on URL type
                    val mediaSource = when {
                        url?.endsWith(".m3u8", ignoreCase = true) == true -> {
                            // HLS stream
                            HlsMediaSource.Factory(dataSourceFactory)
                                .setAllowChunklessPreparation(true)
                                .createMediaSource(MediaItem.fromUri(url!!))
                        }
                        url?.endsWith(".mp4", ignoreCase = true) == true ||
                        url?.endsWith(".avi", ignoreCase = true) == true ||
                        url?.endsWith(".mkv", ignoreCase = true) == true ||
                        url?.endsWith(".mov", ignoreCase = true) == true ||
                        url?.endsWith(".webm", ignoreCase = true) == true -> {
                            // Progressive video formats
                            val mediaItem = MediaItem.Builder()
                                .setUri(Uri.parse(url))
                                .setMimeType(MimeTypes.APPLICATION_MP4)
                                .build()
                            ProgressiveMediaSource.Factory(dataSourceFactory)
                                .createMediaSource(mediaItem)
                        }
                        else -> {
                            // Try to play as progressive download
                            DefaultMediaSourceFactory(dataSourceFactory)
                                .createMediaSource(MediaItem.fromUri(url!!))
                        }
                    }

                    exoPlayer.setMediaSource(mediaSource)
                    exoPlayer.seekTo(playbackPosition)
                    exoPlayer.playWhenReady = true
                    exoPlayer.prepare()

                    exoPlayer.addListener(object : Player.Listener {
                        override fun onIsLoadingChanged(isLoading: Boolean) {
                            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            showError(error.message ?: getString(R.string.error_playback))
                            errorTextView.visibility = View.VISIBLE
                            playerView.visibility = View.GONE
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    if (!isPlayerReady) {
                                        isPlayerReady = true
                                        progressBar.visibility = View.GONE
                                    }
                                }
                                Player.STATE_BUFFERING -> {
                                    progressBar.visibility = View.VISIBLE
                                }
                                Player.STATE_ENDED -> {
                                    progressBar.visibility = View.GONE
                                }
                                Player.STATE_IDLE -> {
                                    progressBar.visibility = View.GONE
                                }
                            }
                        }
                    })

                    playerView.setControllerVisibilityListener { visibility ->
                        if (visibility == View.VISIBLE) {
                            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                        } else {
                            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("Cleartext HTTP traffic", ignoreCase = true) == true -> 
                    "HTTP security error. Please use HTTPS URLs or contact support."
                else -> e.message ?: getString(R.string.error_playback)
            }
            showError(errorMessage)
            finish()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun lockScreen(lock: Boolean) {
        linearLayoutControlUp.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        linearLayoutControlBottom.visibility = if (lock) View.INVISIBLE else View.VISIBLE
    }

    private fun setupLockScreen() {
        imageViewLock.setOnClickListener {
            isLock = !isLock
            imageViewLock.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isLock) R.drawable.ic_baseline_lock else R.drawable.ic_baseline_lock_open
                )
            )
            lockScreen(isLock)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupFullScreen() {
        imageViewFullScreen.setOnClickListener {
            isFullScreen = !isFullScreen
            imageViewFullScreen.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isFullScreen) R.drawable.ic_baseline_fullscreen_exit
                    else R.drawable.ic_baseline_fullscreen
                )
            )
            requestedOrientation = if (isFullScreen) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            supportActionBar?.hide()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            supportActionBar?.show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        player.currentPosition.let { position ->
            playbackPosition = position
            outState.putLong("playbackPosition", position)
        }
        outState.putString("URL", url)
        outState.putString("USER_AGENT", userAgent)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        playbackPosition = savedInstanceState.getLong("playbackPosition", 0L)
        url = savedInstanceState.getString("URL")
        userAgent = savedInstanceState.getString("USER_AGENT")
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            setupPlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23) {
            setupPlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        player.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            exoPlayer.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isLock) return
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            imageViewFullScreen.performClick()
        } else {
            super.onBackPressed()
        }
    }
} 