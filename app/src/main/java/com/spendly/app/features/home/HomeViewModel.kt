package com.spendly.app.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.designsystem.CategoryUiHelper
import com.spendly.app.core.util.FormatUtils
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.ReminderStatus
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.model.UpiSession
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.TransactionRepository
import com.spendly.app.domain.repository.UpiSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySpend(
    val category: Category,
    val totalAmountMinor: Long,
    val percentage: Float
)

data class HomeUiState(
    val trackedBalanceMinor: Long = 0L,
    val currentMonthIncomeMinor: Long = 0L,
    val currentMonthExpenseMinor: Long = 0L,
    val pendingReminderSession: UpiSession? = null,
    val reminderAppName: String = "UPI",
    val recentTransactions: List<Transaction> = emptyList(),
    val categoriesMap: Map<String, Category> = emptyMap(),
    val topCategorySpends: List<CategorySpend> = emptyList(),
    val currencySymbol: String = "₹",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val upiSessionRepository: UpiSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                transactionRepository.getAll(),
                categoryRepository.getAll(),
                appSettingsRepository.getSettingsFlow(),
                upiSessionRepository.getUnreconciledSessions()
            ) { transactions, categories, settings, unreconciledSessions ->
                val currencySymbol = settings?.currencySymbol ?: "₹"
                val catMap = categories.associateBy { it.id }

                val startOfMonth = FormatUtils.getStartOfMonth()
                val endOfMonth = FormatUtils.getEndOfMonth()

                // Calculate total tracked balance
                var totalBalance = 0L
                var monthIncome = 0L
                var monthExpense = 0L
                val monthExpenseByCategory = mutableMapOf<String, Long>()

                for (tx in transactions) {
                    when (tx.transactionType) {
                        TransactionType.CREDIT -> {
                            totalBalance += tx.amountMinorUnits
                            if (tx.transactionDate in startOfMonth..endOfMonth) {
                                monthIncome += tx.amountMinorUnits
                            }
                        }
                        TransactionType.DEBIT -> {
                            totalBalance -= tx.amountMinorUnits
                            if (tx.transactionDate in startOfMonth..endOfMonth) {
                                monthExpense += tx.amountMinorUnits
                                val catId = tx.categoryId ?: "other"
                                monthExpenseByCategory[catId] = (monthExpenseByCategory[catId] ?: 0L) + tx.amountMinorUnits
                            }
                        }
                        TransactionType.REFUND -> {
                            totalBalance += tx.amountMinorUnits
                            if (tx.transactionDate in startOfMonth..endOfMonth) {
                                monthExpense -= tx.amountMinorUnits
                            }
                        }
                        TransactionType.TRANSFER -> {
                            // Transfers between own accounts don't affect tracked net balance
                        }
                    }
                }

                // Category breakdown
                val topCategories = monthExpenseByCategory.entries
                    .sortedByDescending { it.value }
                    .take(5)
                    .map { (catId, amount) ->
                        val cat = catMap[catId] ?: Category(
                            id = catId,
                            name = catId.replaceFirstChar { it.uppercase() },
                            icon = catId,
                            color = 0xFF607D8BL
                        )
                        val pct = if (monthExpense > 0) (amount.toFloat() / monthExpense.toFloat()) else 0f
                        CategorySpend(
                            category = cat,
                            totalAmountMinor = amount,
                            percentage = pct
                        )
                    }

                // Recent transactions (last 6)
                val recent = transactions.sortedByDescending { it.transactionDate }.take(6)

                // Pending reminder session
                val pendingSession = unreconciledSessions.firstOrNull {
                    it.reminderStatus != ReminderStatus.CONFIRMED_NO_PAYMENT &&
                    it.reminderStatus != ReminderStatus.CONFIRMED_PAID &&
                    it.reminderStatus != ReminderStatus.DISMISSED
                }

                val appName = when {
                    pendingSession?.packageName?.contains("google", ignoreCase = true) == true -> "Google Pay"
                    pendingSession?.packageName?.contains("phonepe", ignoreCase = true) == true -> "PhonePe"
                    pendingSession?.packageName?.contains("paytm", ignoreCase = true) == true -> "Paytm"
                    pendingSession?.packageName?.contains("bhim", ignoreCase = true) == true -> "BHIM"
                    pendingSession?.packageName?.contains("dreamplug", ignoreCase = true) == true -> "CRED"
                    else -> "UPI"
                }

                HomeUiState(
                    trackedBalanceMinor = totalBalance,
                    currentMonthIncomeMinor = monthIncome,
                    currentMonthExpenseMinor = monthExpense,
                    pendingReminderSession = pendingSession,
                    reminderAppName = appName,
                    recentTransactions = recent,
                    categoriesMap = catMap,
                    topCategorySpends = topCategories,
                    currencySymbol = currencySymbol,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onReminderNoPayment(session: UpiSession) {
        viewModelScope.launch {
            upiSessionRepository.update(
                session.copy(reminderStatus = ReminderStatus.CONFIRMED_NO_PAYMENT)
            )
        }
    }

    fun onReminderDismissLater(session: UpiSession) {
        viewModelScope.launch {
            upiSessionRepository.update(
                session.copy(reminderStatus = ReminderStatus.DISMISSED)
            )
        }
    }
}
