package com.samyak.urlplayerbeta.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samyak.urlplayerbeta.AdManage.loadBannerAd
import com.samyak.urlplayerbeta.R
import com.samyak.urlplayerbeta.databinding.ActivityInfoBinding

class InfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bannerContainer.loadBannerAd()
        
        setupToolbar()
        setupAppInfo()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.what_is_app)
            
            binding.toolbar.navigationIcon?.setTint(
                ContextCompat.getColor(this@InfoActivity, android.R.color.white)
            )
        }
        
        binding.toolbar.setTitleTextColor(
            ContextCompat.getColor(this, android.R.color.white)
        )
    }
    
    private fun setupAppInfo() {
        // You can set the app info content here
        binding.appInfoText.text = getString(R.string.app_info_content)
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 