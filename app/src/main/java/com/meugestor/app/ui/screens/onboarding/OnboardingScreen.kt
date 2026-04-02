@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.meugestor.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.meugestor.app.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Default.AccountBalance, "Bem-vindo ao Meu Gestor",
            "Controle completo da sua vida financeira pessoal de forma simples, visual e inteligente.",
            EmeraldGreen),
        OnboardingPage(Icons.Default.AccountBalanceWallet, "Controle Total",
            "Gerencie entradas, saídas, despesas fixas e variáveis. Saiba exatamente para onde seu dinheiro vai.",
            BluePetrol),
        OnboardingPage(Icons.Default.CreditCard, "Cartões de Crédito",
            "Controle seus cartões, faturas e parcelas. Nunca seja surpreendido pelo fechamento da fatura.",
            EmeraldGreenDark),
        OnboardingPage(Icons.Default.CalendarMonth, "Planejamento Futuro",
            "Visualize recebimentos e compromissos futuros. Evite o risco de saldo negativo com antecedência.",
            BluePetrolDark),
        OnboardingPage(Icons.Default.Description, "Imposto de Renda",
            "Organize seus documentos para o IR durante o ano. Nunca mais perca um comprovante importante.",
            GoldAccent),
        OnboardingPage(Icons.Default.Flag, "Metas Financeiras",
            "Defina metas de gastos por categoria e receba alertas quando estiver se aproximando do limite.",
            EmeraldGreen)
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            OnboardingPageContent(pages[page])
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (pagerState.currentPage == index) 24.dp else 8.dp)
                            .background(
                                color = if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) { Text("Anterior") }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                if (pagerState.currentPage < pages.size - 1) {
                    Button(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) { Text("Próximo") }
                } else {
                    Button(
                        onClick = onComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Começar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onComplete) {
                Text("Pular introdução", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .padding(top = 80.dp, bottom = 200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = page.color.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = page.color
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
        )
    }
}
