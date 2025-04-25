package com.samyak.urlplayerbeta.screen

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
        setupCoffeeButton()
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
    
    private fun setupCoffeeButton() {
        binding.coffeeButton.setOnClickListener {
            val coffeeUrl = getString(R.string.coffee_url)
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(coffeeUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.cannot_open_browser), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 