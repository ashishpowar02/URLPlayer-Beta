package com.samyak.urlplayerbeta.utils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.samyak.urlplayerbeta.R

/**
 * AppRatingManager handles the app rating flow using Google Play's In-App Review API
 * for a seamless user experience matching the Google Play rating flow
 */
object AppRatingManager {
    private const val PREF_NAME = "app_rating_prefs"
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_FIRST_LAUNCH_TIME = "first_launch_time"
    private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    private const val KEY_DONT_SHOW_AGAIN = "dont_show_again"
    private const val KEY_RATED = "rated_on_store"
    
    // Configuration parameters
    private const val DAYS_UNTIL_PROMPT = 3 // Minimum days before showing the dialog
    private const val LAUNCHES_UNTIL_PROMPT = 5 // Minimum launches before showing the dialog
    private const val DAYS_BETWEEN_PROMPTS = 7 // Days between prompts if user chooses "Maybe Later"
    
    /**
     * Call this method at app launch to track app usage
     */
    fun trackAppLaunch(context: Context) {
        val prefs = getPreferences(context)
        
        // If user has rated or doesn't want to see again
        if (prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false) || 
            prefs.getBoolean(KEY_RATED, false)) {
            return
        }
        
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, 0)
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        
        // First launch - save current time
        if (firstLaunchTime == 0L) {
            prefs.edit().putLong(KEY_FIRST_LAUNCH_TIME, System.currentTimeMillis()).apply()
        }
        
        // Increment launch counter
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launchCount + 1).apply()
        
        // Check if we should show the rating dialog
        checkAndShowRatingDialogIfNeeded(context)
    }
    
    /**
     * Call this after user completes a significant action to show a contextual rating prompt
     */
    fun showRatingDialogAfterSignificantEvent(activity: Activity) {
        val prefs = getPreferences(activity)
        
        // Don't show if user has rated or doesn't want to see again
        if (prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false) || 
            prefs.getBoolean(KEY_RATED, false)) {
            return
        }
        
        // Show the in-app review flow
        requestInAppReview(activity)
    }
    
    /**
     * Force show the rating dialog (e.g. from settings)
     */
    fun showRatingDialog(activity: Activity) {
        // If Google Play In-App Review is available, use it
        requestInAppReview(activity)
    }
    
    /**
     * Request the in-app review flow using Google Play's API
     */
    private fun requestInAppReview(activity: Activity) {
        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // We got the ReviewInfo object
                    val reviewInfo = task.result
                    
                    // Launch the in-app review flow
                    launchReviewFlow(activity, manager, reviewInfo)
                } else {
                    // There was some problem, fall back to the app store rating
                    showCustomRatingDialog(activity)
                }
            }
        } catch (e: Exception) {
            // Fall back to custom dialog if any exception occurs
            showCustomRatingDialog(activity)
        }
    }
    
    /**
     * Launch the in-app review flow with the provided ReviewInfo
     */
    private fun launchReviewFlow(activity: Activity, manager: ReviewManager, reviewInfo: ReviewInfo) {
        val flow = manager.launchReviewFlow(activity, reviewInfo)
        flow.addOnCompleteListener { _ ->
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown.
            
            // Mark as reviewed to prevent showing again
            val prefs = getPreferences(activity)
            prefs.edit()
                .putBoolean(KEY_RATED, true)
                .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
                .apply()
        }
    }
    
    /**
     * Fallback method to show our custom rating dialog if Google Play In-App Review is not available
     */
    private fun showCustomRatingDialog(activity: Activity) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_app_rating, null)
        val ratingDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btn_submit)
        val btnLater = dialogView.findViewById<Button>(R.id.btn_later)
        val tvNeverAsk = dialogView.findViewById<TextView>(R.id.tv_never_ask)
        
        // Handle submit button click
        btnSubmit.setOnClickListener {
            val prefs = getPreferences(activity)
            
            // Directly open Play Store for all ratings
            openPlayStore(activity)
            
            // Mark as rated
            prefs.edit().putBoolean(KEY_RATED, true).apply()
            
            // Update last prompt time
            prefs.edit().putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis()).apply()
            
            dialogView.post {
                ratingDialog.dismiss()
            }
        }
        
        // Handle "Maybe Later" button click
        btnLater.setOnClickListener {
            // Update last prompt time to postpone the next prompt
            getPreferences(activity).edit()
                .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
                .apply()
                
            dialogView.post {
                ratingDialog.dismiss()
            }
        }
        
        // Handle "Never ask again" text click
        tvNeverAsk.setOnClickListener {
            // Mark "don't show again" as true
            getPreferences(activity).edit()
                .putBoolean(KEY_DONT_SHOW_AGAIN, true)
                .apply()
                
            dialogView.post {
                ratingDialog.dismiss()
            }
        }
        
        ratingDialog.show()
    }
    
    /**
     * Open the Play Store to the app's page
     */
    private fun openPlayStore(activity: Activity) {
        val appPackageName = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, 
                Uri.parse("market://details?id=$appPackageName")))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }
    
    private fun checkAndShowRatingDialogIfNeeded(context: Context) {
        if (context !is Activity || (context as Activity).isFinishing) {
            return
        }
        
        val prefs = getPreferences(context)
        val launchCount = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val firstLaunchTime = prefs.getLong(KEY_FIRST_LAUNCH_TIME, 0)
        val lastPromptTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0)
        
        // Don't show if user marked "don't show again" or has already rated
        if (prefs.getBoolean(KEY_DONT_SHOW_AGAIN, false) || 
            prefs.getBoolean(KEY_RATED, false)) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val daysElapsedSinceFirstLaunch = (currentTime - firstLaunchTime) / (1000 * 60 * 60 * 24)
        val daysElapsedSinceLastPrompt = (currentTime - lastPromptTime) / (1000 * 60 * 60 * 24)
        
        // Show dialog if:
        // 1. App has been launched enough times AND
        // 2. At least DAYS_UNTIL_PROMPT days have passed since first launch
        // 3. If we've shown the prompt before, enough days have passed since last prompt
        val hasMetLaunchThreshold = launchCount >= LAUNCHES_UNTIL_PROMPT
        val hasMetFirstLaunchTimeThreshold = daysElapsedSinceFirstLaunch >= DAYS_UNTIL_PROMPT
        val hasMetRepromptTimeThreshold = lastPromptTime == 0L || daysElapsedSinceLastPrompt >= DAYS_BETWEEN_PROMPTS
        
        if (hasMetLaunchThreshold && hasMetFirstLaunchTimeThreshold && hasMetRepromptTimeThreshold) {
            requestInAppReview(context)
        }
    }
    
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Reset the rating preferences (for development/testing)
     */
    fun resetRatingPreferences(context: Context) {
        getPreferences(context).edit().clear().apply()
    }
    
    /**
     * Show feedback dialog - can be called independently of rating
     */
    fun showFeedbackDialog(activity: Activity) {
        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_feedback, null)
        val feedbackDialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        val btnSend = dialogView.findViewById<Button>(R.id.btn_send)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        val etFeedback = dialogView.findViewById<View>(R.id.et_feedback)
        val cbUiUx = dialogView.findViewById<CheckBox>(R.id.cb_ui_ux)
        val cbPerformance = dialogView.findViewById<CheckBox>(R.id.cb_performance)
        val cbFeatures = dialogView.findViewById<CheckBox>(R.id.cb_features)
        val cbAds = dialogView.findViewById<CheckBox>(R.id.cb_ads)
        val cbOther = dialogView.findViewById<CheckBox>(R.id.cb_other)
        
        btnSend.setOnClickListener {
            // Collect all feedback
            val feedbackItems = mutableListOf<String>()
            if (cbUiUx.isChecked) feedbackItems.add(activity.getString(R.string.feedback_ui_ux))
            if (cbPerformance.isChecked) feedbackItems.add(activity.getString(R.string.feedback_performance))
            if (cbFeatures.isChecked) feedbackItems.add(activity.getString(R.string.feedback_features))
            if (cbAds.isChecked) feedbackItems.add(activity.getString(R.string.feedback_ads))
            if (cbOther.isChecked) feedbackItems.add(activity.getString(R.string.feedback_other))
            
            val additionalFeedback = when (etFeedback) {
                is android.widget.EditText -> etFeedback.text.toString()
                is com.google.android.material.textfield.TextInputEditText -> etFeedback.text.toString()
                else -> ""
            }
            
            // Save feedback
            saveFeedback(activity, 0f, feedbackItems, additionalFeedback)
            
            // Show thank you message
            Toast.makeText(activity, R.string.feedback_sent, Toast.LENGTH_SHORT).show()
            
            feedbackDialog.dismiss()
        }
        
        btnCancel.setOnClickListener {
            // Just dismiss the dialog
            feedbackDialog.dismiss()
        }
        
        feedbackDialog.show()
    }
    
    private fun saveFeedback(
        context: Context, 
        rating: Float, 
        feedbackItems: List<String>, 
        additionalFeedback: String
    ) {
        // Here you could:
        // 1. Store feedback in local storage
        // 2. Send feedback to your backend
        // 3. Send feedback to analytics service
        
        // For this implementation, we'll just store it locally
        val prefs = getPreferences(context)
        val timestamp = System.currentTimeMillis()
        
        // Convert feedback items to comma-separated string
        val feedbackItemsStr = feedbackItems.joinToString(",")
        
        // Store feedback with timestamp as key
        prefs.edit()
            .putFloat("feedback_rating_$timestamp", rating)
            .putString("feedback_items_$timestamp", feedbackItemsStr)
            .putString("feedback_additional_$timestamp", additionalFeedback)
            .apply()
        
        // Optional: If you're using Firebase Analytics, you could send the feedback there
        // FirebaseAnalytics.getInstance(context).logEvent("app_feedback", bundleOf(
        //    "rating" to rating.toDouble(),
        //    "feedback_categories" to feedbackItemsStr,
        //    "feedback_text" to additionalFeedback
        // ))
    }
} 