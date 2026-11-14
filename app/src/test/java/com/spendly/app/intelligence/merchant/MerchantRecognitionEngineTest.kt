package com.spendly.app.intelligence.merchant

import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.entity.MerchantRuleEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MerchantRecognitionEngineTest {

    private lateinit var merchantRuleDao: MerchantRuleDao
    private lateinit var engine: MerchantRecognitionEngine

    @Before
    fun setup() {
        merchantRuleDao = mockk(relaxed = true)
        coEvery { merchantRuleDao.getUserDefinedRules() } returns flowOf(emptyList())
        engine = MerchantRecognitionEngine(merchantRuleDao)
    }

    @Test
    fun normalizeMerchantStripsUpiPrefixesAndGateways() {
        assertEquals("ZOMATO", engine.normalizeMerchant("UPI-PAYTO-ZOMATO-OKAXIS").uppercase())
        assertEquals("SWIGGY", engine.normalizeMerchant("PAYU*SWIGGY BANGALORE").uppercase())
        assertEquals("UBER INDIA", engine.normalizeMerchant("VPA: UBER INDIA").uppercase())
        assertEquals("AMZN MKTP", engine.normalizeMerchant("AMZN MKTP*1234").uppercase())
        assertEquals("STARBUCKS", engine.normalizeMerchant("STARBUCKS STORE#456").uppercase())
    }

    @Test
    fun formatDisplayNameProducesCleanTitleCase() {
        assertEquals("Zomato", engine.formatDisplayName("ZOMATO"))
        assertEquals("Swiggy", engine.formatDisplayName("SWIGGY"))
        assertEquals("McDonald's", engine.formatDisplayName("MCDONALDS"))
        assertEquals("KFC", engine.formatDisplayName("KFC"))
        assertEquals("Uber", engine.formatDisplayName("UBER"))
    }

    @Test
    fun recognizeBuiltInMerchantsWithHighConfidence() = runBlocking {
        val zomatoResult = engine.recognizeMerchant("ZOMATO*1234")
        assertEquals("Zomato", zomatoResult.cleanName)
        assertEquals(MerchantRecognitionEngine.CATEGORY_FOOD, zomatoResult.categoryId)
        assertEquals(0.95f, zomatoResult.confidence, 0.01f)
        assertFalse(zomatoResult.isUserDefined)

        val uberResult = engine.recognizeMerchant("UBER INDIA")
        assertEquals("Uber", uberResult.cleanName)
        assertEquals(MerchantRecognitionEngine.CATEGORY_TRAVEL, uberResult.categoryId)
        assertEquals(0.95f, uberResult.confidence, 0.01f)

        val amazonResult = engine.recognizeMerchant("AMAZON PAY INDIA")
        assertEquals("Amazon", amazonResult.cleanName)
        assertEquals(MerchantRecognitionEngine.CATEGORY_SHOPPING, amazonResult.categoryId)
        assertEquals(0.95f, amazonResult.confidence, 0.01f)

        val hpclResult = engine.recognizeMerchant("HPCL PETROL PUMP")
        assertEquals("HPCL", hpclResult.cleanName)
        assertEquals(MerchantRecognitionEngine.CATEGORY_FUEL, hpclResult.categoryId)
        assertEquals(0.95f, hpclResult.confidence, 0.01f)
    }

    @Test
    fun userDefinedRulesOverrideBuiltInDictionaryWithConfidenceOne() = runBlocking {
        // User customized "Zomato" to be considered "entertainment" or a custom category
        val customRule = MerchantRuleEntity(
            id = "rule-1",
            merchantPattern = "ZOMATO",
            categoryId = "custom_cat_treats",
            preferredName = "Zomato Night Out",
            confidence = 1.0f,
            isUserDefined = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        coEvery { merchantRuleDao.getUserDefinedRules() } returns flowOf(listOf(customRule))

        val result = engine.recognizeMerchant("ZOMATO BANGALORE")

        assertEquals("Zomato Night Out", result.cleanName)
        assertEquals("custom_cat_treats", result.categoryId)
        assertEquals(1.0f, result.confidence, 0.001f)
        assertTrue(result.isUserDefined)
    }

    @Test
    fun learnUserCorrectionSavesRuleToDao() = runBlocking {
        engine.learnUserCorrection(
            rawMerchant = "LOCAL BAKERY*99",
            categoryId = MerchantRecognitionEngine.CATEGORY_FOOD,
            preferredName = "Local Bakery"
        )

        coVerify {
            merchantRuleDao.insert(match {
                it.isUserDefined &&
                        it.categoryId == MerchantRecognitionEngine.CATEGORY_FOOD &&
                        it.preferredName == "Local Bakery" &&
                        it.confidence == 1.0f
            })
        }
    }
}
