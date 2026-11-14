package com.spendly.app.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.spendly.app.domain.model.Category

object CategoryUiHelper {

    fun getCategoryIcon(iconName: String?): ImageVector {
        return when (iconName?.lowercase()) {
            "restaurant", "food" -> Icons.Filled.Restaurant
            "shopping_bag", "shopping" -> Icons.Filled.ShoppingBag
            "flight", "travel" -> Icons.Filled.Flight
            "local_gas_station", "fuel" -> Icons.Filled.LocalGasStation
            "receipt_long", "bills" -> Icons.Filled.ReceiptLong
            "movie", "entertainment" -> Icons.Filled.Movie
            "medical_services", "health" -> Icons.Filled.MedicalServices
            "school", "education" -> Icons.Filled.School
            "home", "rent" -> Icons.Filled.Home
            "local_grocery_store", "groceries" -> Icons.Filled.LocalGroceryStore
            "swap_horiz", "transfers" -> Icons.Filled.SwapHoriz
            "more_horiz", "other" -> Icons.Filled.MoreHoriz
            else -> Icons.Filled.Category
        }
    }

    fun getIconForCategory(categoryName: String?): ImageVector = getCategoryIcon(categoryName)

    fun getCategoryColor(colorLong: Long?): Color {
        return if (colorLong != null && colorLong != 0L) {
            Color(colorLong)
        } else {
            Color(0xFF607D8B)
        }
    }

    val defaultCategories = listOf(
        Category(id = "food", name = "Food", icon = "restaurant", color = 0xFFFF5722L, isDefault = true),
        Category(id = "shopping", name = "Shopping", icon = "shopping_bag", color = 0xFFE91E63L, isDefault = true),
        Category(id = "travel", name = "Travel", icon = "flight", color = 0xFF2196F3L, isDefault = true),
        Category(id = "fuel", name = "Fuel", icon = "local_gas_station", color = 0xFFFF9800L, isDefault = true),
        Category(id = "bills", name = "Bills", icon = "receipt_long", color = 0xFF9C27B0L, isDefault = true),
        Category(id = "entertainment", name = "Entertainment", icon = "movie", color = 0xFF673AB7L, isDefault = true),
        Category(id = "health", name = "Health", icon = "medical_services", color = 0xFF4CAF50L, isDefault = true),
        Category(id = "education", name = "Education", icon = "school", color = 0xFF00BCD4L, isDefault = true),
        Category(id = "rent", name = "Rent", icon = "home", color = 0xFF795548L, isDefault = true),
        Category(id = "groceries", name = "Groceries", icon = "local_grocery_store", color = 0xFF8BC34AL, isDefault = true),
        Category(id = "transfers", name = "Transfers", icon = "swap_horiz", color = 0xFF009688L, isDefault = true),
        Category(id = "other", name = "Other", icon = "more_horiz", color = 0xFF607D8BL, isDefault = true)
    )
}
