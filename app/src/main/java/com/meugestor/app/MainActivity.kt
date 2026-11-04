package com.meugestor.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.meugestor.app.ui.navigation.MainScaffold
import com.meugestor.app.ui.security.AppLockGate
import com.meugestor.app.ui.theme.MeuGestorTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MeuGestorApp

        setContent {
            // null = seguir o sistema; o switch "Modo Escuro" das configurações agora é aplicado.
            val darkModePreference by app.userPreferences.isDarkMode.collectAsState(initial = null)
            MeuGestorTheme(darkTheme = darkModePreference ?: isSystemInDarkTheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppLockGate(activity = this, app = app) {
                        MainScaffold(app = app)
                    }
                }
            }
        }
    }
}
