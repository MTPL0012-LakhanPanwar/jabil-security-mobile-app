package com.jabil.securityapp.manager

import kotlinx.coroutines.delay

/**
 * Mock Manager
 * Simulates backend operations for POC
 */
object MockManager {

    // Simulated Valid QR Codes
    private const val VALID_ENTRY_CODE = "CAM_LOCK_ENTRY"
    private const val VALID_EXIT_CODE = "CAM_LOCK_EXIT"

    suspend fun validateEntryQR(code: String): Boolean {
        delay(1000) // Simulate network delay
        return code.contains("ENTRY", ignoreCase = true) || code == VALID_ENTRY_CODE
    }

    suspend fun validateExitQR(code: String): Boolean {
        delay(1000) // Simulate network delay
        return code.contains("EXIT", ignoreCase = true) || code == VALID_EXIT_CODE
    }
}
