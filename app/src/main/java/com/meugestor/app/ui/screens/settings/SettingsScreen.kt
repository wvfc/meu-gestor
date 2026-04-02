package com.meugestor.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.data.backup.BackupExporter
import com.meugestor.app.ui.security.BiometricHelper
import com.meugestor.app.util.DateUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(app: MeuGestorApp) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val biometricEnabled by app.userPreferences.isBiometricEnabled.collectAsState(initial = false)
    val lastBackupDate by app.userPreferences.lastBackupDate.collectAsState(initial = "")

    var pendingBackupContent by remember { mutableStateOf<String?>(null) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null || pendingBackupContent == null) return@rememberLauncherForActivityResult

        scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(pendingBackupContent!!.toByteArray())
                }
                app.userPreferences.setLastBackupDate(DateUtils.today())
            }.onSuccess {
                snackbarHostState.showSnackbar("Backup salvo com sucesso.")
            }.onFailure {
                snackbarHostState.showSnackbar("Falha ao salvar backup: ${it.message}")
            }
        }
    }

    fun toggleBiometric(enable: Boolean) {
        if (!enable) {
            scope.launch { app.userPreferences.setBiometricEnabled(false) }
            return
        }

        if (activity == null) {
            scope.launch { snackbarHostState.showSnackbar("Biometria indisponível nesta tela.") }
            return
        }

        if (!BiometricHelper.canAuthenticate(activity)) {
            scope.launch { snackbarHostState.showSnackbar("Biometria não disponível neste dispositivo.") }
            return
        }

        BiometricHelper.authenticate(
            activity = activity,
            title = "Ativar proteção biométrica",
            subtitle = "Confirme sua identidade para ativar a biometria",
            onSuccess = {
                scope.launch {
                    app.userPreferences.setBiometricEnabled(true)
                    snackbarHostState.showSnackbar("Biometria ativada.")
                }
            },
            onError = { error ->
                scope.launch {
                    snackbarHostState.showSnackbar(error)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AccountBalance,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Meu Gestor",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Versão 1.0.0",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Gestão Financeira Pessoal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Aparência") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "Modo Escuro",
                    subtitle = "Alternar tema claro/escuro",
                    checked = darkMode,
                    onCheckedChange = { darkMode = it }
                )
            }

            item { SectionHeader("Notificações") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notificações",
                    subtitle = "Alertas de vencimento e metas",
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }

            item { SectionHeader("Segurança") }
            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Biometria / PIN",
                    subtitle = if (biometricEnabled) {
                        "Aplicativo protegido por biometria"
                    } else {
                        "Ativar proteção do app por biometria"
                    },
                    checked = biometricEnabled,
                    onCheckedChange = { toggleBiometric(it) }
                )
            }

            item { SectionHeader("Dados") }
            item {
                SettingsActionItem(
                    icon = Icons.Outlined.Backup,
                    title = "Backup Local",
                    subtitle = if (lastBackupDate.isBlank()) {
                        "Exportar dados do app em JSON"
                    } else {
                        "Último backup: ${DateUtils.formatDate(lastBackupDate)}"
                    },
                    onClick = {
                        scope.launch {
                            runCatching {
                                pendingBackupContent = BackupExporter.generateBackupJson(app)
                            }.onSuccess {
                                saveBackupLauncher.launch("meu_gestor_backup_${DateUtils.today()}.json")
                            }.onFailure {
                                snackbarHostState.showSnackbar("Falha ao gerar backup: ${it.message}")
                            }
                        }
                    }
                )
            }
            item {
                SettingsActionItem(
                    icon = Icons.Outlined.FileDownload,
                    title = "Exportar CSV",
                    subtitle = "Pode ser implementado depois do backup JSON",
                    onClick = {},
                    enabled = false
                )
            }

            item { SectionHeader("Sobre") }
            item {
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    title = "Meu Gestor",
                    subtitle = "v1.0.0 • Gestão Financeira Pessoal"
                )
            }
            item {
                SettingsInfoItem(
                    icon = Icons.Outlined.Shield,
                    title = "Privacidade",
                    subtitle = "Os dados ficam armazenados localmente no seu dispositivo"
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = if (enabled) onClick else ({}),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (enabled) {
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsInfoItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
