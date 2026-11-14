package com.spendly.app.features.quickentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendly.app.core.designsystem.CategoryUiHelper
import com.spendly.app.core.util.FormatUtils
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.PaymentMethod
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType
import com.spendly.app.domain.repository.AppSettingsRepository
import com.spendly.app.domain.repository.CategoryRepository
import com.spendly.app.domain.repository.MerchantRuleRepository
import com.spendly.app.domain.repository.TransactionRepository
import com.spendly.app.domain.repository.UpiSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuickExpenseEntryUiState(
    val amountText: String = "",
    val currencySymbol: String = "₹",
    val transactionType: TransactionType = TransactionType.DEBIT,
    val selectedCategory: Category? = null,
    val categories: List<Category> = CategoryUiHelper.defaultCategories,
    val merchant: String = "",
    val merchantSuggestions: List<String> = emptyList(),
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val dateMillis: Long = System.currentTimeMillis(),
    val purpose: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val showCustomCategoryDialog: Boolean = false,
    val associatedSessionId: String? = null
)

@HiltViewModel
class QuickExpenseEntryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val merchantRuleRepository: MerchantRuleRepository,
    private val upiSessionRepository: UpiSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickExpenseEntryUiState())
    val uiState: StateFlow<QuickExpenseEntryUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // Load currency from settings
            appSettingsRepository.getSettingsFlow().collect { settings ->
                if (settings != null) {
                    _uiState.update { it.copy(currencySymbol = settings.currencySymbol) }
                }
            }
        }

        viewModelScope.launch {
            // Load categories from repository
            categoryRepository.getAll().collect { dbCategories ->
                val categories = if (dbCategories.isNotEmpty()) {
                    dbCategories
                } else {
                    CategoryUiHelper.defaultCategories
                }
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        selectedCategory = state.selectedCategory ?: categories.firstOrNull()
                    )
                }
            }
        }

        viewModelScope.launch {
            // Collect recent merchants for suggestions
            transactionRepository.getRecent(50).collect { transactions ->
                val merchants = transactions
                    .mapNotNull { it.merchantName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(8)
                _uiState.update { it.copy(merchantSuggestions = merchants) }
            }
        }
    }

    fun prefillData(
        appName: String? = null,
        merchantName: String? = null,
        amountMinorUnits: Long? = null,
        timestamp: Long? = null,
        sessionId: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                merchant = merchantName ?: state.merchant,
                amountText = if (amountMinorUnits != null && amountMinorUnits > 0) {
                    (amountMinorUnits / 100.0).toString()
                } else {
                    state.amountText
                },
                dateMillis = timestamp ?: System.currentTimeMillis(),
                paymentMethod = if (appName != null) PaymentMethod.UPI else state.paymentMethod,
                purpose = if (appName != null) "Paid via $appName" else state.purpose,
                associatedSessionId = sessionId
            )
        }
    }

    fun onAmountChanged(newAmount: String) {
        val filtered = newAmount.filter { it.isDigit() || it == '.' }
        // Ensure only one decimal point
        val dotsCount = filtered.count { it == '.' }
        if (dotsCount <= 1) {
            _uiState.update { it.copy(amountText = filtered, errorMessage = null) }
        }
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onTransactionTypeSelected(type: TransactionType) {
        _uiState.update { it.copy(transactionType = type) }
    }

    fun onMerchantChanged(merchant: String) {
        _uiState.update { it.copy(merchant = merchant) }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onDateChanged(millis: Long) {
        _uiState.update { it.copy(dateMillis = millis) }
    }

    fun onPurposeChanged(purpose: String) {
        _uiState.update { it.copy(purpose = purpose) }
    }

    fun onNoteChanged(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun showAddCategoryDialog(show: Boolean) {
        _uiState.update { it.copy(showCustomCategoryDialog = show) }
    }

    fun addCustomCategory(name: String, icon: String = "category", color: Long = 0xFF607D8BL) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val newCategory = Category(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                icon = icon,
                color = color,
                isDefault = false
            )
            categoryRepository.insert(newCategory)
            _uiState.update {
                it.copy(
                    selectedCategory = newCategory,
                    showCustomCategoryDialog = false
                )
            }
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val state = _uiState.value
        val amountMinor = FormatUtils.parseMajorToMinorUnits(state.amountText)

        if (amountMinor <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter an amount greater than 0") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val category = state.selectedCategory ?: state.categories.firstOrNull()

                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amountMinorUnits = amountMinor,
                    currency = "INR",
                    transactionType = state.transactionType,
                    merchantName = state.merchant.trim().takeIf { it.isNotBlank() },
                    categoryId = category?.id,
                    purpose = state.purpose.trim().takeIf { it.isNotBlank() },
                    paymentMethod = state.paymentMethod,
                    source = TransactionSource.MANUAL,
                    confidence = 1.0f,
                    status = TransactionStatus.CONFIRMED,
                    transactionDate = state.dateMillis,
                    note = state.note.trim().takeIf { it.isNotBlank() }
                )

                transactionRepository.insert(transaction)

                // If associated with a UPI session, reconcile it
                state.associatedSessionId?.let { sessionId ->
                    val session = upiSessionRepository.getById(sessionId)
                    if (session != null) {
                        upiSessionRepository.update(
                            session.copy(
                                reminderStatus = com.spendly.app.domain.model.ReminderStatus.CONFIRMED_PAID,
                                associatedTransactionIds = session.associatedTransactionIds + transaction.id
                            )
                        )
                    }
                }

                // Reset state
                _uiState.update {
                    QuickExpenseEntryUiState(
                        currencySymbol = it.currencySymbol,
                        categories = it.categories,
                        selectedCategory = it.categories.firstOrNull()
                    )
                }

                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message ?: "Failed to save") }
            }
        }
    }
}
