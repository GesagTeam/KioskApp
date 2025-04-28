package com.example.firsttrykhs

import AdminExitDialog
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    private lateinit var passwordManager: PasswordManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName
    private lateinit var sharedPreferences: SharedPreferences
    private var tapCount = 0
    private val tapHandler = Handler(Looper.getMainLooper())

    // Track kiosk mode state
    private var isKioskModeEnabled by mutableStateOf(true)
    private var adminPassword: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)






        // Initialize DevicePolicyManager and admin component
        devicePolicyManager = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponentName = ComponentName(this, MyDeviceAdminReceiver::class.java)




        // Start in kiosk mode if enabled
        if (isKioskModeEnabled) enableKioskMode()
        setContent {
            var showAdminDialog by remember { mutableStateOf(false) }
            var showReLockDialog by remember { mutableStateOf(false) }

            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        // Detect 5 rapid taps
                        registerTap {
                            if (isKioskModeEnabled) {
                                // Show admin dialog to exit kiosk mode
                                showAdminDialog = true
                            } else {
                                // Show dialog to re-enable kiosk mode
                                showReLockDialog = true
                            }
                        }
                    }
            ) {
                // WebBrowser composable or your main app UI here
                WebBrowser(Modifier.padding(it))

                // Unlock dialog (Admin dialog)
                if (showAdminDialog) {
                    AdminExitDialog(
                        onDismiss = { showAdminDialog = false },
                        onUnlock = {
                            exitKioskMode()
                            isKioskModeEnabled = false
                            showAdminDialog = false
                        }
                    )
                }

                // Re-lock dialog with password or re-enable kiosk mode
                if (showReLockDialog) {
                    ReLockDialog(
                        onDismiss = { showReLockDialog = false },
                        onReLock = {
                            enableKioskMode()
                            isKioskModeEnabled = true
                            showReLockDialog = false
                        }
                    )
                }
            }
        }
    }

    // Detect 5 rapid taps
    private fun registerTap(onSuccess: () -> Unit) {
        tapCount++
        tapHandler.postDelayed({ tapCount = 0 }, 800) // Reset tap count after 800ms
        if (tapCount >= 5) {
            onSuccess()
            tapCount = 0
        }
    }

    // Enable kiosk mode
    private fun enableKioskMode() {
        if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
            devicePolicyManager.setLockTaskPackages(adminComponentName, arrayOf(packageName))
            startLockTask()  // Locks the task to this app
            Log.d("MainActivity", "Kiosk Mode Enabled")
        } else {
            Log.e("MainActivity", "App is not the device owner")
        }
    }

    // Disable kiosk mode
    private fun exitKioskMode() {
        stopLockTask()  // Exit kiosk mode
        Log.d("MainActivity", "Exited Kiosk Mode")
    }
}
