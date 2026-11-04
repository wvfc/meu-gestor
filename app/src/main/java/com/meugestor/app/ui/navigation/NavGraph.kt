package com.meugestor.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.ui.screens.cashflow.CashFlowScreen
import com.meugestor.app.ui.screens.creditcards.CreditCardsScreen
import com.meugestor.app.ui.screens.future.FutureEntriesScreen
import com.meugestor.app.ui.screens.goals.GoalsScreen
import com.meugestor.app.ui.screens.home.HomeScreen
import com.meugestor.app.ui.screens.reports.ReportsScreen
import com.meugestor.app.ui.screens.reserves.ReservesScreen
import com.meugestor.app.ui.screens.settings.SettingsScreen
import com.meugestor.app.ui.screens.taxorganizer.TaxOrganizerScreen

/**
 * @param onNavigate navegação entre telas principais. Usa a navegação de nível superior
 * (com restauração de estado) para não empilhar telas repetidas no back stack.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    app: MeuGestorApp,
    onNavigate: (String) -> Unit,
    startDestination: String = Screen.Home.route
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Home.route) {
            HomeScreen(app = app, onNavigate = onNavigate)
        }
        composable(Screen.CashFlow.route) {
            CashFlowScreen(app = app)
        }
        composable(Screen.CreditCards.route) {
            CreditCardsScreen(app = app)
        }
        composable(Screen.Future.route) {
            FutureEntriesScreen(app = app)
        }
        composable(Screen.Reports.route) {
            ReportsScreen(app = app)
        }
        composable(Screen.TaxOrganizer.route) {
            TaxOrganizerScreen(app = app)
        }
        composable(Screen.Goals.route) {
            GoalsScreen(app = app)
        }
        composable(Screen.Reserves.route) {
            ReservesScreen(app = app)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(app = app)
        }
    }
}
