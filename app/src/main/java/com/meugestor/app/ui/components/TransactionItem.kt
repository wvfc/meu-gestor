package com.meugestor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.util.CurrencyUtils

/**
 * Tipo de transação.
 */
enum class TransactionType {
    INCOME,  // Receita
    EXPENSE  // Despesa
}

/**
 * Status da transação.
 */
enum class TransactionStatus(val label: String) {
    PAID("Pago"),
    PENDING("Pendente"),
    OVERDUE("Atrasado"),
    CANCELLED("Cancelado")
}

/**
 * Item de lista para exibir uma transação.
 *
 * @param description Descrição da transação.
 * @param category Nome da categoria.
 * @param categoryColor Cor da categoria (exibida no círculo).
 * @param date Data formatada.
 * @param amount Valor da transação.
 * @param type Tipo: receita ou despesa.
 * @param status Status da transação.
 * @param modifier Modifier do Compose.
 * @param onClick Callback ao clicar no item.
 */
@Composable
fun TransactionItem(
    description: String,
    category: String,
    categoryColor: Color,
    date: String,
    amount: Double,
    type: TransactionType,
    status: TransactionStatus,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val containerModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    }

    Row(
        modifier = containerModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Círculo com cor da categoria
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(categoryColor)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Descrição e categoria
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = " \u2022 ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Valor e status
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            val amountColor = when (type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
            }
            val prefix = when (type) {
                TransactionType.INCOME -> "+ "
                TransactionType.EXPENSE -> "- "
            }

            Text(
                text = prefix + CurrencyUtils.formatBRL(amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(status = status)
        }
    }
}
