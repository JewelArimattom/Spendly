package com.spendly.app.features.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spendly.app.core.designsystem.CategoryUiHelper
import com.spendly.app.core.designsystem.theme.ExpenseRed
import com.spendly.app.core.designsystem.theme.IncomeGreen
import com.spendly.app.core.util.FormatUtils
import com.spendly.app.domain.model.Category
import com.spendly.app.domain.model.Transaction
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.domain.model.TransactionType

@Composable
fun CategoryIconBadge(
    category: Category?,
    modifier: Modifier = Modifier,
    size: Int = 44
) {
    val bgColor = CategoryUiHelper.getCategoryColor(category?.color)
    val icon = CategoryUiHelper.getCategoryIcon(category?.icon)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category?.name ?: "Category",
            tint = bgColor,
            modifier = Modifier.size((size * 0.55).dp)
        )
    }
}

@Composable
fun StatusBadge(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label, icon) = when (status) {
        TransactionStatus.CONFIRMED -> {
            Quad(Color(0xFFE8F5E9), Color(0xFF2E7D32), "Confirmed", Icons.Filled.CheckCircle)
        }
        TransactionStatus.DETECTED, TransactionStatus.PENDING_CONFIRMATION -> {
            Quad(Color(0xFFFFF8E1), Color(0xFFF57F17), "Detected", Icons.Filled.Schedule)
        }
        TransactionStatus.DUPLICATE_CANDIDATE -> {
            Quad(Color(0xFFFFEBEE), Color(0xFFC62828), "Duplicate?", Icons.Filled.ContentCopy)
        }
        TransactionStatus.REFUNDED, TransactionStatus.REVERSED -> {
            Quad(Color(0xFFE3F2FD), Color(0xFF1565C0), "Refunded", Icons.Filled.CheckCircle)
        }
        TransactionStatus.FAILED -> {
            Quad(Color(0xFFFFEBEE), Color(0xFFC62828), "Failed", null)
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    category: Category?,
    currencySymbol: String = "₹",
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDebit = transaction.transactionType == TransactionType.DEBIT
    val amountColor = if (isDebit) ExpenseRed else IncomeGreen
    val amountPrefix = if (isDebit) "- " else "+ "

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBadge(category = category)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            val title = transaction.merchantName?.takeIf { it.isNotBlank() }
                ?: transaction.purpose?.takeIf { it.isNotBlank() }
                ?: category?.name
                ?: "Expense"

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                val method = transaction.paymentMethod?.name?.replace("_", " ") ?: "Manual"
                val time = FormatUtils.formatTime(transaction.transactionDate)

                Text(
                    text = "$method • $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (transaction.status != TransactionStatus.CONFIRMED) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = transaction.status)
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "$amountPrefix${FormatUtils.formatCurrency(transaction.amountMinorUnits, currencySymbol)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun SpendlyConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = if (isDestructive) {
                    ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.textButtonColors()
                }
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
