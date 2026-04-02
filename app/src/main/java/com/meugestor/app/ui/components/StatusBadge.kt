package com.meugestor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.meugestor.app.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor) = when (status.uppercase()) {
        "PAID" -> Triple("Pago", IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
        "PENDING" -> Triple("Pendente", WarningOrange.copy(alpha = 0.15f), WarningOrange)
        "OVERDUE" -> Triple("Atrasado", ExpenseRed.copy(alpha = 0.15f), ExpenseRed)
        "CANCELLED" -> Triple("Cancelado", MediumGray, DarkGray)
        "RECEIVED" -> Triple("Recebido", IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
        else -> Triple(status, MediumGray, DarkGray)
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
