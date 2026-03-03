package com.jabil.securityapp.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jabil.securityapp.CameraBlockerService
import com.jabil.securityapp.R
import com.jabil.securityapp.databinding.ActivityCameraDisabledBinding
import com.jabil.securityapp.utils.DeviceUtils
import com.jabil.securityapp.utils.PrefsManager
import com.jabil.securityapp.utils.getTimeFormat

class CameraDisabledActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCameraDisabledBinding
    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraDisabledBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefsManager = PrefsManager(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initFields()
        initClickListeners()
    }

    fun isKioskModeActive(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Returns LOCK_TASK_MODE_LOCKED (Device Owner)
            // or LOCK_TASK_MODE_PINNED (Standard App/Screen Pinning)
            val state = activityManager.lockTaskModeState
            state != ActivityManager.LOCK_TASK_MODE_NONE
        } else {
            // Deprecated but still works for older devices
            activityManager.isInLockTaskMode
        }
    }

    private fun initClickListeners() {
        binding.btnScanEntry.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("SCAN_ACTION", "EXIT")
            startActivity(intent)
        }

        // This back press dispatcher is implemented for suppressed the toast message,
        // which is displayed when user press back while kiosk mode is activated
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isKioskModeActive()) {
                    finish()
                }
            }
        })
    }

    private fun initFields() {
        binding.iToolbar.toolbarTitle.text = "Active Session"
        binding.iToolbar.btnBack.visibility = View.GONE
        binding.tvEntryTime.text = getTimeFormat(this, prefsManager.entryTime)
        if (DeviceUtils.isTargetedXiaomiVersion()) {
            // Apply the "Lock" to prevent 'Clear All' button access
            try {
                // Note: If NOT Device Owner, this triggers standard Screen Pinning
                startLockTask()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val serviceIntent = Intent(this, CameraBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

}