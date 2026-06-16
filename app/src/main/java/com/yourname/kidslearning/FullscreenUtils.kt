// File: Utils.kt
package com.yourname.kidslearning

import android.view.Window

import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun enableImmersiveMode(window: Window) {
    // Let the app draw behind system bars
    WindowCompat.setDecorFitsSystemWindows(window, false)

    // Hide both status bar and navigation bar
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    controller.hide(WindowInsetsCompat.Type.systemBars())

    // Allow system bars to temporarily reappear with a swipe
    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
