package com.beranfamily.hub

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.beranfamily.hub.ui.Dashboard
import com.beranfamily.hub.ui.FamilyTheme
import com.beranfamily.hub.ui.FamilyViewModel

class MainActivity : ComponentActivity() {

    private val vm: FamilyViewModel by viewModels()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            vm.checkPermission()
        }

    private var askedForPermission = false

    private fun requestCalendarPermission() {
        if (askedForPermission && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR)) {
            // Android won't ask again, so open this app's settings page instead.
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
            return
        }
        askedForPermission = true
        permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        if (!vm.hasCalendarPermission) requestCalendarPermission()

        setContent {
            val keepOn = vm.data.keepScreenOn
            LaunchedEffect(keepOn) {
                if (keepOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            FamilyTheme {
                Dashboard(vm, onRequestCalendarPermission = ::requestCalendarPermission)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        vm.checkPermission()
    }

    /** Full-screen for a wall display; swipe from an edge to show the system bars. */
    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
