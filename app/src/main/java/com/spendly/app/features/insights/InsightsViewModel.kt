package com.spendly.app.features.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class CategorySpend(
    val categoryId: String?,
    val categoryName: String,
    val categoryColor: Long,
    val totalMinorUnits: Long,
    val percentage: Float
)

data class InsightsUiState(
    val currencySymbol: String = "₹",
    val thisMonthExpenseMinorUnits: Long = 0L,
    val thisMonthIncomeMinorUnits: Long = 0L,
    val previousMonthExpenseMinorUnits: Long = 0L,
    val thisWeekExpenseMinorUnits: Long = 0L,
    val dailyAverageExpenseMinorUnits: Long = 0L,
    val biggestExpense: Transaction? = null,
    val monthOverMonthChangePercent: Float? = null,
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val totalTransactionCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            appSettingsRepository.getSettingsFlow().collect { settings ->
                if (settings != null) {
                    _uiState.update { it.copy(currencySymbol = settings.currencySymbol) }
                }
            }
        }

        viewModelScope.launch {
            combine(
                transactionRepository.getAll(),
                categoryRepository.getAll()
            ) { transactions, categories ->
                calculateInsights(transactions, categories)
            }.collect { calculated ->
                _uiState.update { calculated }
            }
        }
    }

    private fun calculateInsights(
        transactions: List<Transaction>,
        categories: List<Category>
    ): InsightsUiState {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Current Month Bounds
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val currentMonthStart = calendar.timeInMillis
        val daysInCurrentMonthPassed = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        // Previous Month Bounds
        calendar.add(Calendar.MONTH, -1)
        val prevMonthStart = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val prevMonthEnd = calendar.timeInMillis

        // Current Week Bounds
        calendar.timeInMillis = now
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val currentWeekStart = calendar.timeInMillis

        // Filter transactions
        val thisMonthDebits = transactions.filter {
            it.transactionDate >= currentMonthStart && it.transactionType == TransactionType.DEBIT
        }
        val thisMonthCredits = transactions.filter {
            it.transactionDate >= currentMonthStart && it.transactionType == TransactionType.CREDIT
        }
        val prevMonthDebits = transactions.filter {
            it.transactionDate in prevMonthStart..prevMonthEnd && it.transactionType == TransactionType.DEBIT
        }
        val thisWeekDebits = transactions.filter {
            it.transactionDate >= currentWeekStart && it.transactionType == TransactionType.DEBIT
        }

        val totalMonthExpense = thisMonthDebits.sumOf { it.amountMinorUnits }
        val totalMonthIncome = thisMonthCredits.sumOf { it.amountMinorUnits }
        val totalPrevMonthExpense = prevMonthDebits.sumOf { it.amountMinorUnits }
        val totalWeekExpense = thisWeekDebits.sumOf { it.amountMinorUnits }

        val dailyAvg = if (daysInCurrentMonthPassed > 0) totalMonthExpense / daysInCurrentMonthPassed else totalMonthExpense
        val biggest = thisMonthDebits.maxByOrNull { it.amountMinorUnits }

        val momChange = if (totalPrevMonthExpense > 0) {
            ((totalMonthExpense - totalPrevMonthExpense).toFloat() / totalPrevMonthExpense.toFloat()) * 100f
        } else null

        // Category breakdown
        val categoryMap = categories.associateBy { it.id }
        val groupedByCategory = thisMonthDebits.groupBy { it.categoryId }

        val categorySpends = groupedByCategory.map { (catId, txs) ->
            val sum = txs.sumOf { it.amountMinorUnits }
            val cat = catId?.let { categoryMap[it] }
            val pct = if (totalMonthExpense > 0) (sum.toFloat() / totalMonthExpense.toFloat()) * 100f else 0f
            CategorySpend(
                categoryId = catId,
                categoryName = cat?.name ?: "Other",
                categoryColor = cat?.color ?: 0xFF9E9E9EL,
                totalMinorUnits = sum,
                percentage = pct
            )
        }.sortedByDescending { it.totalMinorUnits }

        return _uiState.value.copy(
            thisMonthExpenseMinorUnits = totalMonthExpense,
            thisMonthIncomeMinorUnits = totalMonthIncome,
            previousMonthExpenseMinorUnits = totalPrevMonthExpense,
            thisWeekExpenseMinorUnits = totalWeekExpense,
            dailyAverageExpenseMinorUnits = dailyAvg,
            biggestExpense = biggest,
            monthOverMonthChangePercent = momChange,
            categoryBreakdown = categorySpends,
            totalTransactionCount = thisMonthDebits.size
        )
    }
}
