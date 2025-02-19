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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
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
        player = ExoPlayer.Builder(this)
            .setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS)
            .build()
            .also { exoPlayer ->
                playerView.player = exoPlayer

                val dataSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(userAgent ?: Util.getUserAgent(this, "URLPlayerBeta"))

                val mediaItem = MediaItem.fromUri(Uri.parse(url))
                
                // Create appropriate media source based on URL type
                val mediaSource = when {
                    url?.endsWith(".m3u8", ignoreCase = true) == true -> {
                        // HLS stream
                        HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
                    }
                    url?.endsWith(".mp4", ignoreCase = true) == true -> {
                        // MP4 video
                        val mp4MediaItem = MediaItem.Builder()
                            .setUri(Uri.parse(url))
                            .setMimeType(MimeTypes.APPLICATION_MP4)
                            .build()
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mp4MediaItem)
                    }
                    else -> {
                        // Try to play as progressive download
                        ProgressiveMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(mediaItem)
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
                        errorTextView.visibility = View.VISIBLE
                        playerView.visibility = View.GONE
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY && !isPlayerReady) {
                            isPlayerReady = true
                            progressBar.visibility = View.GONE
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
        playbackPosition = player.currentPosition
        outState.putLong("playbackPosition", playbackPosition)
        outState.putString("URL", url)
        outState.putString("USER_AGENT", userAgent)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            player.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || !isPlayerReady) {
            player.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            playbackPosition = player.currentPosition
            player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
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