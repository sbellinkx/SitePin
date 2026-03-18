package com.sitepinapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sitepinapp.platform.PlatformContext
import com.sitepinapp.ui.navigation.SitePinNavHost
import com.sitepinapp.ui.theme.SitePinTheme
import com.sitepinapp.services.UserProfileManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlatformContext.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            SitePinTheme(themeMode = UserProfileManager.getTheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SitePinNavHost()
                }
            }
        }
    }
}
