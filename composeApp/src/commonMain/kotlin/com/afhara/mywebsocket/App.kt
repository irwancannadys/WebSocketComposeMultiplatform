package com.afhara.mywebsocket

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.afhara.mywebsocket.core.di.appModule
import com.afhara.mywebsocket.feature.auth.LoginScreen
import com.afhara.mywebsocket.feature.cryptolist.CryptoListScreen
import com.afhara.mywebsocket.feature.dashboard.persentation.DashboardScreen
import com.afhara.mywebsocket.feature.detail.DetailScreen
import com.afhara.mywebsocket.navigation.BottomNavItem
import com.afhara.mywebsocket.navigation.Screen
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(application = {
        modules(appModule)
    }) {
        MaterialTheme {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            val showBottomBar = currentDestination?.let { dest ->
                BottomNavItem.items.any { dest.hasRoute(it.route::class) }
            } ?: false

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            BottomNavItem.items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any {
                                        it.hasRoute(item.route::class)
                                    } == true,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Dashboard) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Login,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable<Screen.Login> {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard) {
                                    popUpTo(Screen.Login) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable<Screen.Dashboard> {
                        DashboardScreen(
                            onCryptoClick = { symbol ->
                                navController.navigate(Screen.Detail(symbol = symbol))
                            }
                        )
                    }

                    composable<Screen.CryptoList> {
                        CryptoListScreen(
                            onCryptoClick = { symbol ->
                                navController.navigate(Screen.Detail(symbol = symbol))
                            }
                        )
                    }

                    composable<Screen.Detail> {
                        DetailScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}