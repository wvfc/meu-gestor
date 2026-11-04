@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.meugestor.app.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meugestor.app.data.database.entity.TransactionEntity
import com.meugestor.app.data.database.entity.TransactionStatus
import com.meugestor.app.data.database.entity.TransactionType
import com.meugestor.app.domain.displayStatus
import com.meugestor.app.domain.effectiveDueIso
import com.meugestor.app.domain.isOpen
import com.meugestor.app.domain.isPartOfSeries
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.ui.theme.WarningOrange
import com.meugestor.app.util.CurrencyUtils
import com.meugestor.app.util.DateUtils

/** Texto de vencimento de um item em aberto ("Vence hoje", "Vencido há 2 dias"...). */
fun dueLabel(transaction: TransactionEntity): String {
    val dueIso = transaction.effectiveDueIso()
    val days = DateUtils.daysUntilDue(dueIso)
    return when {
        days < 0 -> if (days == -1) "Vencido há 1 dia" else "Vencido há ${-days} dias"
        days == 0 -> "Vence hoje"
        days == 1 -> "Vence amanhã"
        days <= 7 -> "Vence em $days dias"
        else -> "Vence em ${DateUtils.formatDate(dueIso)}"
    }
}

/** Linha padrão de lançamento usada nas listas do app. */
@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    modifier: Modifier = Modifier,
    categoryName: String? = null,
    showDueInfo: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isCard = transaction.creditCardId != null
    val color = if (isIncome) IncomeGreen else ExpenseRed
    val status = transaction.displayStatus()

    val subtitle = buildList {
        if (showDueInfo && transaction.isOpen()) add(dueLabel(transaction))
        else add(DateUtils.formatDate(transaction.date))
        categoryName?.let { add(it) }
        if ((transaction.totalInstallments ?: 1) > 1) {
            add("${transaction.installmentNumber}/${transaction.totalInstallments}")
        }
    }.joinToString(" • ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.small,
            color = color.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when {
                        isCard -> Icons.Filled.CreditCard
                        isIncome -> Icons.Filled.ArrowUpward
                        else -> Icons.Filled.ArrowDownward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = color
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == TransactionStatus.OVERDUE) ExpenseRed
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!transaction.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.StickyNote2,
                        contentDescription = "Possui observações",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isIncome) "+" else "-"} ${CurrencyUtils.formatBRL(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            StatusBadge(status.name)
        }
    }
}

private enum class PendingDelete { SINGLE, SERIES }

/**
 * Detalhes de um lançamento com as ações disponíveis: marcar como pago/pendente,
 * editar e excluir (com confirmação).
 */
@Composable
fun TransactionActionsDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    categoryName: String? = null,
    accountName: String? = null,
    cardName: String? = null,
    onEdit: (() -> Unit)? = null,
    onDeleteOpenSeries: (() -> Unit)? = null
) {
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    val confirmation = pendingDelete
    if (confirmation != null) {
        ConfirmDialog(
            title = if (confirmation == PendingDelete.SINGLE) "Excluir lançamento?" else "Excluir parcelas em aberto?",
            message = if (confirmation == PendingDelete.SINGLE) {
                "\"${transaction.description}\" será excluído. Se já estava pago, o saldo da conta é ajustado automaticamente."
            } else {
                "Todas as parcelas/repetições em aberto de \"${transaction.description}\" serão excluídas. Itens já pagos são mantidos."
            },
            confirmLabel = "Excluir",
            onConfirm = {
                if (confirmation == PendingDelete.SINGLE) onDelete() else onDeleteOpenSeries?.invoke()
                onDismiss()
            },
            onDismiss = { pendingDelete = null }
        )
        return
    }

    val isIncome = transaction.type == TransactionType.INCOME
    val status = transaction.displayStatus()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(transaction.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailLine("Valor", CurrencyUtils.formatBRL(transaction.amount), if (isIncome) IncomeGreen else ExpenseRed)
                DetailLine("Tipo", if (isIncome) "Receita" else "Despesa")
                DetailLine("Data", DateUtils.formatDate(transaction.date))
                if (transaction.isOpen()) {
                    DetailLine(
                        "Vencimento",
                        "${DateUtils.formatDate(transaction.effectiveDueIso())} (${dueLabel(transaction).lowercase()})",
                        if (status == TransactionStatus.OVERDUE) ExpenseRed else WarningOrange
                    )
                }
                transaction.paymentDate?.let { DetailLine(if (isIncome) "Recebido em" else "Pago em", DateUtils.formatDate(it)) }
                accountName?.let { DetailLine("Conta", it) }
                cardName?.let { DetailLine("Cartão", it) }
                categoryName?.let { DetailLine("Categoria", it) }
                if ((transaction.totalInstallments ?: 1) > 1) {
                    DetailLine("Parcela", "${transaction.installmentNumber} de ${transaction.totalInstallments}")
                }
                if (!transaction.notes.isNullOrBlank()) {
                    DetailLine("Observações", transaction.notes)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.bodySmall)
                    StatusBadge(status.name)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                if (transaction.status != TransactionStatus.CANCELLED) {
                    Button(
                        onClick = {
                            onToggleStatus()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                transaction.status == TransactionStatus.PAID -> "Marcar como pendente"
                                isIncome -> "Marcar como recebido"
                                else -> "Marcar como pago"
                            }
                        )
                    }
                }
                if (onEdit != null) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Text("Editar")
                    }
                }
                OutlinedButton(
                    onClick = { pendingDelete = PendingDelete.SINGLE },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
                if (onDeleteOpenSeries != null && transaction.isPartOfSeries()) {
                    TextButton(
                        onClick = { pendingDelete = PendingDelete.SERIES },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Excluir todas as parcelas em aberto", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@Composable
private fun DetailLine(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
