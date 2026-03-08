package com.example.scanflow.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavigationBar(currentRoute: String, onScanClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary

    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = currentRoute == "files",
            onClick = {},
            icon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = if (currentRoute == "files") primary else Color.Gray
                )
            },
            label = { Text("FILES", color = if (currentRoute == "files") primary else Color.Gray) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavigationBarItem(
            selected = currentRoute == "scan",
            onClick = onScanClick,
            icon = {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = if (currentRoute == "scan") primary else Color.Gray
                )
            },
            label = { Text("SCAN", color = if (currentRoute == "scan") primary else Color.Gray) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
    }
}
