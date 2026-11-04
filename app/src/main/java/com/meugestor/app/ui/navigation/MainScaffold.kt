package com.meugestor.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.meugestor.app.MeuGestorApp
import com.meugestor.app.ui.screens.onboarding.OnboardingScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(app: MeuGestorApp) {
    // null enquanto a preferência carrega, para não piscar a introdução para quem já a viu.
    val hasCompletedOnboarding by app.userPreferences.hasCompletedOnboarding.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    when (hasCompletedOnboarding) {
        null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        false -> OnboardingScreen(onComplete = {
            scope.launch { app.userPreferences.setHasCompletedOnboarding(true) }
        })
        true -> AppContent(app)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppContent(app: MeuGestorApp) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentTitle = Screen.bottomNavItems.find { it.route == currentRoute }?.title
        ?: Screen.drawerItems.find { it.route == currentRoute }?.title
        ?: "Meu Gestor"

    fun navigateToTopLevel(route: String) {
        if (route == currentRoute) return
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // O botão voltar fecha o menu lateral antes de sair da tela.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Meu Gestor",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(8.dp))

                Screen.drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navigateToTopLevel(screen.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        // Antes as telas do menu lateral (IR, Metas, Reservas, Configurações) ficavam sem
        // barra superior e inferior, sem nenhuma forma visível de navegar.
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentTitle) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    Screen.bottomNavItems.forEach { screen ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                val icon = if (selected) screen.selectedIcon else screen.icon
                                icon?.let { Icon(imageVector = it, contentDescription = screen.title) }
                            },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = selected,
                            onClick = { navigateToTopLevel(screen.route) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                NavGraph(
                    navController = navController,
                    app = app,
                    onNavigate = { navigateToTopLevel(it) }
                )
            }
        }
    }
}
