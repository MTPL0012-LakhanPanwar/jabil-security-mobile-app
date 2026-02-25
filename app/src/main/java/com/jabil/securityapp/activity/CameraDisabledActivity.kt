package com.jabil.securityapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jabil.securityapp.R
import com.jabil.securityapp.databinding.ActivityCameraDisabledBinding

class CameraDisabledActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCameraDisabledBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraDisabledBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        binding.btnScanEntry.setOnClickListener {
            // Start ScanActivity with EXIT action to scan exit QR
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("SCAN_ACTION", "EXIT")
            startActivity(intent)
        }
    }
}