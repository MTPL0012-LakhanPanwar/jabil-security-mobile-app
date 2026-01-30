# Complete Android Implementation - Remaining Files

## CameraTestActivity.kt

Create: `app/src/main/java/com/example/cameralockdemo/CameraTestActivity.kt`

```kotlin
package com.example.cameralockdemo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cameralockdemo.manager.DeviceAdminManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraTestActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var tvCameraStatus: TextView
    private lateinit var btnBack: Button
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var deviceAdminManager: DeviceAdminManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_test)
        
        deviceAdminManager = DeviceAdminManager(this)
        
        previewView = findViewById(R.id.previewView)
        tvCameraStatus = findViewById(R.id.tvCameraStatus)
        btnBack = findViewById(R.id.btnBack)
        
        btnBack.setOnClickListener {
            finish()
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        checkCameraStatus()
    }
    
    private fun checkCameraStatus() {
        val isCameraLocked = deviceAdminManager.isCameraLocked()
        
        tvCameraStatus.text = if (isCameraLocked) {
            "❌ Camera is LOCKED by device admin\n\n" +
            "Camera hardware is disabled at system level.\n" +
            "No app can access the camera until you scan the exit QR code."
        } else {
            "✅ Camera is UNLOCKED\n\n" +
            "Camera preview will appear below:"
        }
        
        if (!isCameraLocked) {
            startCamera()
        }
    }
    
    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                100
            )
            return
        }
        
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Camera initialization failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Cannot access camera: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
```

---

## Layout Files

### 1. activity_main.xml

Create: `app/src/main/res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fillViewport="true"
    android:background="#F5F5F5">

    <androidx.constraintlayout.widget.ConstraintLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="24dp">

        <!-- App Title -->
        <TextView
            android:id="@+id/tvAppTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Camera Lock Demo"
            android:textSize="28sp"
            android:textStyle="bold"
            android:textColor="#1976D2"
            android:gravity="center"
            app:layout_constraintTop_toTopOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"/>

        <!-- Status Card -->
        <androidx.cardview.widget.CardView
            android:id="@+id/cardStatus"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="24dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp"
            app:layout_constraintTop_toBottomOf="@id/tvAppTitle">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="20dp">

                <TextView
                    android:id="@+id/tvStatus"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="📱 Camera Unlocked"
                    android:textSize="20sp"
                    android:textStyle="bold"
                    android:gravity="center"
                    android:textColor="#4CAF50"/>

                <View
                    android:layout_width="match_parent"
                    android:layout_height="1dp"
                    android:layout_marginTop="16dp"
                    android:layout_marginBottom="16dp"
                    android:background="#E0E0E0"/>

                <TextView
                    android:id="@+id/tvFacilityInfo"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Facility Info"
                    android:textSize="14sp"
                    android:visibility="gone"
                    android:textColor="#666"/>

            </LinearLayout>

        </androidx.cardview.widget.CardView>

        <!-- Device Info Card -->
        <androidx.cardview.widget.CardView
            android:id="@+id/cardDeviceInfo"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            app:cardCornerRadius="12dp"
            app:cardElevation="4dp"
            app:layout_constraintTop_toBottomOf="@id/cardStatus">

            <TextView
                android:id="@+id/tvDeviceInfo"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:padding="16dp"
                android:text="Device Info"
                android:textSize="12sp"
                android:textColor="#666"/>

        </androidx.cardview.widget.CardView>

        <!-- Buttons -->
        <Button
            android:id="@+id/btnScanEntry"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:layout_marginTop="24dp"
            android:text="🔐 Scan Entry QR"
            android:textSize="18sp"
            android:textStyle="bold"
            android:backgroundTint="#1976D2"
            app:layout_constraintTop_toBottomOf="@id/cardDeviceInfo"/>

        <Button
            android:id="@+id/btnScanExit"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:layout_marginTop="12dp"
            android:text="🔓 Scan Exit QR"
            android:textSize="18sp"
            android:textStyle="bold"
            android:backgroundTint="#4CAF50"
            android:enabled="false"
            app:layout_constraintTop_toBottomOf="@id/btnScanEntry"/>

        <Button
            android:id="@+id/btnTestCamera"
            android:layout_width="match_parent"
            android:layout_height="60dp"
            android:layout_marginTop="12dp"
            android:text="📷 Test Camera"
            android:textSize="18sp"
            android:textStyle="bold"
            android:backgroundTint="#FF9800"
            app:layout_constraintTop_toBottomOf="@id/btnScanExit"/>

        <Button
            android:id="@+id/btnSettings"
            android:layout_width="match_parent"
            android:layout_height="50dp"
            android:layout_marginTop="12dp"
            android:text="⚙️ Device Admin Settings"
            android:textSize="14sp"
            android:backgroundTint="#757575"
            app:layout_constraintTop_toBottomOf="@id/btnTestCamera"/>

        <!-- Progress Bar -->
        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="50dp"
            android:layout_height="50dp"
            android:layout_marginTop="24dp"
            android:visibility="gone"
            app:layout_constraintTop_toBottomOf="@id/btnSettings"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintEnd_toEndOf="parent"/>

    </androidx.constraintlayout.widget.ConstraintLayout>

</ScrollView>
```

### 2. activity_camera_test.xml

Create: `app/src/main/res/layout/activity_camera_test.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <TextView
        android:id="@+id/tvCameraStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:padding="24dp"
        android:text="Camera Status"
        android:textSize="16sp"
        android:textColor="#FFFFFF"
        android:gravity="center"
        app:layout_constraintTop_toTopOf="parent"/>

    <androidx.camera.view.PreviewView
        android:id="@+id/previewView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        app:layout_constraintTop_toBottomOf="@id/tvCameraStatus"
        app:layout_constraintBottom_toTopOf="@id/btnBack"/>

    <Button
        android:id="@+id/btnBack"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:layout_margin="16dp"
        android:text="← Back"
        android:textSize="18sp"
        app:layout_constraintBottom_toBottomOf="parent"/>

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## Gradle Files

### 1. app/build.gradle

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.cameralockdemo'
    compileSdk 34

    defaultConfig {
        applicationId "com.example.cameralockdemo"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = '1.8'
    }
    
    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    
    // QR Code Scanner
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
    
    // Networking
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.activity:activity-ktx:1.8.2'
    
    // CameraX
    def camerax_version = "1.3.1"
    implementation "androidx.camera:camera-core:${camerax_version}"
    implementation "androidx.camera:camera-camera2:${camerax_version}"
    implementation "androidx.camera:camera-lifecycle:${camerax_version}"
    implementation "androidx.camera:camera-view:${camerax_version}"
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

## Testing & Running

### Step 1: Configure Backend URL

Edit `Constants.kt`:
```kotlin
// For Android Emulator
const val BASE_URL = "http://10.0.2.2:5000/api/"

// For Physical Device (replace with your computer's IP)
const val BASE_URL = "http://192.168.1.100:5000/api/"
```

### Step 2: Start Backend Server

```bash
cd camera-lock-backend
npm start
```

### Step 3: Build & Run Android App

```bash
# In Android Studio, click Run ▶️
# Or use command line:
./gradlew installDebug
```

### Step 4: Test the Flow

1. ✅ Backend running on port 5000
2. ✅ Open app on device/emulator
3. ✅ Click "Scan Entry QR"
4. ✅ Scan QR code (generate from backend first!)
5. ✅ Grant Device Admin permission
6. ✅ Camera locks ✅
7. ✅ Try "Test Camera" → Should show "Camera Locked"
8. ✅ Click "Scan Exit QR"
9. ✅ Scan exit QR
10. ✅ Camera unlocks ✅

---

## Troubleshooting

### Problem: Cannot connect to backend

**Solution:**
```
- Ensure backend is running
- Check IP address in Constants.kt
- For emulator: use 10.0.2.2
- For device: use computer's local IP (192.168.x.x)
- Check firewall allows port 5000
```

### Problem: Camera not locking

**Solution:**
```
- Check device admin is enabled
- Go to Settings → Security → Device admin apps
- Ensure "CameraLockDemo" is active
- Try toggling it off and on
```

### Problem: QR scanner not working

**Solution:**
```
- Grant camera permission
- Settings → Apps → CameraLockDemo → Permissions
- Enable Camera
```

---

## Demo Script

### For Presentation:

**1. Introduction (30 seconds)**
```
"I'll demonstrate a proof of concept for camera locking using QR codes and device admin."
```

**2. Show Normal State (15 seconds)**
```
- Click "Test Camera"
- Show camera preview works
```

**3. Entry Flow (60 seconds)**
```
- Click "Scan Entry QR"
- Scan entry QR from backend
- Grant device admin permission
- Show "Camera Locked" status
```

**4. Verify Lock (30 seconds)**
```
- Click "Test Camera"
- Show "Camera is LOCKED" message
- Try opening default Camera app → System blocks it
```

**5. Exit Flow (30 seconds)**
```
- Click "Scan Exit QR"
- Scan exit QR
- Show "Camera Unlocked" status
```

**6. Verify Unlock (15 seconds)**
```
- Click "Test Camera"
- Show camera preview works again
```

**Total: ~3 minutes**

---

## Production Considerations

### For Real Deployment:

1. **Use Work Profile Instead of Device Admin**
   - Less invasive
   - Better for BYOD scenarios
   - Requires EMM/MDM provider

2. **Add Visitor Registration**
   - Collect name, email, purpose
   - Send to backend with enrollment

3. **Offline Support**
   - Cache enrollment status
   - Queue API calls
   - Sync when online

4. **Better UI/UX**
   - Material Design 3
   - Animations
   - Better error messages

5. **Push Notifications**
   - Remind to scan exit QR
   - Emergency unlock notifications

6. **Analytics**
   - Track enrollment success rate
   - Monitor errors
   - Usage statistics

---

This completes the Android POC/Demo implementation!
