package com.meugestor.app.ui.common

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.Flow

/** Exibe como Toast as mensagens emitidas pelo ViewModel (sucesso/erro de operações). */
@Composable
fun CollectMessages(messages: Flow<String>) {
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
