package com.samyak.urlplayerbeta.screen

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
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
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.samyak.urlplayerbeta.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.util.Locale
import android.media.AudioManager
import android.content.res.Resources
import android.view.GestureDetector
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.MotionEventCompat
import com.github.vkay94.dtpv.youtube.YouTubeOverlay
import com.google.android.exoplayer2.video.VideoSize
import com.samyak.urlplayerbeta.databinding.MoreFeaturesBinding
import kotlin.math.abs
import com.samyak.urlplayerbeta.databinding.ActivityPlayerBinding
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout

class PlayerActivity : AppCompatActivity(), GestureDetector.OnGestureListener {
    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var linearLayoutControlUp: LinearLayout
    private lateinit var linearLayoutControlBottom: LinearLayout

    // Custom controller views
    private lateinit var backButton: ImageButton
    private lateinit var videoTitle: TextView
    private lateinit var moreFeaturesButton: ImageButton
    private lateinit var playPauseButton: ImageButton
    private lateinit var orientationButton: ImageButton
    private lateinit var repeatButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var fullScreenButton: ImageButton

    private var playbackPosition = 0L
    private var isPlayerReady = false
    private var isFullscreen: Boolean = false
    private var url: String? = null
    private var userAgent: String? = null
    private lateinit var trackSelector: DefaultTrackSelector
    private var currentQuality = "Auto"
    
    private data class VideoQuality(
        val height: Int,
        val width: Int,
        val bitrate: Int,
        val label: String,
        val description: String
    )

    private val availableQualities = listOf(
        VideoQuality(1080, 1920, 8_000_000, "1080p", "Full HD - Best quality"),
        VideoQuality(720, 1280, 5_000_000, "720p", "HD - High quality"),
        VideoQuality(480, 854, 2_500_000, "480p", "SD - Good quality"),
        VideoQuality(360, 640, 1_500_000, "360p", "SD - Normal quality"),
        VideoQuality(240, 426, 800_000, "240p", "Low - Basic quality"),
        VideoQuality(144, 256, 500_000, "144p", "Very Low - Minimal quality")
    )

    private var isManualQualityControl = false

    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var minSwipeY: Float = 0f
    private var brightness: Int = 0
    private var volume: Int = 0
    private var audioManager: AudioManager? = null

    private var isLocked = false

    companion object {
        private const val INCREMENT_MILLIS = 5000L
        var pipStatus: Int = 0
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        url = intent.getStringExtra("URL")
        userAgent = intent.getStringExtra("USER_AGENT")

        if (url == null) {
            finish()
            return
        }

        // Initialize views first
        initializeViews()

        // Initialize gesture and audio controls
        gestureDetectorCompat = GestureDetectorCompat(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0

        setupPlayer()
        setupGestureControls()
    }

    private fun initializeViews() {
        // Initialize main views from binding
        playerView = binding.playerView
        progressBar = binding.progressBar
        errorTextView = binding.errorTextView
        linearLayoutControlUp = binding.linearLayoutControlUp
        linearLayoutControlBottom = binding.linearLayoutControlBottom

        // Setup player first
        setupPlayer()

        // Then initialize custom controller views and actions
        setupCustomControllerViews()
        setupCustomControllerActions()
    }

    private fun setupCustomControllerViews() {
        try {
            // Find all controller views from playerView
            backButton = playerView.findViewById(R.id.backBtn)
            videoTitle = playerView.findViewById(R.id.videoTitle)
            moreFeaturesButton = playerView.findViewById(R.id.moreFeaturesBtn)
            playPauseButton = playerView.findViewById(R.id.playPauseBtn)
            orientationButton = playerView.findViewById(R.id.orientationBtn)
            repeatButton = playerView.findViewById(R.id.repeatBtn)
            prevButton = playerView.findViewById(R.id.prevBtn)
            nextButton = playerView.findViewById(R.id.nextBtn)
            fullScreenButton = playerView.findViewById(R.id.fullScreenBtn)

            // Set initial title
            val channelName = intent.getStringExtra("CHANNEL_NAME") ?: getString(R.string.video_name)
            videoTitle.text = channelName
            videoTitle.isSelected = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error setting up controller views", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCustomControllerActions() {
        // Back button
        backButton.setOnClickListener {
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
            if (isFullscreen) {
                isFullscreen = false
                playInFullscreen(enable = false)
            } else {
                isFullscreen = true
                playInFullscreen(enable = true)
            }
        }

        // More Features button
        moreFeaturesButton.setOnClickListener {
            pauseVideo()
            val customDialog = LayoutInflater.from(this)
                .inflate(R.layout.more_features, null, false)
            val bindingMF = MoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(this)
                .setView(customDialog)
                .setOnCancelListener { playVideo() }
                .setBackground(ColorDrawable(0x803700B3.toInt()))
                .create()
            dialog.show()

            // Add subtitle button click listener
            bindingMF.subtitlesBtn.setOnClickListener {
                dialog.dismiss()
                playVideo()
                val subtitles = ArrayList<String>()
                val subtitlesList = ArrayList<String>()
                var hasSubtitles = false
                
                // Get available subtitle tracks
                try {
                    for (group in player.currentTracksInfo.trackGroupInfos) {
                        if (group.trackType == C.TRACK_TYPE_TEXT) {
                            hasSubtitles = true
                            val groupInfo = group.trackGroup
                            for (i in 0 until groupInfo.length) {
                                val format = groupInfo.getFormat(i)
                                val language = format.language ?: "unknown"
                                val label = format.label ?: Locale(language).displayLanguage
                                
                                subtitles.add(language)
                                subtitlesList.add(
                                    "${subtitlesList.size + 1}. $label" + 
                                    if (language != "unknown") " (${Locale(language).displayLanguage})" else ""
                                )
                            }
                        }
                    }

                    if (!hasSubtitles) {
                        Toast.makeText(this, "No subtitles available for this video", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val tempTracks = subtitlesList.toArray(arrayOfNulls<CharSequence>(subtitlesList.size))
                    
                    MaterialAlertDialogBuilder(this, R.style.SubtitleDialogStyle)
                        .setTitle("Select Subtitles")
                        .setOnCancelListener { playVideo() }
                        .setPositiveButton("Off Subtitles") { self, _ ->
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                            )
                            self.dismiss()
                            playVideo()
                            Snackbar.make(playerView, "Subtitles disabled", 3000).show()
                        }
                        .setItems(tempTracks) { _, position ->
                            try {
                                trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                        .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setPreferredTextLanguage(subtitles[position])
                                )
                                Snackbar.make(
                                    playerView,
                                    "Selected: ${subtitlesList[position]}", 
                                    3000
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(this, "Error selecting subtitles", Toast.LENGTH_SHORT).show()
                            }
                            playVideo()
                        }
                        .setBackground(ColorDrawable(0x803700B3.toInt()))
                        .create()
                        .apply {
                            show()
                            getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(Color.WHITE)
                        }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading subtitles", Toast.LENGTH_SHORT).show()
                }
            }

            // Video Quality button in more features dialog
            bindingMF.videoQuality.setOnClickListener {
                dialog.dismiss()
                showQualityDialog()
            }
        }

        // Lock button
        binding.lockButton.setOnClickListener {
            isLocked = !isLocked
            lockScreen(isLocked)
            binding.lockButton.setImageResource(
                if (isLocked) R.drawable.close_lock_icon 
                else R.drawable.lock_open_icon
            )
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

    private fun playInFullscreen(enable: Boolean) {
        if (enable) {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            fullScreenButton.setImageResource(R.drawable.fullscreen_exit_icon)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            fullScreenButton.setImageResource(R.drawable.fullscreen_icon)
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

    private fun showQualityDialog() {
        val qualities = getAvailableQualities()
        val qualityItems = buildQualityItems(qualities)
        val currentIndex = (qualityItems.indexOfFirst { it.contains(currentQuality) }).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this, R.style.QualityDialogStyle)
            .setTitle(getString(R.string.select_quality))
            .setSingleChoiceItems(qualityItems.toTypedArray(), currentIndex) { dialog, which ->
                val selectedQuality = if (which == 0) "Auto" else qualities[which - 1].label
                isManualQualityControl = selectedQuality != "Auto"
                applyQuality(selectedQuality, qualities)
                dialog.dismiss()

                Toast.makeText(
                    this,
                    if (selectedQuality == "Auto") {
                        getString(R.string.auto_quality_enabled)
                    } else {
                        getString(R.string.quality_changed, selectedQuality)
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .create()
            .show()
    }

    private fun buildQualityItems(qualities: List<VideoQuality>): List<String> {
        val items = mutableListOf("Auto (Recommended)")
        
        qualities.forEach { quality ->
            val currentFormat = player.videoFormat
            val isCurrent = when {
                currentFormat == null -> false
                !isManualQualityControl -> currentFormat.height == quality.height
                else -> currentQuality == quality.label
            }
            
            val qualityText = buildString {
                append(quality.label)
                append(" - ")
                append(quality.description)
                if (isCurrent) append(" ✓")
            }
            items.add(qualityText)
        }
        
        return items
    }

    private fun getAvailableQualities(): List<VideoQuality> {
        val tracks = mutableListOf<VideoQuality>()
        
        try {
            player.currentTrackGroups.let { trackGroups ->
                for (groupIndex in 0 until trackGroups.length) {
                    val group = trackGroups[groupIndex]
                    
                    for (trackIndex in 0 until group.length) {
                        val format = group.getFormat(trackIndex)
                        
                        if (format.height > 0 && format.width > 0) {
                            availableQualities.find { 
                                it.height == format.height 
                            }?.let { tracks.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return tracks.distinct().sortedByDescending { it.height }
    }

    private fun applyQuality(quality: String, availableTracks: List<VideoQuality>) {
        val parameters = trackSelector.buildUponParameters()

        when (quality) {
            "Auto" -> {
                parameters.clearVideoSizeConstraints()
                    .setForceHighestSupportedBitrate(false)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
            }
            else -> {
                availableTracks.find { it.label == quality }?.let { track ->
                    parameters.setMaxVideoSize(track.width, track.height)
                        .setMinVideoSize(track.width/2, track.height/2)
                        .setMaxVideoBitrate(track.bitrate)
                        .setMinVideoBitrate(track.bitrate/2)
                        .setForceHighestSupportedBitrate(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(false)
                }
            }
        }

        try {
            val position = player.currentPosition
            val wasPlaying = player.isPlaying

            trackSelector.setParameters(parameters)
            currentQuality = quality

            // Save preferences
            getSharedPreferences("player_settings", Context.MODE_PRIVATE).edit().apply {
                putString("preferred_quality", quality)
                putBoolean("manual_quality_control", isManualQualityControl)
                apply()
            }

            // Restore playback state
            player.seekTo(position)
            player.playWhenReady = wasPlaying

        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.quality_change_failed, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initializeQuality() {
        val prefs = getSharedPreferences("player_settings", Context.MODE_PRIVATE)
        val savedQuality = prefs.getString("preferred_quality", "Auto") ?: "Auto"
        isManualQualityControl = prefs.getBoolean("manual_quality_control", false)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    getAvailableQualities().let { tracks ->
                        if (tracks.isNotEmpty()) {
                            // If manual control is off, use Auto
                            val qualityToApply = if (isManualQualityControl) savedQuality else "Auto"
                            applyQuality(qualityToApply, tracks)
                            player.removeListener(this)
                        }
                    }
                }
            }
        })
    }

    private fun getCurrentQualityInfo(): String {
        val currentTrack = player.videoFormat
        return when {
            currentTrack == null -> "Unknown"
            !isManualQualityControl -> "Auto (${currentTrack.height}p)"
            else -> currentQuality
        }
    }

    private fun setupPlayer() {
        if (::player.isInitialized) {
            player.release()
        }

        trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .setSeekBackIncrementMs(INCREMENT_MILLIS)
            .setSeekForwardIncrementMs(INCREMENT_MILLIS)
            .build()

        // Set player to playerView
        playerView.player = player

        // Setup media source
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent ?: Util.getUserAgent(this, "URLPlayerBeta"))

        val mediaItem = MediaItem.fromUri(Uri.parse(url))
        val mediaSource = when {
            url?.endsWith(".m3u8", ignoreCase = true) == true -> {
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

        player.setMediaSource(mediaSource)
        player.seekTo(playbackPosition)
        player.playWhenReady = true
        player.prepare()

        setupPlayerListeners()
    }

    private fun setupPlayerListeners() {
        player.addListener(object : Player.Listener {
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

            override fun onPlayerError(error: PlaybackException) {
                errorTextView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
            }
        })
    }

    private fun updateQualityInfo() {
        videoTitle.text = getCurrentQualityInfo()
    }

    private fun lockScreen(lock: Boolean) {
        linearLayoutControlUp.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        linearLayoutControlBottom.visibility = if (lock) View.INVISIBLE else View.VISIBLE
        playerView.useController = !lock
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
        if (audioManager == null) {
            audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        audioManager?.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        if (brightness != 0) setScreenBrightness(brightness)
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
        audioManager?.abandonAudioFocus(null)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureControls() {
        binding.playerView.player = player
        
        // Setup YouTube style overlay
        binding.ytOverlay.performListener(object : YouTubeOverlay.PerformListener {
            override fun onAnimationEnd() {
                binding.ytOverlay.visibility = View.GONE
            }

            override fun onAnimationStart() {
                binding.ytOverlay.visibility = View.VISIBLE
            }
        })
        binding.ytOverlay.player(player)

        // Handle touch events
        binding.playerView.setOnTouchListener { _, motionEvent ->
            if (!isLocked) {
                gestureDetectorCompat.onTouchEvent(motionEvent)
                
                if (motionEvent.action == MotionEvent.ACTION_UP) {
                    binding.brightnessIcon.visibility = View.GONE
                    binding.volumeIcon.visibility = View.GONE
                    
                    // For immersive mode
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, binding.root).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = 
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
            false
        }
    }

    override fun onScroll(
        e1: MotionEvent?,
        event: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (isLocked) return false
        
        minSwipeY += distanceY

        val sWidth = Resources.getSystem().displayMetrics.widthPixels
        val sHeight = Resources.getSystem().displayMetrics.heightPixels

        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()
        if (event.x < border || event.y < border || 
            event.x > sWidth - border || event.y > sHeight - border)
            return false

        if (abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50) {
            if (event.x < sWidth / 2) {
                // Brightness control
                binding.brightnessIcon.visibility = View.VISIBLE
                binding.volumeIcon.visibility = View.GONE
                val increase = distanceY > 0
                val newValue = if (increase) brightness + 1 else brightness - 1
                if (newValue in 0..30) brightness = newValue
                binding.brightnessIcon.text = brightness.toString()
                setScreenBrightness(brightness)
            } else {
                // Volume control
                binding.brightnessIcon.visibility = View.GONE
                binding.volumeIcon.visibility = View.VISIBLE
                val maxVolume = audioManager!!.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue = if (increase) volume + 1 else volume - 1
                if (newValue in 0..maxVolume) volume = newValue
                binding.volumeIcon.text = volume.toString()
                audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
            minSwipeY = 0f
        }
        return true
    }

    private fun setScreenBrightness(value: Int) {
        val d = 1.0f / 30
        val lp = window.attributes
        lp.screenBrightness = d * value
        window.attributes = lp
    }

    // Add other required GestureDetector.OnGestureListener methods
    override fun onDown(e: MotionEvent) = false
    override fun onShowPress(e: MotionEvent) = Unit
    override fun onSingleTapUp(e: MotionEvent) = false
    override fun onLongPress(e: MotionEvent) = Unit
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float) = false
}