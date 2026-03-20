package com.mahabirneogy.scanflow.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahabirneogy.scanflow.ui.util.WindowSize
import com.mahabirneogy.scanflow.ui.util.rememberWindowSize
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val windowSize = rememberWindowSize()
    val iconSize = when (windowSize) {
        WindowSize.Compact -> 160.dp
        WindowSize.Medium -> 200.dp
        WindowSize.Expanded -> 220.dp
    }
    val innerIconSize = when (windowSize) {
        WindowSize.Compact -> 80.dp
        WindowSize.Medium -> 100.dp
        WindowSize.Expanded -> 110.dp
    }

    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-200).dp)
                .size(300.dp)
                .blur(80.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(150.dp))
        )
        Box(
            modifier = Modifier
                .offset(x = 100.dp, y = 200.dp)
                .size(300.dp)
                .blur(80.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(150.dp))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Color(0xFF7986CB))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "scan")
                val scanOffset by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scan_offset"
                )

                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(innerIconSize)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (iconSize.value * scanOffset).dp)
                        .background(Color.White.copy(alpha = 0.8f))
                        .blur(1.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ScanFlow",
                style = if (windowSize == WindowSize.Compact) MaterialTheme.typography.headlineLarge
                        else MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "SMART PDF SCANNING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp
            )
        }

    }
}
