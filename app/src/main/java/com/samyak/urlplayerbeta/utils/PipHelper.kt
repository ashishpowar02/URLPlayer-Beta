package com.samyak.urlplayerbeta.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.util.Rational
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.samyak.urlplayerbeta.R
import android.app.PendingIntent
import android.view.View
import android.view.WindowManager

/**
 * Helper class for managing Picture-in-Picture mode in modern Android versions
 * Handles compatibility across Android 8.0 (O) through Android 14+ (UPSIDE_DOWN_CAKE)
 */
class PipHelper(private val activity: Activity) {

    private val TAG = "PipHelper"

    // Action constants
    companion object {
        const val PIP_ACTION_PLAY = "com.samyak.urlplayerbeta.PIP_PLAY"
        const val PIP_ACTION_PAUSE = "com.samyak.urlplayerbeta.PIP_PAUSE"
        const val PIP_ACTION_CLOSE = "com.samyak.urlplayerbeta.PIP_CLOSE"
        const val PIP_CONTROL_TYPE_PLAY = 1
        const val PIP_CONTROL_TYPE_PAUSE = 2
        const val PIP_CONTROL_TYPE_CLOSE = 3

        // Check if the device supports PiP
        fun isPipSupported(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.packageManager.hasSystemFeature(
                    PackageManager.FEATURE_PICTURE_IN_PICTURE
                ) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            } else false
        }

        // Request battery optimization exemption (needed for Android 14+ PiP background playback)
        fun requestBatteryOptimizationExemption(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    val packageName = context.packageName

                    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = Uri.parse("package:$packageName")
                        }
                        context.startActivity(intent)
                    }
                }
            } catch (e: Exception) {
                Log.e("PipHelper", "Error requesting battery exemption: ${e.message}")
            }
        }
    }

    // Check if device is in fullscreen mode
    private fun isFullscreenMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val windowInsets = activity.window.decorView.rootWindowInsets
            windowInsets?.displayCutout != null ||
                    (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
        } else {
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
        }
    }

    // Create PiP params with proper aspect ratio and actions
    @RequiresApi(Build.VERSION_CODES.O)
    fun createPipParams(
        videoWidth: Int?,
        videoHeight: Int?,
        isPlaying: Boolean,
        videoView: View? = null
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()

        // Calculate aspect ratio
        val videoRatio = if (videoWidth != null && videoHeight != null && videoWidth > 0 && videoHeight > 0) {
            Rational(videoWidth, videoHeight)
        } else {
            Rational(16, 9) // Default aspect ratio
        }

        // Set the aspect ratio
        builder.setAspectRatio(videoRatio)

        // Add actions for Android 9+ (API 28+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val actions = createPipActions(isPlaying)
            if (actions.isNotEmpty()) {
                builder.setActions(actions)
            }
        }

        // Add source rect hint for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val sourceRectHint = Rect()

                // If we have a specific video view, use its dimensions
                if (videoView != null && videoView.width > 0 && videoView.height > 0) {
                    // Get the video view bounds
                    val locationInWindow = IntArray(2)
                    videoView.getLocationInWindow(locationInWindow)

                    sourceRectHint.set(
                        locationInWindow[0],
                        locationInWindow[1],
                        locationInWindow[0] + videoView.width,
                        locationInWindow[1] + videoView.height
                    )
                } else {
                    // Use the entire window for fullscreen content
                    if (isFullscreenMode()) {
                        // In fullscreen, use the entire screen area
                        activity.window.decorView.getGlobalVisibleRect(sourceRectHint)
                    } else {
                        // For non-fullscreen, get the content area
                        val contentView = activity.findViewById<View>(android.R.id.content)
                        contentView.getGlobalVisibleRect(sourceRectHint)
                    }
                }

                // Apply the source rect hint
                if (!sourceRectHint.isEmpty) {
                    builder.setSourceRectHint(sourceRectHint)
                    Log.d(TAG, "SourceRectHint: $sourceRectHint")
                }

                // Additional Android 12+ features
                builder.setSeamlessResizeEnabled(true)
                builder.setAutoEnterEnabled(false)
            } catch (e: Exception) {
                Log.e(TAG, "Error setting Android 12+ PiP features: ${e.message}")
            }
        }

        // Add Android 13+ (API 33, TIRAMISU) features
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                builder.setExpandedAspectRatio(Rational(16, 9))
            } catch (e: Exception) {
                Log.e(TAG, "Error setting expanded aspect ratio: ${e.message}")
            }
        }

        // Add Android 14+ (API 34, UPSIDE_DOWN_CAKE) features
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                // Remove the setStableAspectRatio line as it's not supported
                // Any additional Android 14+ PiP features can be added here
            } catch (e: Exception) {
                Log.e(TAG, "Error setting Android 14+ PiP features: ${e.message}")
            }
        }

        return builder.build()
    }

    // Create PiP control actions (play/pause buttons)
    @RequiresApi(Build.VERSION_CODES.P)
    private fun createPipActions(isPlaying: Boolean): List<RemoteAction> {
        val actions = ArrayList<RemoteAction>()

        try {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            // Add play/pause action based on current state
            if (isPlaying) {
                // Add pause action
                val pauseIntent = PendingIntent.getBroadcast(
                    activity,
                    PIP_CONTROL_TYPE_PAUSE,
                    Intent(PIP_ACTION_PAUSE).setPackage(activity.packageName),
                    flags
                )

                val pauseIcon = Icon.createWithResource(activity, R.drawable.pause_icon)
                val pauseAction = RemoteAction(
                    pauseIcon,
                    "Pause",
                    "Pause playback",
                    pauseIntent
                )
                actions.add(pauseAction)
            } else {
                // Add play action
                val playIntent = PendingIntent.getBroadcast(
                    activity,
                    PIP_CONTROL_TYPE_PLAY,
                    Intent(PIP_ACTION_PLAY).setPackage(activity.packageName),
                    flags
                )

                val playIcon = Icon.createWithResource(activity, R.drawable.play_icon)
                val playAction = RemoteAction(
                    playIcon,
                    "Play",
                    "Play video",
                    playIntent
                )
                actions.add(playAction)
            }

            // Add close action - many users expect this
            val closeIntent = PendingIntent.getBroadcast(
                activity,
                PIP_CONTROL_TYPE_CLOSE,
                Intent(PIP_ACTION_CLOSE).setPackage(activity.packageName),
                flags
            )

            val closeIcon = Icon.createWithResource(activity, android.R.drawable.ic_menu_close_clear_cancel)
            val closeAction = RemoteAction(
                closeIcon,
                "Close",
                "Close player",
                closeIntent
            )
            actions.add(closeAction)

        } catch (e: Exception) {
            Log.e(TAG, "Error creating PiP actions: ${e.message}")
        }

        return actions
    }

    // Enter PiP mode with proper error handling
    fun enterPipMode(
        videoWidth: Int?,
        videoHeight: Int?,
        isPlaying: Boolean,
        videoView: View? = null
    ): Boolean {
        if (!isPipSupported(activity)) {
            Toast.makeText(activity, "PiP mode not supported on this device", Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // For fullscreen mode, prepare the UI first
                if (isFullscreenMode()) {
                    // Make sure system UI is visible briefly to improve PiP transition
                    activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }

                val params = createPipParams(videoWidth, videoHeight, isPlaying, videoView)

                // Ensure we have necessary permissions for Android 13+
                if (Build.VERSION.SDK_INT >= 33) {
                    ensureModernAndroidPermissions()
                }

                // Enter PiP
                activity.enterPictureInPictureMode(params)
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Error entering PiP mode: ${e.message}")
                Toast.makeText(activity, "Failed to enter PiP mode", Toast.LENGTH_SHORT).show()
                return false
            }
        } else {
            Toast.makeText(activity, "PiP mode requires Android 8.0 or higher", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    // Make sure we have necessary modern Android permissions
    private fun ensureModernAndroidPermissions() {
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            val notificationPermission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(activity, notificationPermission) !=
                PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(notificationPermission), 100)
            }
        }

        // Request battery optimization exemption for Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            requestBatteryOptimizationExemption(activity)
        }
    }

    // Update PiP parameters (e.g., when play/pause state changes)
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePipParams(
        videoWidth: Int?,
        videoHeight: Int?,
        isPlaying: Boolean,
        videoView: View? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = createPipParams(videoWidth, videoHeight, isPlaying, videoView)
                activity.setPictureInPictureParams(params)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating PiP params: ${e.message}")
            }
        }
    }

    // Add to process importance information for Android 14+ debugging
    fun getProcessImportanceInfo(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val importance = activityManager.runningAppProcesses?.find {
                    it.pid == android.os.Process.myPid()
                }?.importance ?: -1

                return when (importance) {
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "Foreground Service"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "Perceptible"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Service"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> "Top Sleeping"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "Can't Save State"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "Cached"
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "Gone"
                    else -> "Unknown ($importance)"
                }
            } catch (e: Exception) {
                return "Error: ${e.message}"
            }
        }
        return "Not applicable (pre-O)"
    }

    // Fix for Android 14 PiP close issue (handle cleanup when PiP is closed)
    @RequiresApi(Build.VERSION_CODES.O)
    fun fixPipCloseIssue(onClose: () -> Unit) {
        if (Build.VERSION.SDK_INT >= 34) { // Android 14 (UPSIDE_DOWN_CAKE)
            // We'll check PiP state periodically
            val handler = Handler(Looper.getMainLooper())

            var wasInPipMode = false

            val pipCheckRunnable = object : Runnable {
                override fun run() {
                    val isInPipNow = activity.isInPictureInPictureMode

                    // Only call onClose if we were in PIP mode and now we're not,
                    // AND the activity is being finished (actually closing)
                    if (wasInPipMode && !isInPipNow && activity.isFinishing) {
                        // PiP mode actually ended and we're closing
                        onClose()
                    } else if (!isInPipNow && wasInPipMode) {
                        // We're transitioning from PIP to fullscreen
                        // Don't call onClose here - just update state
                        Log.d(TAG, "Transitioning from PIP to fullscreen")
                    }

                    // Update our state for next check
                    wasInPipMode = isInPipNow

                    // Only continue checking if activity is still alive
                    if (!activity.isDestroyed && !activity.isFinishing) {
                        handler.postDelayed(this, 500) // Check more frequently
                    }
                }
            }

            // Start checking
            handler.postDelayed(pipCheckRunnable, 500)
        }
    }
}