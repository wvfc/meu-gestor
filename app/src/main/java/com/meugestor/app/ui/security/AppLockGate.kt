package com.meugestor.app.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meugestor.app.MeuGestorApp

/**
 * Estado de desbloqueio da sessão. Fica num ViewModel para sobreviver à rotação de tela,
 * mas não à morte do processo (um estado salvo em Bundle liberaria o app sem autenticação).
 */
class AppLockState : ViewModel() {
    var unlocked by mutableStateOf(false)
}

/**
 * Protege o conteúdo do app com biometria/credencial do aparelho quando a opção está ativa.
 *
 * - Nada é exibido enquanto a preferência carrega (antes o conteúdo aparecia por um instante).
 * - O app volta a bloquear quando vai para segundo plano.
 * - Ativar a biometria nas configurações não pede uma segunda autenticação na hora.
 */
@Composable
fun AppLockGate(
    activity: FragmentActivity,
    app: MeuGestorApp,
    content: @Composable () -> Unit
) {
    val biometricEnabled by app.userPreferences.isBiometricEnabled.collectAsState(initial = null)
    val lockState: AppLockState = viewModel()
    var authInProgress by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    fun requestUnlock() {
        if (authInProgress || lockState.unlocked) return
        if (!BiometricHelper.canAuthenticate(activity)) {
            lockState.unlocked = true
            message = "Biometria indisponível neste aparelho."
            return
        }

        authInProgress = true
        BiometricHelper.authenticate(
            activity = activity,
            title = "Desbloquear Meu Gestor",
            subtitle = "Use sua biometria ou credencial do dispositivo",
            onSuccess = {
                authInProgress = false
                lockState.unlocked = true
                message = ""
            },
            onError = { error ->
                authInProgress = false
                lockState.unlocked = false
                message = error
            },
            onFailedAttempt = {
                message = "Autenticação falhou. Tente novamente."
            }
        )
    }

    // Quando a preferência é carregada pela primeira vez: sem proteção, libera direto.
    LaunchedEffect(biometricEnabled) {
        when (biometricEnabled) {
            false -> {
                lockState.unlocked = true
            }
            true -> {
                if (!lockState.unlocked) requestUnlock()
            }
            null -> Unit
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // A tela de PIN/padrão do sistema também pausa a activity; não rebloquear nesse caso.
                    if (biometricEnabled == true && !authInProgress) lockState.unlocked = false
                }
                Lifecycle.Event.ON_START -> {
                    if (biometricEnabled == true && !lockState.unlocked) requestUnlock()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when {
        biometricEnabled == null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        biometricEnabled == false || lockState.unlocked -> content()
        else -> LockedScreen(message = message, onUnlockClick = { requestUnlock() })
    }
}

@Composable
private fun LockedScreen(message: String, onUnlockClick: () -> Unit) {
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
                text = message.ifBlank { "Use a biometria para continuar." },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onUnlockClick) {
                Text("Desbloquear")
            }
        }
    }
}
