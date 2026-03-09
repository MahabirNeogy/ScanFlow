package com.example.scanflow.ui.util

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

enum class WindowSize { Compact, Medium, Expanded }

@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp) {
        when {
            configuration.screenWidthDp >= 840 -> WindowSize.Expanded
            configuration.screenWidthDp >= 600 -> WindowSize.Medium
            else -> WindowSize.Compact
        }
    }
}

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}
