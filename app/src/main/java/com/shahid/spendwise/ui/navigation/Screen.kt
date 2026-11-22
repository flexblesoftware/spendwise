package com.shahid.spendwise.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddEditTransaction : Screen("add_edit_transaction?transactionId={transactionId}") {
        fun createRoute(transactionId: Long? = null) =
            if (transactionId != null) "add_edit_transaction?transactionId=$transactionId" else "add_edit_transaction"
    }
    object CameraReceipt : Screen("camera_receipt")
    object Budgets : Screen("budgets")
    object Recurring : Screen("recurring")
    object SplitBills : Screen("split_bills")
    object Debts : Screen("debts")
    object SavingsGoals : Screen("savings_goals")
    object Analytics : Screen("analytics")
    object SearchFilter : Screen("search_filter")
    object VoiceEntry : Screen("voice_entry")
    object BackupExport : Screen("backup_export")
    object Settings : Screen("settings")
}
