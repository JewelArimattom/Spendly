package com.spendly.app.intelligence.reconciliation

import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.intelligence.merchant.MerchantRecognitionEngine
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/**
 * Result outcome of transaction reconciliation.
 */
sealed class ReconciliationResult {
    data class Merged(
        val mergedTransaction: TransactionEntity,
        val existingTransaction: TransactionEntity,
        val incomingTransaction: TransactionEntity,
        val reason: String
    ) : ReconciliationResult()

    data class DuplicateCandidate(
        val existingTransaction: TransactionEntity,
        val incomingTransaction: TransactionEntity,
        val reason: String
    ) : ReconciliationResult()

    data class RefundMatched(
        val originalTransaction: TransactionEntity,
        val refundTransaction: TransactionEntity,
        val reason: String
    ) : ReconciliationResult()

    data class NewTransaction(
        val transaction: TransactionEntity,
        val status: TransactionStatus
    ) : ReconciliationResult()
}

/**
 * Feature F: Multi-source Transaction Deduplication and Reconciliation Engine.
 *
 * Reconciles incoming transaction records from multiple sources (notifications, SMS, UPI sessions, imports)
 * according to explicit criteria, handles transaction lifecycle transitions, and resolves duplicate candidates.
 */
class TransactionReconciliationEngine(
    private val transactionDao: TransactionDao,
    private val merchantEngine: MerchantRecognitionEngine
) {

    companion object {
        const val DEFAULT_TIME_WINDOW_MS = 5 * 60 * 1000L   // 5 minutes
        const val MAX_TIME_WINDOW_MS = 15 * 60 * 1000L       // 15 minutes
        const val CANDIDATE_WINDOW_MS = 2 * 60 * 1000L       // 2 minutes
        const val CONFIDENCE_THRESHOLD_CONFIRMED = 0.90f
    }

    /**
     * Reconciles an incoming transaction against existing records in the database.
     *
     * @param incoming The new incoming transaction candidate.
     * @param timeWindowMs Configurable time window in milliseconds (default: 5 minutes, up to 15 minutes).
     * @return ReconciliationResult detailing whether the transaction was merged, flagged as a duplicate candidate,
     *         matched as a refund, or saved as a new record.
     */
    suspend fun reconcile(
        incoming: TransactionEntity,
        timeWindowMs: Long = DEFAULT_TIME_WINDOW_MS
    ): ReconciliationResult {
        val window = timeWindowMs.coerceIn(1_000L, MAX_TIME_WINDOW_MS)
        val now = System.currentTimeMillis()

        // 1. Check for Refund / Reversal:
        // If incoming is CREDIT or REFUND and references an existing DEBIT's UTR / reference ID,
        // mark existing transaction as REFUNDED.
        val isCreditOrRefund = incoming.transactionType.equals("CREDIT", ignoreCase = true) ||
                incoming.transactionType.equals("REFUND", ignoreCase = true)

        if (isCreditOrRefund && !incoming.sourceReference.isNullOrBlank()) {
            val existing = transactionDao.getBySourceReference(incoming.sourceReference)
            if (existing != null && existing.id != incoming.id) {
                val isExistingDebit = existing.transactionType.equals("DEBIT", ignoreCase = true)
                if (isExistingDebit) {
                    val updatedExisting = existing.copy(
                        status = TransactionStatus.REFUNDED.name,
                        updatedAt = now
                    )
                    transactionDao.update(updatedExisting)

                    val refundTx = incoming.copy(
                        status = TransactionStatus.REFUNDED.name,
                        confidence = 1.0f,
                        updatedAt = now
                    )
                    transactionDao.insert(refundTx)

                    return ReconciliationResult.RefundMatched(
                        originalTransaction = updatedExisting,
                        refundTransaction = refundTx,
                        reason = "Credit notification matches debit reference ID: ${incoming.sourceReference}"
                    )
                }
            }
        }

        // 2. Match Criterion 1: Exact external reference match (UPI reference ID / UTR number / bank ref number).
        if (!incoming.sourceReference.isNullOrBlank() && incoming.sourceReference.trim().length >= 4) {
            val existingRefMatch = transactionDao.getBySourceReference(incoming.sourceReference.trim())
            if (existingRefMatch != null && existingRefMatch.id != incoming.id) {
                // Same transaction captured from multiple sources (e.g. PhonePe notification + Bank SMS)
                val merged = mergeTransactions(
                    primary = existingRefMatch,
                    secondary = incoming,
                    confidenceBoost = 0.95f
                )
                transactionDao.update(merged)
                return ReconciliationResult.Merged(
                    mergedTransaction = merged,
                    existingTransaction = existingRefMatch,
                    incomingTransaction = incoming,
                    reason = "Exact external reference match: ${incoming.sourceReference}"
                )
            }
        }

        // Fetch candidate transactions within the time window around incoming transaction date
        val rangeStart = incoming.transactionDate - window
        val rangeEnd = incoming.transactionDate + window
        val candidatesInRange = try {
            transactionDao.getByDateRange(rangeStart, rangeEnd).first()
        } catch (_: Exception) {
            emptyList()
        }

        // Filter out self and find candidates with exact same amount
        val sameAmountCandidates = candidatesInRange.filter {
            it.id != incoming.id && it.amountMinorUnits == incoming.amountMinorUnits
        }

        // 3. Match Criterion 2: Exact amount + same merchant + within configurable time window (default 5m, up to 15m)
        val sameMerchantMatch = sameAmountCandidates.firstOrNull { candidate ->
            val timeDiff = abs(candidate.transactionDate - incoming.transactionDate)
            timeDiff <= window && isSameMerchant(candidate.merchantName, incoming.merchantName)
        }

        if (sameMerchantMatch != null) {
            val merged = mergeTransactions(
                primary = sameMerchantMatch,
                secondary = incoming,
                confidenceBoost = 0.92f
            )
            transactionDao.update(merged)
            return ReconciliationResult.Merged(
                mergedTransaction = merged,
                existingTransaction = sameMerchantMatch,
                incomingTransaction = incoming,
                reason = "Exact amount (${incoming.amountMinorUnits}) and same merchant within ${window / 60000}m window"
            )
        }

        // 4. Match Criterion 3: Same amount + different merchant name + within 2 minutes:
        // Flag as DUPLICATE_CANDIDATE for manual user resolution.
        val differentMerchantCandidate = sameAmountCandidates.firstOrNull { candidate ->
            val timeDiff = abs(candidate.transactionDate - incoming.transactionDate)
            timeDiff <= CANDIDATE_WINDOW_MS && !isSameMerchant(candidate.merchantName, incoming.merchantName)
        }

        if (differentMerchantCandidate != null) {
            val flaggedExisting = differentMerchantCandidate.copy(
                status = TransactionStatus.DUPLICATE_CANDIDATE.name,
                updatedAt = now
            )
            transactionDao.update(flaggedExisting)

            val flaggedIncoming = incoming.copy(
                status = TransactionStatus.DUPLICATE_CANDIDATE.name,
                updatedAt = now
            )
            transactionDao.insert(flaggedIncoming)

            return ReconciliationResult.DuplicateCandidate(
                existingTransaction = flaggedExisting,
                incomingTransaction = flaggedIncoming,
                reason = "Same amount (${incoming.amountMinorUnits}) with different merchant within 2 minutes"
            )
        }

        // 5. Never automatically merge two transactions solely based on amount if merchants or timestamps differ significantly.
        // Determine status lifecycle for non-duplicate incoming transaction:
        val determinedStatus = when {
            incoming.confidence > CONFIDENCE_THRESHOLD_CONFIRMED -> TransactionStatus.CONFIRMED.name
            incoming.status == TransactionStatus.CONFIRMED.name -> TransactionStatus.CONFIRMED.name
            incoming.status == TransactionStatus.PENDING_CONFIRMATION.name -> TransactionStatus.PENDING_CONFIRMATION.name
            incoming.status == TransactionStatus.DETECTED.name -> {
                if (incoming.confidence < 0.70f || incoming.merchantName.isNullOrBlank()) {
                    TransactionStatus.PENDING_CONFIRMATION.name
                } else {
                    TransactionStatus.DETECTED.name
                }
            }
            incoming.status.isBlank() -> {
                if (incoming.confidence < 0.70f || incoming.merchantName.isNullOrBlank()) {
                    TransactionStatus.PENDING_CONFIRMATION.name
                } else {
                    TransactionStatus.DETECTED.name
                }
            }
            else -> incoming.status
        }

        val finalTransaction = incoming.copy(
            status = determinedStatus,
            updatedAt = now
        )
        transactionDao.insert(finalTransaction)

        val statusEnum = try {
            TransactionStatus.valueOf(determinedStatus)
        } catch (_: Exception) {
            TransactionStatus.DETECTED
        }

        return ReconciliationResult.NewTransaction(
            transaction = finalTransaction,
            status = statusEnum
        )
    }

    /**
     * Resolves a duplicate candidate pair:
     * - If merge = true: Merges transactions, deletes the duplicate, keeps highest confidence fields,
     *   sets status to CONFIRMED.
     * - If merge = false: Keeps both transactions as separate CONFIRMED records.
     */
    suspend fun resolveDuplicate(
        primaryId: String,
        duplicateId: String,
        merge: Boolean
    ): Result<TransactionEntity?> {
        val primary = transactionDao.getById(primaryId)
            ?: return Result.failure(IllegalArgumentException("Primary transaction not found: $primaryId"))
        val duplicate = transactionDao.getById(duplicateId)
            ?: return Result.failure(IllegalArgumentException("Duplicate transaction not found: $duplicateId"))

        val now = System.currentTimeMillis()

        return if (merge) {
            val merged = mergeTransactions(
                primary = primary,
                secondary = duplicate,
                confidenceBoost = 1.0f
            ).copy(
                status = TransactionStatus.CONFIRMED.name,
                updatedAt = now
            )

            transactionDao.update(merged)
            transactionDao.deleteById(duplicateId)
            Result.success(merged)
        } else {
            // Keep both as separate transactions with CONFIRMED status
            val resolvedPrimary = primary.copy(
                status = TransactionStatus.CONFIRMED.name,
                updatedAt = now
            )
            val resolvedDuplicate = duplicate.copy(
                status = TransactionStatus.CONFIRMED.name,
                updatedAt = now
            )

            transactionDao.update(resolvedPrimary)
            transactionDao.update(resolvedDuplicate)
            Result.success(null)
        }
    }

    /**
     * Determines if two merchant names refer to the same merchant.
     */
    fun isSameMerchant(name1: String?, name2: String?): Boolean {
        if (name1.isNullOrBlank() || name2.isNullOrBlank()) return false
        val clean1 = name1.trim()
        val clean2 = name2.trim()

        if (clean1.equals(clean2, ignoreCase = true)) return true

        val norm1 = merchantEngine.normalizeMerchant(clean1)
        val norm2 = merchantEngine.normalizeMerchant(clean2)

        if (norm1.isNotBlank() && norm2.isNotBlank()) {
            if (norm1.equals(norm2, ignoreCase = true)) return true
            if (norm1.contains(norm2, ignoreCase = true) || norm2.contains(norm1, ignoreCase = true)) return true
        }

        return false
    }

    /**
     * Merges two transaction records into one, retaining the highest confidence fields.
     */
    private fun mergeTransactions(
        primary: TransactionEntity,
        secondary: TransactionEntity,
        confidenceBoost: Float
    ): TransactionEntity {
        val now = System.currentTimeMillis()
        val pickPrimary = primary.confidence >= secondary.confidence

        val bestMerchant = if (pickPrimary) {
            primary.merchantName?.ifBlank { null } ?: secondary.merchantName
        } else {
            secondary.merchantName?.ifBlank { null } ?: primary.merchantName
        }

        val bestCategory = primary.categoryId ?: secondary.categoryId
        val bestPurpose = if (pickPrimary) {
            primary.purpose?.ifBlank { null } ?: secondary.purpose
        } else {
            secondary.purpose?.ifBlank { null } ?: primary.purpose
        }
        val bestPaymentMethod = primary.paymentMethod ?: secondary.paymentMethod
        val bestSourceRef = primary.sourceReference?.ifBlank { null } ?: secondary.sourceReference

        // Combine notes cleanly
        val combinedNotes = listOfNotNull(primary.note, secondary.note)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("; ")
            .ifBlank { null }

        val finalConfidence = maxOf(primary.confidence, secondary.confidence, confidenceBoost)
        val finalStatus = if (finalConfidence >= CONFIDENCE_THRESHOLD_CONFIRMED) {
            TransactionStatus.CONFIRMED.name
        } else {
            primary.status
        }

        return primary.copy(
            merchantName = bestMerchant,
            categoryId = bestCategory,
            purpose = bestPurpose,
            paymentMethod = bestPaymentMethod,
            sourceReference = bestSourceRef,
            confidence = finalConfidence,
            status = finalStatus,
            transactionDate = minOf(primary.transactionDate, secondary.transactionDate),
            note = combinedNotes,
            updatedAt = now
        )
    }
}
