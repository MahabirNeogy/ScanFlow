package com.example.scanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.scanflow.ui.navigation.AppNavigation
import com.example.scanflow.ui.theme.ScanFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanFlowTheme {
                AppNavigation()
            }
        }
    }
}
