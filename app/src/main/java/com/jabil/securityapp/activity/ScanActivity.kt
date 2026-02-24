package com.jabil.securityapp.activity

import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.zxing.integration.android.IntentIntegrator
import com.jabil.securityapp.CameraBlockerService
import com.jabil.securityapp.R
import com.jabil.securityapp.api.RetrofitClient
import com.jabil.securityapp.api.models.DeviceInfo
import com.jabil.securityapp.api.models.ScanEntryRequest
import com.jabil.securityapp.api.models.ScanExitRequest
import com.jabil.securityapp.camera.AnyOrientationCaptureActivity
import com.jabil.securityapp.databinding.ActivityScanBinding
import com.jabil.securityapp.manager.DeviceAdminManager
import com.jabil.securityapp.utils.Constants
import com.jabil.securityapp.utils.DeviceUtils
import com.jabil.securityapp.utils.PrefsManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class ScanActivity : AppCompatActivity() {
    private lateinit var binding : ActivityScanBinding
    private var currentScanAction: ScanAction = ScanAction.NONE
    private enum class ScanAction { NONE, ENTRY, EXIT }
    private lateinit var deviceAdminManager: DeviceAdminManager
    private lateinit var prefsManager: PrefsManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnHelp.setOnClickListener {
            startActivity(Intent(this, CameraDisabledActivity::class.java))
        }
    }

    private fun startQRScan() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Scan ${if (currentScanAction == ScanActivity.ScanAction.ENTRY) "Entry" else "Exit"} QR Code")
        // Remove setCameraId(0) to allow default selection (fixes some device issues)
        // integrator.setCameraId(0)
        integrator.setBeepEnabled(true)
        integrator.setBarcodeImageEnabled(false)
        integrator.setOrientationLocked(false) // Fixes some orientation/init issues
        integrator.setCaptureActivity(AnyOrientationCaptureActivity::class.java) // See step 3
        integrator.initiateScan()
    }
    private fun handleScanResult(qrContent: String) {

        lifecycleScope.launch {
            try {
                when (currentScanAction) {
                    ScanAction.ENTRY -> processEntryScan(qrContent)
                    ScanAction.EXIT -> processExitScan(qrContent)
                    else -> {}
                }
                updateUI()
            } catch (e: Exception) {
                handleApiError(e)
            } finally {
                currentScanAction = ScanAction.NONE
                updateUI()
            }
        }
    }
    private fun handleApiError(e: Exception) {
        val message = when (e) {
            is IOException -> Constants.ERROR_NO_INTERNET
            is HttpException -> "Server error: ${e.code()}"
            else -> e.message ?: "Unknown error occurred"
        }
        showErrorDialog(message)

        // If error occurred during Exit, re-lock
        if (currentScanAction == ScanAction.EXIT) {
            deviceAdminManager.lockCamera()
            updateUI()
        }
    }
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    private fun isServiceRunning(): Boolean {
        return try {
            val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Integer.MAX_VALUE)
            for (service in services) {
                if (service.service.className == "com.example.cameralockdemo.CameraBlockerService") {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(javaClass.name, "Error checking service status", e)
            false
        }
    }

    private fun updateUI() {
        // Updated logic: Check Hardware Lock, Service Lock, AND Persistent Intent
        val isHardwareLocked = deviceAdminManager.isCameraLocked()
        val isServiceLocked = isServiceRunning()
        val isPersistentlyLocked = prefsManager.isLocked

        val isLocked = isHardwareLocked || isServiceLocked || isPersistentlyLocked
        val isAdmin = deviceAdminManager.isDeviceAdminActive()

        // Colors
        val colorLocked = ContextCompat.getColor(this, R.color.state_locked)
        val colorUnlocked = ContextCompat.getColor(this, R.color.btn_blue)
    }

    private suspend fun processExitScan(token: String) {
        val deviceId = DeviceUtils.getDeviceId(this)

        val request = ScanExitRequest(
            token = token,
            deviceId = deviceId
        )

        val response = RetrofitClient.apiService.scanExit(request)

        if (response.isSuccessful && response.body()?.status == "success") {
            // API validated -> Unlock
            unlockAndRemoveAdmin()
        } else {
            showErrorDialog(response.body()?.message ?: "Exit failed. Please try again.")
            // Validation failed, re-lock
            deviceAdminManager.lockCamera()
            updateUI()
        }
    }

    private fun unlockAndRemoveAdmin() {
        // Clear lock state
        prefsManager.isLocked = false

        // 1. Stop Software Lock (Service)
        stopService(Intent(this, CameraBlockerService::class.java))

        // 2. Unlock Hardware
        if (deviceAdminManager.unlockCamera()) {
            // Try to remove admin
            if (deviceAdminManager.removeDeviceAdmin()) {
                showSuccessDialog("Camera Unlocked", Constants.SUCCESS_CAMERA_UNLOCKED)
            } else {
                showSuccessDialog("Camera Unlocked", "You are checked out. Please manually remove device admin permission if prompted.")
            }
            updateUI()
        } else {
            // Even if hardware unlock fails (maybe it wasn't locked), we stopped the service, so we are good.
            showSuccessDialog("Camera Unlocked", Constants.SUCCESS_CAMERA_UNLOCKED)
            updateUI()
        }
    }
    private fun showSuccessDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setIcon(R.drawable.logo_jabil)
            .show()
    }

    private suspend fun processEntryScan(token: String) {
        val deviceId = DeviceUtils.getDeviceId(this)
        val deviceInfo = DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            osVersion = Build.VERSION.RELEASE,
            platform = "android",
            appVersion = Constants.APP_VERSION,
            deviceName = Build.DEVICE
        )

        val request = ScanEntryRequest(
            token = token,
            deviceId = deviceId,
            deviceInfo = deviceInfo
        )

        val response = RetrofitClient.apiService.scanEntry(request)

        if (response.isSuccessful && response.body()?.status == "success") {
            // API validated -> Request Admin / Lock
            requestDeviceAdmin()
        } else {
            showErrorDialog(response.body()?.message ?: "Entry failed. Please try again.")
        }
    }
    private fun requestDeviceAdmin() {
        // Only handle Device Admin here. Other permissions are handled pre-scan.
        if (!deviceAdminManager.isDeviceAdminActive()) {
            val intent = deviceAdminManager.requestDeviceAdminPermission()
            startActivityForResult(intent, Constants.DEVICE_ADMIN_REQUEST_CODE)
        } else {
            // Already admin, just lock
            lockCamera()
        }
    }
    private fun isMiuiDevice(): Boolean {
        return try {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val brand = Build.BRAND.lowercase()

            manufacturer.contains("xiaomi") ||
                    manufacturer.contains("redmi") ||
                    brand.contains("xiaomi") ||
                    brand.contains("redmi") ||
                    brand.contains("mi")
        } catch (e: Exception) {
            false
        }
    }

    private fun lockCamera() {
        // Persist lock state
        prefsManager.isLocked = true

        // Check if this is MIUI Android 14+ device
        val isMiui14Plus = isMiuiDevice() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

        Log.d(javaClass.name, "Locking camera - MIUI 14+: $isMiui14Plus")

        if (isMiui14Plus) {
            // MIUI Android 14+ specific approach
            lockCameraMiui14Plus()
        } else {
            // Standard approach for all other devices
            lockCameraStandard()
        }

        showSuccessDialog("Camera Locked", if (isMiui14Plus) {
            Constants.SUCCESS_CAMERA_LOCKED + "\n(Active via MIUI 14+ Enhanced Blocking)"
        } else {
            Constants.SUCCESS_CAMERA_LOCKED + "\n(Active via Service & Admin)"
        })
        updateUI()
    }

    private fun lockCameraStandard() {
        // 1. Try Hardware Lock (Legacy)
        var hardwareLockSuccess = false
        try {
            hardwareLockSuccess = deviceAdminManager.lockCamera()
        } catch (e: Exception) {
            Log.e(javaClass.name, "Hardware lock failed", e)
        }

        // 2. Start Software Lock (Service) - Always start this as backup/primary
        try {
            val serviceIntent = Intent(this, CameraBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "Failed to start blocker service", e)
        }
    }

    private fun lockCameraMiui14Plus() {
        Log.d(javaClass.name, "Using MIUI 14+ specific camera locking approach")

        // 1. Try Hardware Lock (may not work but try anyway)
        try {
            deviceAdminManager.lockCamera()
            Log.d(javaClass.name, "MIUI 14+: Hardware lock attempted")
        } catch (e: Exception) {
            Log.e(javaClass.name, "MIUI 14+: Hardware lock failed (expected)", e)
        }

        // 2. Start Enhanced Software Lock with MIUI 14+ specific flags
        try {
            val serviceIntent = Intent(this, CameraBlockerService::class.java)
            serviceIntent.putExtra("IS_MIUI_14_PLUS", true)
            serviceIntent.putExtra("USE_AGGRESSIVE_BLOCKING", true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            Log.d(javaClass.name, "MIUI 14+: Enhanced blocker service started")
        } catch (e: Exception) {
            Log.e(javaClass.name, "MIUI 14+: Failed to start enhanced blocker service", e)
        }

        // 3. Additional MIUI 14+ specific setup
        setupMiui14PlusBlocking()
    }

    private fun setupMiui14PlusBlocking() {
        Log.d(javaClass.name, "Setting up MIUI 14+ specific blocking mechanisms")

        try {
            // Request additional permissions that might help with MIUI 14+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Try to get usage stats permission if not already granted
                if (!hasUsageStatsPermission()) {
                    Log.w(javaClass.name, "MIUI 14+: Usage stats permission not granted - blocking may be less effective")
                }

                // Log MIUI version for debugging
                try {
                    val miuiVersion = Class.forName("android.os.SystemProperties")
                        .getMethod("get", String::class.java)
                        .invoke(null, "ro.miui.ui.version.name") as? String
                    Log.d(javaClass.name, "MIUI 14+: Detected MIUI version: $miuiVersion")
                } catch (e: Exception) {
                    Log.d(javaClass.name, "MIUI 14+: Could not detect MIUI version")
                }
            }
        } catch (e: Exception) {
            Log.e(javaClass.name, "MIUI 14+: Setup failed", e)
        }
    }
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Handle QR Result
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                handleScanResult(result.contents)
            } else {
                Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_SHORT).show()

                // Re-lock if cancelled during Exit flow
                if (currentScanAction == ScanAction.EXIT) {
                    deviceAdminManager.lockCamera()
                    updateUI()
                }
            }
            return
        }

        // Handle Device Admin Result
        if (requestCode == Constants.DEVICE_ADMIN_REQUEST_CODE) {
            if (resultCode == RESULT_OK || deviceAdminManager.isDeviceAdminActive()) {
                // Admin granted, lock camera
                lockCamera()
            } else {
                Toast.makeText(this, "Device admin permission denied", Toast.LENGTH_SHORT).show()
            }
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

}