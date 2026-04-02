package com.meugestor.app.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.meugestor.app.MeuGestorApp

@Composable
fun AppLockGate(
    activity: FragmentActivity,
    app: MeuGestorApp,
    content: @Composable () -> Unit
) {
    val biometricEnabled by app.userPreferences.isBiometricEnabled.collectAsState(initial = false)
    var unlocked by remember(biometricEnabled) { mutableStateOf(!biometricEnabled) }
    var message by remember { mutableStateOf("") }

    fun requestUnlock() {
        if (!BiometricHelper.canAuthenticate(activity)) {
            unlocked = true
            message = "Biometria indisponível neste aparelho."
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = "Desbloquear Meu Gestor",
            subtitle = "Use sua biometria ou credencial do dispositivo",
            onSuccess = {
                unlocked = true
                message = ""
            },
            onError = { error ->
                unlocked = false
                message = error
            }
        )
    }

    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            unlocked = false
            requestUnlock()
        } else {
            unlocked = true
        }
    }

    if (!biometricEnabled || unlocked) {
        content()
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aplicativo protegido",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (message.isBlank()) {
                    "Use a biometria para continuar."
                } else {
                    message
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { requestUnlock() }) {
                Text("Desbloquear")
            }
        }
    }
}
