package com.spendly.app.intelligence.reconciliation

import com.spendly.app.core.database.dao.TransactionDao
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.domain.model.TransactionStatus
import com.spendly.app.intelligence.merchant.MerchantRecognitionEngine
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionReconciliationEngineTest {

    private lateinit var transactionDao: TransactionDao
    private lateinit var merchantEngine: MerchantRecognitionEngine
    private lateinit var engine: TransactionReconciliationEngine

    @Before
    fun setup() {
        transactionDao = mockk(relaxed = true)
        merchantEngine = mockk(relaxed = true)
        coEvery { merchantEngine.normalizeMerchant(any()) } answers { firstArg<String?>() ?: "" }
        engine = TransactionReconciliationEngine(transactionDao, merchantEngine)
    }

    @Test
    fun exactExternalReferenceMatchMergesTransactionsFromDifferentSources() = runBlocking {
        val now = System.currentTimeMillis()
        val upiRef = "UPI123456789012"

        // Existing transaction from UPI app notification (e.g. PhonePe)
        val existing = TransactionEntity(
            id = "tx-1",
            amountMinorUnits = 50000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Starbucks",
            categoryId = "food",
            purpose = "Coffee",
            paymentMethod = "UPI",
            source = "NOTIFICATION",
            sourceReference = upiRef,
            confidence = 0.85f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now,
            createdAt = now,
            updatedAt = now
        )

        // Incoming transaction from Bank notification with same UTR / Ref
        val incoming = TransactionEntity(
            id = "tx-2",
            amountMinorUnits = 50000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "TATA STARBUCKS",
            categoryId = null,
            purpose = null,
            paymentMethod = "UPI",
            source = "BANK_SMS",
            sourceReference = upiRef,
            confidence = 0.80f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now + 1000L,
            createdAt = now + 1000L,
            updatedAt = now + 1000L
        )

        coEvery { transactionDao.getBySourceReference(upiRef) } returns existing

        val result = engine.reconcile(incoming)

        assertTrue(result is ReconciliationResult.Merged)
        val merged = (result as ReconciliationResult.Merged).mergedTransaction
        assertEquals("tx-1", merged.id)
        assertEquals(50000L, merged.amountMinorUnits)
        assertEquals(TransactionStatus.CONFIRMED.name, merged.status)
        assertTrue(merged.confidence >= 0.90f)

        coVerify { transactionDao.update(any()) }
    }

    @Test
    fun sameAmountAndMerchantWithinTimeWindowMergesAsDuplicate() = runBlocking {
        val now = System.currentTimeMillis()

        val existing = TransactionEntity(
            id = "tx-1",
            amountMinorUnits = 32000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Swiggy",
            categoryId = "food",
            purpose = null,
            paymentMethod = "UPI",
            source = "NOTIFICATION",
            sourceReference = null,
            confidence = 0.85f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now,
            createdAt = now,
            updatedAt = now
        )

        val incoming = TransactionEntity(
            id = "tx-2",
            amountMinorUnits = 32000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Swiggy",
            categoryId = "food",
            purpose = null,
            paymentMethod = "UPI",
            source = "UPI_SESSION",
            sourceReference = null,
            confidence = 0.80f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now + 60_000L, // 1 minute later
            createdAt = now + 60_000L,
            updatedAt = now + 60_000L
        )

        coEvery { transactionDao.getBySourceReference(any()) } returns null
        coEvery { transactionDao.getByDateRange(any(), any()) } returns flowOf(listOf(existing))
        coEvery { merchantEngine.normalizeMerchant("Swiggy") } returns "SWIGGY"

        val result = engine.reconcile(incoming)

        assertTrue("Should merge transactions with identical amount and merchant in 5-minute window", result is ReconciliationResult.Merged)
    }

    @Test
    fun ambiguousMatchWithinCandidateWindowIsFlaggedAsDuplicateCandidate() = runBlocking {
        val now = System.currentTimeMillis()

        // Same amount, but different merchant names within 2 minutes
        val existing = TransactionEntity(
            id = "tx-1",
            amountMinorUnits = 25000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Store A",
            categoryId = null,
            purpose = null,
            paymentMethod = "UPI",
            source = "NOTIFICATION",
            sourceReference = null,
            confidence = 0.70f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now,
            createdAt = now,
            updatedAt = now
        )

        val incoming = TransactionEntity(
            id = "tx-2",
            amountMinorUnits = 25000L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Store B",
            categoryId = null,
            purpose = null,
            paymentMethod = "UPI",
            source = "BANK_SMS",
            sourceReference = null,
            confidence = 0.70f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now + 45_000L, // 45 seconds later
            createdAt = now + 45_000L,
            updatedAt = now + 45_000L
        )

        coEvery { transactionDao.getBySourceReference(any()) } returns null
        coEvery { transactionDao.getByDateRange(any(), any()) } returns flowOf(listOf(existing))
        coEvery { merchantEngine.normalizeMerchant("Store A") } returns "STORE A"
        coEvery { merchantEngine.normalizeMerchant("Store B") } returns "STORE B"

        val result = engine.reconcile(incoming)

        // Must not automatically merge without matching merchant or ref
        assertTrue("Ambiguous match should be flagged as DuplicateCandidate for manual user review", result is ReconciliationResult.DuplicateCandidate)
    }

    @Test
    fun refundCreditMatchesDebitAndMarksBothRefunded() = runBlocking {
        val now = System.currentTimeMillis()
        val originalRef = "REF9988776655"

        val debitTx = TransactionEntity(
            id = "debit-1",
            amountMinorUnits = 99900L,
            currency = "INR",
            transactionType = "DEBIT",
            merchantName = "Amazon",
            categoryId = "shopping",
            purpose = null,
            paymentMethod = "UPI",
            source = "NOTIFICATION",
            sourceReference = originalRef,
            confidence = 0.95f,
            status = TransactionStatus.CONFIRMED.name,
            transactionDate = now - 3600_000L,
            createdAt = now - 3600_000L,
            updatedAt = now - 3600_000L
        )

        val refundIncoming = TransactionEntity(
            id = "refund-1",
            amountMinorUnits = 99900L,
            currency = "INR",
            transactionType = "REFUND",
            merchantName = "Amazon India",
            categoryId = "shopping",
            purpose = "Return item",
            paymentMethod = "UPI",
            source = "NOTIFICATION",
            sourceReference = originalRef,
            confidence = 0.90f,
            status = TransactionStatus.DETECTED.name,
            transactionDate = now,
            createdAt = now,
            updatedAt = now
        )

        coEvery { transactionDao.getBySourceReference(originalRef) } returns debitTx

        val result = engine.reconcile(refundIncoming)

        assertTrue(result is ReconciliationResult.RefundMatched)
        val matched = result as ReconciliationResult.RefundMatched
        assertEquals(TransactionStatus.REFUNDED.name, matched.originalTransaction.status)
        assertEquals(TransactionStatus.REFUNDED.name, matched.refundTransaction.status)

        coVerify { transactionDao.update(match { it.id == "debit-1" && it.status == TransactionStatus.REFUNDED.name }) }
        coVerify { transactionDao.insert(match { it.id == "refund-1" && it.status == TransactionStatus.REFUNDED.name }) }
    }
}
