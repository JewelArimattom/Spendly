package com.spendly.app.features.activity

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendly.app.core.designsystem.CategoryUiHelper
import com.spendly.app.core.designsystem.theme.ExpenseRed
import com.spendly.app.core.designsystem.theme.IncomeGreen
import com.spendly.app.core.util.FormatUtils
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionSource
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Activity",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by merchant, note, purpose...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedDateFilter == filter,
                        onClick = { viewModel.onDateFilterSelected(filter) },
                        label = { Text(filter.label) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category & Source Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source Filters
                TransactionSource.entries.forEach { src ->
                    val isSelected = state.selectedSource == src
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.onSourceFilterSelected(if (isSelected) null else src)
                        },
                        label = { Text(src.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Category Filters
                state.categories.forEach { cat ->
                    val isSelected = state.selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.onCategoryFilterSelected(if (isSelected) null else cat.id)
                        },
                        label = { Text(cat.name) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction List
            if (state.filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.searchQuery.isNotBlank() || state.selectedCategoryId != null)
                                "No transactions match your filters"
                            else
                                "No transactions recorded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.filteredTransactions,
                        key = { it.id }
                    ) { transaction ->
                        val category = state.categories.find { it.id == transaction.categoryId }
                        TransactionListItem(
                            transaction = transaction,
                            category = category,
                            currencySymbol = state.currencySymbol,
                            onClick = { viewModel.onSelectTransactionForEdit(transaction) },
                            onConfirm = { viewModel.confirmTransaction(transaction.id) },
                            onDelete = { showDeleteDialog = transaction }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Edit Transaction Dialog
    state.selectedTransactionForEdit?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            categories = state.categories,
            currencySymbol = state.currencySymbol,
            onDismiss = { viewModel.onSelectTransactionForEdit(null) },
            onSave = { amountMinor, merchant, catId, note, status ->
                viewModel.updateTransaction(
                    transactionId = transaction.id,
                    amountMinorUnits = amountMinor,
                    merchantName = merchant,
                    categoryId = catId,
                    note = note,
                    status = status
                )
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { transaction ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Transaction?") },
            text = {
                Text("Are you sure you want to delete the transaction for ${transaction.merchantName ?: "Unknown"} (${FormatUtils.formatCurrency(transaction.amountMinorUnits, state.currencySymbol)})? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Duplicate Resolution Dialog
    state.duplicateCandidates?.let { (primary, duplicate) ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateResolution,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Possible Duplicate Found")
                }
            },
            text = {
                Column {
                    Text(
                        "We detected two similar payments. Would you like to merge them into a single confirmed expense or keep both?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Primary: ${primary.merchantName ?: "Unknown"}", fontWeight = FontWeight.SemiBold)
                            Text("Source: ${primary.source.name} | ${FormatUtils.formatCurrency(primary.amountMinorUnits, state.currencySymbol)}")
                            Text("Date: ${FormatUtils.formatDateTime(primary.transactionDate)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Candidate: ${duplicate.merchantName ?: "Unknown"}", fontWeight = FontWeight.SemiBold)
                            Text("Source: ${duplicate.source.name} | ${FormatUtils.formatCurrency(duplicate.amountMinorUnits, state.currencySymbol)}")
                            Text("Date: ${FormatUtils.formatDateTime(duplicate.transactionDate)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.resolveDuplicate(merge = true) }) {
                    Text("Merge into One")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.resolveDuplicate(merge = false) }) {
                    Text("Keep Both")
                }
            }
        )
    }
}

@Composable
private fun TransactionListItem(
    transaction: Transaction,
    category: Category?,
    currencySymbol: String,
    onClick: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit
) {
    val isCredit = transaction.transactionType == TransactionType.CREDIT
    val categoryIcon = category?.let { CategoryUiHelper.getIconForCategory(it.name) }
        ?: CategoryUiHelper.getIconForCategory("Other")
    val categoryColor = category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Merchant / Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.merchantName ?: transaction.purpose ?: "Expense",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category?.name ?: "Other",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = FormatUtils.formatDate(transaction.transactionDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Source and Status tags
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source tag
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = transaction.source.name,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status tag if not confirmed
                    if (transaction.status != TransactionStatus.CONFIRMED) {
                        val statusColor = when (transaction.status) {
                            TransactionStatus.DETECTED -> MaterialTheme.colorScheme.tertiary
                            TransactionStatus.PENDING_CONFIRMATION -> Color(0xFFF57C00)
                            TransactionStatus.DUPLICATE_CANDIDATE -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = transaction.status.name.replace("_", " "),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and quick actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isCredit) "+" else "-"} ${FormatUtils.formatCurrency(transaction.amountMinorUnits, currencySymbol)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) IncomeGreen else ExpenseRed
                    )
                )

                if (transaction.status == TransactionStatus.PENDING_CONFIRMATION ||
                    transaction.status == TransactionStatus.DETECTED
                ) {
                    IconButton(
                        onClick = onConfirm,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amountMinor: Long, merchant: String, catId: String?, note: String?, status: TransactionStatus) -> Unit
) {
    var amountText by remember { mutableStateOf((transaction.amountMinorUnits / 100.0).toString()) }
    var merchantName by remember { mutableStateOf(transaction.merchantName ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var status by remember { mutableStateOf(transaction.status) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = merchantName,
                    onValueChange = { merchantName = it },
                    label = { Text("Merchant / Payee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    val currentCategory = categories.find { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = currentCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = ((amountText.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onSave(
                        amountMinor,
                        merchantName,
                        selectedCategoryId,
                        note.ifBlank { null },
                        TransactionStatus.CONFIRMED
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
