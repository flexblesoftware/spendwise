package com.shahid.spendwise.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(name: String): ImageVector {
        return when (name) {
            "Restaurant" -> Icons.Default.Restaurant
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "Home" -> Icons.Default.Home
            "Bolt" -> Icons.Default.Bolt
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "LocalHospital" -> Icons.Default.LocalHospital
            "Movie" -> Icons.Default.Movie
            "School" -> Icons.Default.School
            "Flight" -> Icons.Default.Flight
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "Work" -> Icons.Default.Work
            "TrendingUp" -> Icons.Default.TrendingUp
            "CardGiftcard" -> Icons.Default.CardGiftcard
            else -> Icons.Default.Category
        }
    }
}
