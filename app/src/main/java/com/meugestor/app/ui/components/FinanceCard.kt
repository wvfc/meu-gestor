package com.meugestor.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meugestor.app.ui.theme.ExpenseRed
import com.meugestor.app.ui.theme.IncomeGreen
import com.meugestor.app.util.CurrencyUtils

/**
 * Indicador de tendência para o card financeiro.
 *
 * @param percentage Porcentagem de variação (positivo = alta, negativo = queda).
 */
data class TrendIndicator(
    val percentage: Double,
    val isPositive: Boolean = percentage >= 0
)

/**
 * Card reutilizável para resumos financeiros.
 *
 * @param title Título do card (ex: "Receitas", "Despesas", "Saldo").
 * @param value Valor monetário a exibir.
 * @param icon Ícone representativo.
 * @param iconTint Cor do ícone.
 * @param trend Indicador de tendência opcional.
 * @param modifier Modifier do Compose.
 */
@Composable
fun FinanceCard(
    title: String,
    value: Double,
    icon: ImageVector,
    iconTint: Color,
    trend: TrendIndicator? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Cabeçalho com ícone e título
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Valor formatado em reais
            Text(
                text = CurrencyUtils.formatBRL(value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Indicador de tendência (opcional)
            if (trend != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    val trendColor = if (trend.isPositive) IncomeGreen else ExpenseRed
                    val trendIcon = if (trend.isPositive) {
                        Icons.Default.TrendingUp
                    } else {
                        Icons.Default.TrendingDown
                    }

                    Icon(
                        imageVector = trendIcon,
                        contentDescription = if (trend.isPositive) "Tendência de alta" else "Tendência de queda",
                        tint = trendColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = CurrencyUtils.formatPercent(
                            if (trend.percentage < 0) -trend.percentage else trend.percentage
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = trendColor
                    )
                }
            }
        }
    }
}
