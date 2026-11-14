package com.spendly.app.features.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.TransactionRepository
import com.spendly.app.intelligence.merchant.MerchantRecognitionEngine
import com.spendly.app.intelligence.reconciliation.TransactionReconciliationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DateFilter(val label: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month")
}

data class ActivityUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val currencySymbol: String = "₹",
    val searchQuery: String = "",
    val selectedDateFilter: DateFilter = DateFilter.ALL,
    val selectedCategoryId: String? = null,
    val selectedSource: TransactionSource? = null,
    val selectedStatus: TransactionStatus? = null,
    val selectedTransactionForEdit: Transaction? = null,
    val duplicateCandidates: Pair<Transaction, Transaction>? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val reconciliationEngine: TransactionReconciliationEngine,
    private val merchantEngine: MerchantRecognitionEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

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
            categoryRepository.getAll().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }

        viewModelScope.launch {
            transactionRepository.getAll().collect { transactions ->
                _uiState.update { state ->
                    val filtered = applyFilters(
                        transactions = transactions,
                        query = state.searchQuery,
                        dateFilter = state.selectedDateFilter,
                        categoryId = state.selectedCategoryId,
                        source = state.selectedSource,
                        status = state.selectedStatus
                    )
                    state.copy(
                        transactions = transactions,
                        filteredTransactions = filtered
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredTransactions = applyFilters(
                    state.transactions,
                    query,
                    state.selectedDateFilter,
                    state.selectedCategoryId,
                    state.selectedSource,
                    state.selectedStatus
                )
            )
        }
    }

    fun onDateFilterSelected(filter: DateFilter) {
        _uiState.update { state ->
            state.copy(
                selectedDateFilter = filter,
                filteredTransactions = applyFilters(
                    state.transactions,
                    state.searchQuery,
                    filter,
                    state.selectedCategoryId,
                    state.selectedSource,
                    state.selectedStatus
                )
            )
        }
    }

    fun onCategoryFilterSelected(categoryId: String?) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = categoryId,
                filteredTransactions = applyFilters(
                    state.transactions,
                    state.searchQuery,
                    state.selectedDateFilter,
                    categoryId,
                    state.selectedSource,
                    state.selectedStatus
                )
            )
        }
    }

    fun onSourceFilterSelected(source: TransactionSource?) {
        _uiState.update { state ->
            state.copy(
                selectedSource = source,
                filteredTransactions = applyFilters(
                    state.transactions,
                    state.searchQuery,
                    state.selectedDateFilter,
                    state.selectedCategoryId,
                    source,
                    state.selectedStatus
                )
            )
        }
    }

    fun onStatusFilterSelected(status: TransactionStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedStatus = status,
                filteredTransactions = applyFilters(
                    state.transactions,
                    state.searchQuery,
                    state.selectedDateFilter,
                    state.selectedCategoryId,
                    state.selectedSource,
                    status
                )
            )
        }
    }

    fun onSelectTransactionForEdit(transaction: Transaction?) {
        _uiState.update { it.copy(selectedTransactionForEdit = transaction) }
    }

    fun updateTransaction(
        transactionId: String,
        amountMinorUnits: Long,
        merchantName: String,
        categoryId: String?,
        note: String?,
        status: TransactionStatus
    ) {
        viewModelScope.launch {
            val existing = transactionRepository.getById(transactionId) ?: return@launch
            val updated = existing.copy(
                amountMinorUnits = amountMinorUnits,
                merchantName = merchantName,
                categoryId = categoryId,
                note = note,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
            transactionRepository.update(updated)

            // If user corrected category, teach the merchant engine
            if (categoryId != null && categoryId != existing.categoryId && merchantName.isNotBlank()) {
                merchantEngine.learnUserCorrection(
                    rawMerchant = merchantName,
                    categoryId = categoryId,
                    preferredName = merchantName
                )
            }
            _uiState.update { it.copy(selectedTransactionForEdit = null) }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            transactionRepository.deleteById(transactionId)
        }
    }

    fun showDuplicateResolution(primary: Transaction, duplicate: Transaction) {
        _uiState.update { it.copy(duplicateCandidates = Pair(primary, duplicate)) }
    }

    fun dismissDuplicateResolution() {
        _uiState.update { it.copy(duplicateCandidates = null) }
    }

    fun resolveDuplicate(merge: Boolean) {
        val candidates = _uiState.value.duplicateCandidates ?: return
        viewModelScope.launch {
            reconciliationEngine.resolveDuplicate(
                primaryId = candidates.first.id,
                duplicateId = candidates.second.id,
                merge = merge
            )
            _uiState.update { it.copy(duplicateCandidates = null) }
        }
    }

    fun confirmTransaction(transactionId: String) {
        viewModelScope.launch {
            val existing = transactionRepository.getById(transactionId) ?: return@launch
            transactionRepository.update(
                existing.copy(
                    status = TransactionStatus.CONFIRMED,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun applyFilters(
        transactions: List<Transaction>,
        query: String,
        dateFilter: DateFilter,
        categoryId: String?,
        source: TransactionSource?,
        status: TransactionStatus?
    ): List<Transaction> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val startTimestamp: Long = when (dateFilter) {
            DateFilter.ALL -> 0L
            DateFilter.TODAY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.THIS_WEEK -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
            DateFilter.THIS_MONTH -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }

        return transactions.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    (tx.merchantName?.contains(query, ignoreCase = true) == true) ||
                    (tx.note?.contains(query, ignoreCase = true) == true) ||
                    (tx.purpose?.contains(query, ignoreCase = true) == true)

            val matchesDate = tx.transactionDate >= startTimestamp
            val matchesCategory = categoryId == null || tx.categoryId == categoryId
            val matchesSource = source == null || tx.source == source
            val matchesStatus = status == null || tx.status == status

            matchesQuery && matchesDate && matchesCategory && matchesSource && matchesStatus
        }
    }
}
