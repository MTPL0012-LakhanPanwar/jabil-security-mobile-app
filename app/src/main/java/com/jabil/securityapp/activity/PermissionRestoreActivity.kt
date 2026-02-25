package com.jabil.securityapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jabil.securityapp.R
import com.jabil.securityapp.databinding.ActivityPermissionRestoreBinding
import com.jabil.securityapp.manager.DeviceAdminManager

class PermissionRestoreActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPermissionRestoreBinding
    private lateinit var deviceAdminManager: DeviceAdminManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceAdminManager = DeviceAdminManager(this)
        // Deactivate device admin
        if (deviceAdminManager.isDeviceAdminActive()) {
            deviceAdminManager.removeDeviceAdmin()
        }

        binding.btnContinue.setOnClickListener {
            // Navigate to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}