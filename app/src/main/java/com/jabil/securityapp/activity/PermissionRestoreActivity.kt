package com.jabil.securityapp.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jabil.securityapp.R
import com.jabil.securityapp.databinding.ActivityPermissionRestoreBinding

class PermissionRestoreActivity : AppCompatActivity() {
    private lateinit var binding : ActivityPermissionRestoreBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}