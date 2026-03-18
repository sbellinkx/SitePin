package com.sitepinapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.sitepinapp.ui.navigation.SitePinNavHost
import com.sitepinapp.ui.theme.SitePinTheme
import com.sitepinapp.services.UserProfileManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

fun MainViewController() = ComposeUIViewController {
    val themeMode = remember { mutableStateOf(UserProfileManager.getTheme()) }
    SitePinTheme(themeMode = themeMode.value) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            SitePinNavHost()
        }
    }
}
