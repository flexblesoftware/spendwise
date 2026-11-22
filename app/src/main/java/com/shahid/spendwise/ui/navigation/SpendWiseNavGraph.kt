package com.shahid.spendwise.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shahid.spendwise.ui.screens.analytics.AnalyticsScreen
import com.shahid.spendwise.ui.screens.backup.BackupExportScreen
import com.shahid.spendwise.ui.screens.budget.BudgetsScreen
import com.shahid.spendwise.ui.screens.camera.ReceiptCameraScreen
import com.shahid.spendwise.ui.screens.debt.DebtsScreen
import com.shahid.spendwise.ui.screens.goals.SavingsGoalsScreen
import com.shahid.spendwise.ui.screens.home.HomeScreen
import com.shahid.spendwise.ui.screens.recurring.RecurringScreen
import com.shahid.spendwise.ui.screens.search.SearchFilterScreen
import com.shahid.spendwise.ui.screens.settings.SettingsScreen
import com.shahid.spendwise.ui.screens.split.SplitBillsScreen
import com.shahid.spendwise.ui.screens.transaction.AddEditTransactionScreen
import com.shahid.spendwise.ui.screens.transaction.AddEditTransactionViewModel
import com.shahid.spendwise.ui.screens.voice.VoiceEntryScreen

@Composable
fun SpendWiseNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onNavigateToAddTransaction = { navController.navigate(Screen.AddEditTransaction.createRoute()) },
                onNavigateToEditTransaction = { id -> navController.navigate(Screen.AddEditTransaction.createRoute(id)) },
                onNavigateToBudgets = { navController.navigate(Screen.Budgets.route) },
                onNavigateToRecurring = { navController.navigate(Screen.Recurring.route) },
                onNavigateToSplit = { navController.navigate(Screen.SplitBills.route) },
                onNavigateToDebts = { navController.navigate(Screen.Debts.route) },
                onNavigateToGoals = { navController.navigate(Screen.SavingsGoals.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToSearch = { navController.navigate(Screen.SearchFilter.route) },
                onNavigateToVoice = { navController.navigate(Screen.VoiceEntry.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToBackup = { navController.navigate(Screen.BackupExport.route) }
            )
        }

        composable(
            route = Screen.AddEditTransaction.route,
            arguments = listOf(
                navArgument("transactionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel: AddEditTransactionViewModel = hiltViewModel()
            AddEditTransactionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { navController.navigate(Screen.CameraReceipt.route) }
            )
        }

        composable(Screen.CameraReceipt.route) {
            ReceiptCameraScreen(
                onPhotoCaptured = { photoPath ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("captured_receipt_path", photoPath)
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Budgets.route) {
            BudgetsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recurring.route) {
            RecurringScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SplitBills.route) {
            SplitBillsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Debts.route) {
            DebtsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SavingsGoals.route) {
            SavingsGoalsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SearchFilter.route) {
            SearchFilterScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTransaction = { id -> navController.navigate(Screen.AddEditTransaction.createRoute(id)) }
            )
        }

        composable(Screen.VoiceEntry.route) {
            VoiceEntryScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BackupExport.route) {
            BackupExportScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBackup = { navController.navigate(Screen.BackupExport.route) }
            )
        }
    }
}
