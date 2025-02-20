package com.samyak.urlplayerbeta.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import com.samyak.urlplayerbeta.databinding.MoreFeaturesBinding

class PlayerActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout

    // Custom controller views
    private lateinit var playPauseButton: ImageButton
    private lateinit var videoTitleText: TextView
    private lateinit var moreFeaturesButton: ImageButton
    private lateinit var orientationButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fullScreenButton: ImageButton

    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullScreen = false
    private var url: String? = null
    private var userAgent: String? = null

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        var pipStatus: Int = 0
        private var volume: Int = 0
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        url = intent.getStringExtra("URL")
        userAgent = intent.getStringExtra("USER_AGENT")

        if (url == null) {
            finish()
            return
        }

        initializeViews()
        setupPlayer()
    }

    private fun initializeViews() {
        playerView = findViewById(R.id.playerView)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        linearLayoutControlUp = findViewById(R.id.linearLayoutControlUp)
        linearLayoutControlBottom = findViewById(R.id.linearLayoutControlBottom)

        setupCustomControllerViews()
        setupCustomControllerActions()
    }

    private fun setupCustomControllerViews() {
        // Find all controller views
        playPauseButton = playerView.findViewById(R.id.playPauseBtn)
        videoTitleText = playerView.findViewById(R.id.videoTitle)
        moreFeaturesButton = playerView.findViewById(R.id.moreFeaturesBtn)
        orientationButton = playerView.findViewById(R.id.orientationBtn)
        repeatButton = playerView.findViewById(R.id.repeatBtn)
        prevButton = playerView.findViewById(R.id.prevBtn)
        nextButton = playerView.findViewById(R.id.nextBtn)
        fullScreenButton = playerView.findViewById(R.id.fullScreenBtn)

        // Set initial video title
        videoTitleText.text = intent.getStringExtra("TITLE") ?: getString(R.string.video_name)
        videoTitleText.isSelected = true // Enable marquee
    }

    private fun setupCustomControllerActions() {
        // Back button
        playerView.findViewById<ImageButton>(R.id.backButton)?.setOnClickListener {
            onBackPressed()
        }

        // Play/Pause button
        playPauseButton.setOnClickListener {
            if (player.isPlaying) {
                player.pause()
                playPauseButton.setImageResource(R.drawable.play_icon)
            } else {
                player.play()
                playPauseButton.setImageResource(R.drawable.pause_icon)
            }
        }

        // Previous/Next buttons (10 seconds skip)
        prevButton.setOnClickListener {
            player.seekTo(maxOf(0, player.currentPosition - 10000))
        }

        nextButton.setOnClickListener {
            player.seekTo(minOf(player.duration, player.currentPosition + 10000))
        }

        // Repeat button
        repeatButton.setOnClickListener {
            when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    repeatButton.setImageResource(R.drawable.repeat_one_icon)
                }
                Player.REPEAT_MODE_ONE -> {
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    repeatButton.setImageResource(R.drawable.repeat_all_icon)
                }
                else -> {
                    player.repeatMode = Player.REPEAT_MODE_OFF
                    repeatButton.setImageResource(R.drawable.repeat_off_icon)
                }
            }
        }

        // Orientation button
        orientationButton.setOnClickListener {
            toggleOrientation()
        }

        // Fullscreen button
        fullScreenButton.setOnClickListener {
            toggleFullscreen()
        }

        // More Features button
        moreFeaturesButton.setOnClickListener {
            pauseVideo() // Pause video when dialog opens
            val customDialog = LayoutInflater.from(this)
                .inflate(R.layout.more_features, null, false) // Changed parent to null
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(customDialog)
                .setOnCancelListener { playVideo() } // Resume video when dialog is cancelled
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialog.show()

            // Handle PiP Mode button click
            bindingMF.pipModeBtn.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                    val status = appOps.checkOpNoThrow(
                        AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                        android.os.Process.myUid(),
                        packageName
                    ) == AppOpsManager.MODE_ALLOWED

                    if (status) {
                        enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                        dialog.dismiss()
                        playerView.hideController()
                        playVideo() // Resume video in PiP mode
                        pipStatus = 0
                    } else {
                        // Open PiP settings if permission not granted
                        val intent = Intent(
                            "android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(this, "Feature Not Supported!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    playVideo() // Resume video if feature not supported
                }
            }
        }
    }

    // Add helper methods for video control
    private fun playVideo() {
        player.play()
        playPauseButton.setImageResource(R.drawable.pause_icon)
    }

    private fun pauseVideo() {
        player.pause()
        playPauseButton.setImageResource(R.drawable.play_icon)
    }

    private fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun toggleFullscreen() {
        isFullScreen = !isFullScreen
        fullScreenButton.setImageResource(
            if (isFullScreen) R.drawable.fullscreen_exit_icon
            else R.drawable.fullscreen_icon
        )
        if (isFullScreen) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    private fun showSpeedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.speed_dialog, null)
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
            .setView(dialogView)
            .create()

        var currentSpeed = player.playbackParameters.speed
        val speedText = dialogView.findViewById<TextView>(R.id.speedText)
        speedText.text = String.format("%.1fx", currentSpeed)

        dialogView.findViewById<ImageButton>(R.id.minusBtn).setOnClickListener {
            if (currentSpeed > 0.25f) {
                currentSpeed -= 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialogView.findViewById<ImageButton>(R.id.plusBtn).setOnClickListener {
            if (currentSpeed < 3.0f) {
                currentSpeed += 0.25f
                speedText.text = String.format("%.1fx", currentSpeed)
                player.setPlaybackSpeed(currentSpeed)
            }
        }

        dialog.show()
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

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playPauseButton.setImageResource(
                            if (isPlaying) R.drawable.pause_icon
                            else R.drawable.play_icon
                        )
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
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            super.onBackPressed()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (pipStatus != 0) {
            finish()
            val intent = Intent(this, PlayerActivity::class.java)
            when (pipStatus) {
                1 -> intent.putExtra("class", "FolderActivity")
                2 -> intent.putExtra("class", "SearchedVideos")
                3 -> intent.putExtra("class", "AllVideos")
            }
            startActivity(intent)
        }
        if (!isInPictureInPictureMode) {
            pauseVideo() // Pause video when exiting PiP mode
        }
    }
}