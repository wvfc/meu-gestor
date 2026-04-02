package com.meugestor.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meugestor.app.MeuGestorApp

@Composable
fun SettingsScreen(app: MeuGestorApp) {
    var darkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var biometricEnabled by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // App info card
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
                            Icon(Icons.Default.AccountBalance, null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Meu Gestor", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text("Versão 1.0.0", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Gestão Financeira Pessoal", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                subtitle = "Proteger o app com biometria (em breve)",
                checked = biometricEnabled,
                onCheckedChange = { biometricEnabled = it },
                enabled = false
            )
        }

        item { SectionHeader("Dados") }
        item {
            SettingsActionItem(
                icon = Icons.Outlined.Backup,
                title = "Backup Local",
                subtitle = "Exportar dados do app",
                onClick = { /* TODO */ }
            )
        }
        item {
            SettingsActionItem(
                icon = Icons.Outlined.CloudUpload,
                title = "Backup na Nuvem",
                subtitle = "Em breve — Google Drive",
                onClick = { /* TODO */ },
                enabled = false
            )
        }
        item {
            SettingsActionItem(
                icon = Icons.Outlined.FileDownload,
                title = "Exportar CSV",
                subtitle = "Exportar lançamentos em planilha",
                onClick = { /* TODO */ },
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
                subtitle = "Todos os dados ficam armazenados localmente no seu dispositivo"
            )
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
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (enabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (enabled) Icon(Icons.Default.ChevronRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsInfoItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
