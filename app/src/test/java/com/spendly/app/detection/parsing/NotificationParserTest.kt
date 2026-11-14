package com.spendly.app.detection.parsing

import com.spendly.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationParserTest {

    private lateinit var parser: NotificationTransactionParser

    @Before
    fun setup() {
        parser = NotificationTransactionParser()
    }

    @Test
    fun parseGooglePayDebitNotification() {
        val title = "Google Pay"
        val text = "Paid ₹450.00 to Starbucks Coffee. UPI Ref: 412345678901"
        val packageName = "com.google.android.apps.nbu.paisa.user"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(45000L, candidate?.amountMinorUnits)
        assertTrue(candidate?.merchantName?.contains("Starbucks", ignoreCase = true) == true)
        assertEquals(TransactionType.DEBIT, candidate?.transactionType)
        assertEquals("412345678901", candidate?.referenceId)
        assertTrue((candidate?.confidence ?: 0f) >= 0.85f)
    }

    @Test
    fun parsePhonePeDebitNotification() {
        val title = "PhonePe"
        val text = "₹1,250.00 paid to ZOMATO. Transaction ID T241114221100."
        val packageName = "com.phonepe.app"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(125000L, candidate?.amountMinorUnits)
        assertTrue(candidate?.merchantName?.contains("ZOMATO", ignoreCase = true) == true)
        assertEquals(TransactionType.DEBIT, candidate?.transactionType)
    }

    @Test
    fun parsePaytmPaymentNotification() {
        val title = "Paytm Payments"
        val text = "Money sent! Paid Rs. 320 to Swiggy Bangalore via UPI. UTR: 432198765432"
        val packageName = "net.one97.paytm"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(32000L, candidate?.amountMinorUnits)
        assertTrue(candidate?.merchantName?.contains("Swiggy", ignoreCase = true) == true)
        assertEquals(TransactionType.DEBIT, candidate?.transactionType)
        assertEquals("432198765432", candidate?.referenceId)
    }

    @Test
    fun parseBhimCreditNotification() {
        val title = "BHIM UPI"
        val text = "Received Rs 2500.00 from John Doe in your account. Ref: 987654321012"
        val packageName = "in.org.npci.upiapp"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(250000L, candidate?.amountMinorUnits)
        assertEquals(TransactionType.CREDIT, candidate?.transactionType)
    }

    @Test
    fun parseHdfcBankDebitAlert() {
        val title = "HDFC Bank Alert"
        val text = "Alert: Rs. 1499.00 debited from HDFC Bank A/c ending 4589 on 14-Nov-26 to UBER INDIA. UPI Ref 498765123456."
        val packageName = "com.snapwork.hdfc"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(149900L, candidate?.amountMinorUnits)
        assertTrue(candidate?.merchantName?.contains("UBER", ignoreCase = true) == true)
        assertEquals(TransactionType.DEBIT, candidate?.transactionType)
        assertEquals("498765123456", candidate?.referenceId)
    }

    @Test
    fun parseSbiDebitNotification() {
        val title = "SBI"
        val text = "Dear SBI User, A/c *1234 debited by Rs 850.50 on 14Nov26 transfer to RAPIDO. UPI: 412987345612."
        val packageName = "com.sbi.upi"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(85050L, candidate?.amountMinorUnits)
        assertTrue(candidate?.merchantName?.contains("RAPIDO", ignoreCase = true) == true)
        assertEquals(TransactionType.DEBIT, candidate?.transactionType)
    }

    @Test
    fun parseFailedTransactionNotification() {
        val title = "PhonePe"
        val text = "Payment of ₹600.00 to Dominos Pizza has failed. Amount not debited."
        val packageName = "com.phonepe.app"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        if (candidate != null) {
            assertTrue(candidate.isFailed)
        } else {
            // Filtered as non-financial or failed payment
            assertNull(candidate)
        }
    }

    @Test
    fun parseRefundNotification() {
        val title = "Google Pay"
        val text = "Refund of ₹350.00 from Amazon India has been credited. Ref: 456789123000"
        val packageName = "com.google.android.apps.nbu.paisa.user"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())

        assertNotNull(candidate)
        assertEquals(35000L, candidate?.amountMinorUnits)
        assertEquals(TransactionType.REFUND, candidate?.transactionType)
    }

    @Test
    fun filterIgnoresPromotionalAndOtpNotifications() {
        // Promotional
        assertTrue(NotificationFilter.isNonFinancial("PhonePe", "Cashback of up to ₹50 on your next recharge!"))
        assertTrue(NotificationFilter.isNonFinancial("Google Pay", "Spin the wheel to win exciting offers!"))
        assertTrue(NotificationFilter.isNonFinancial("Paytm", "Flat 50% off on movie tickets this weekend"))

        // OTP / Security
        assertTrue(NotificationFilter.isNonFinancial("HDFC Bank", "123456 is your secret OTP for login. Do not share."))
        assertTrue(NotificationFilter.isNonFinancial("SBI", "Your NetBanking login password has been changed."))

        // Genuine financial alerts should NOT be ignored
        assertFalse(NotificationFilter.isNonFinancial("Google Pay", "Paid ₹150 to Chai Point. UPI Ref: 123456"))
        assertFalse(NotificationFilter.isNonFinancial("PhonePe", "₹500 debited for Netflix subscription."))
    }

    @Test
    fun parserHandlesMissingAmountGracefully() {
        val title = "Google Pay"
        val text = "Payment to John was completed successfully."
        val packageName = "com.google.android.apps.nbu.paisa.user"

        val candidate = parser.parseNotification(packageName, title, text, System.currentTimeMillis())
        assertNull("Transactions with no parseable amount should return null candidate", candidate)
    }
}
