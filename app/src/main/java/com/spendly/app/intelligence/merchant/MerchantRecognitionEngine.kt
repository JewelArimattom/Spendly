package com.spendly.app.intelligence.merchant

import com.spendly.app.core.database.dao.CategoryDao
import com.spendly.app.core.database.dao.MerchantRuleDao
import com.spendly.app.core.database.entity.MerchantRuleEntity
import kotlinx.coroutines.flow.first
import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

/**
 * Result of merchant recognition and categorization.
 */
data class MerchantRecognitionResult(
    val rawMerchant: String,
    val cleanName: String,
    val categoryId: String?,
    val confidence: Float,
    val isUserDefined: Boolean = false
)

/**
 * Built-in dictionary entry for known merchants.
 */
data class BuiltInMerchantEntry(
    val canonicalKey: String,
    val displayName: String,
    val categoryId: String,
    val aliases: List<String> = emptyList()
)

/**
 * Feature E: Smart Transaction Categorization Engine.
 *
 * Normalizes noisy merchant strings (stripping UPI prefixes/suffixes, payment gateways,
 * city/store codes), matches against user rules and a local dictionary of top Indian merchants,
 * and learns corrections from user feedback.
 */
class MerchantRecognitionEngine(
    private val merchantRuleDao: MerchantRuleDao,
    private val categoryDao: CategoryDao? = null
) {

    companion object {
        // Default category IDs aligned with database seed
        const val CATEGORY_FOOD = "food"
        const val CATEGORY_SHOPPING = "shopping"
        const val CATEGORY_TRAVEL = "travel"
        const val CATEGORY_FUEL = "fuel"
        const val CATEGORY_GROCERIES = "groceries"
        const val CATEGORY_BILLS = "bills"
        const val CATEGORY_ENTERTAINMENT = "entertainment"
        const val CATEGORY_HEALTH = "health"
        const val CATEGORY_EDUCATION = "education"
        const val CATEGORY_RENT = "rent"
        const val CATEGORY_TRANSFERS = "transfers"
        const val CATEGORY_OTHER = "other"

        // Regex patterns for UPI prefix/wrapper removal
        private val UPI_PREFIX_REGEX = Pattern.compile(
            "^(?:UPI(?:[-/:_]?PAYTO)?|PAYTO|VPA[:\\s]+|TO[:\\s]+|PAYMENT\\s+TO[:\\s]+|TRANSFER\\s+TO[:\\s]+)[\\s/:_-]*",
            Pattern.CASE_INSENSITIVE
        )

        // Gateway noise prefixes and suffixes
        private val GATEWAY_PREFIX_REGEX = Pattern.compile(
            "^(?:PAYU|RAZORPAY|BILLDESK|CCAVENUE|CASHFREE|PAYTM|PHONEPE|GPAY|AIRTELMONEY|JIO_MONEY)[*\\s/_#-]+",
            Pattern.CASE_INSENSITIVE
        )
        private val GATEWAY_SUFFIX_REGEX = Pattern.compile(
            "[*\\s/_#-]+(?:PAYU|RAZORPAY|BILLDESK|CCAVENUE|CASHFREE)[*\\s/_#-]*$",
            Pattern.CASE_INSENSITIVE
        )

        // UPI handle / VPA domain suffix (e.g. @okaxis, @okhdfcbank, @paytm, @ybl)
        private val UPI_HANDLE_REGEX = Pattern.compile(
            "@[a-zA-Z0-9.-]+",
            Pattern.CASE_INSENSITIVE
        )

        // Bank / UPI suffix indicators like "-OKAXIS", "-PAYTM", "/OKHDFCBANK"
        private val UPI_BANK_SUFFIX_REGEX = Pattern.compile(
            "[-/_](?:OKAXIS|OKHDFCBANK|OKICICI|OKSBI|PAYTM|APL|YBL|IBL|AXL|IDFCBANK|KOTAK|YESBANK|BARODAMPAY|UPI)\\b",
            Pattern.CASE_INSENSITIVE
        )

        // Trailing Indian city names & airport/station codes
        private val CITY_CODE_REGEX = Pattern.compile(
            "\\b(?:BANGALORE|BENGALURU|BLR|MUMBAI|BOM|DELHI|NEW\\s+DELHI|DEL|HYDERABAD|HYD|CHENNAI|MAA|PUNE|PNQ|KOLKATA|CCU|GURGAON|GURUGRAM|NOIDA|AHMEDABAD|AMD|JAIPUR|JAI|LUCKNOW|LKO|CHANDIGARH|IXC|KOCHI|COK|INDORE|IDR|BHOPAL|BHO|NAGPUR|NAG|PATNA|PAT|VADODARA|BDQ|SURAT|STV|VISAKHAPATNAM|VTZ|COIMBATORE|CJB)\\b",
            Pattern.CASE_INSENSITIVE
        )

        // Store, terminal, POS, branch indicators with numbers
        private val STORE_CODE_REGEX = Pattern.compile(
            "(?:STORE|POS|TERMINAL|STATION|OUTLET|BRANCH|BR|LOC|STN)\\s*#?\\s*\\d+",
            Pattern.CASE_INSENSITIVE
        )

        // Trailing 6-digit pin code
        private val PIN_CODE_REGEX = Pattern.compile("\\b[1-9]\\d{5}\\b")

        // Trailing hash/star identifiers (e.g. *1234, #4321, *12A)
        private val TRAILING_HASH_STAR_REGEX = Pattern.compile("[*#][a-zA-Z0-9]+$")

        // Trailing numbers at the end of merchant string
        private val TRAILING_NUMBERS_REGEX = Pattern.compile("\\s+\\d{2,}$")

        // Acronyms to keep uppercase
        private val UPPERCASE_ACRONYMS = setOf(
            "KFC", "CRED", "HPCL", "IOCL", "BPCL", "IRCTC", "JIO", "VI", "ACT", "1MG",
            "ATM", "SBI", "HDFC", "ICICI", "POS", "LLP", "LTD", "PVT", "DMART", "BESCOM", "TNEB"
        )
    }

    /**
     * Local dictionary of top Indian merchants mapped to default categories.
     */
    val builtInDictionary: List<BuiltInMerchantEntry> = listOf(
        // Food
        BuiltInMerchantEntry("ZOMATO", "Zomato", CATEGORY_FOOD, listOf("ZOMATO", "ZOMATO MEDIA", "ZOMATO RESTAURANT")),
        BuiltInMerchantEntry("SWIGGY", "Swiggy", CATEGORY_FOOD, listOf("SWIGGY", "BUNDL TECHNOLOGIES")),
        BuiltInMerchantEntry("DOMINOS", "Dominos", CATEGORY_FOOD, listOf("DOMINOS", "DOMINOS PIZZA", "JUBILANT FOODWORKS")),
        BuiltInMerchantEntry("MCDONALDS", "McDonald's", CATEGORY_FOOD, listOf("MCDONALDS", "MCDONALD", "MC DONALDS", "HARDCASTLE")),
        BuiltInMerchantEntry("STARBUCKS", "Starbucks", CATEGORY_FOOD, listOf("STARBUCKS", "TATA STARBUCKS")),
        BuiltInMerchantEntry("KFC", "KFC", CATEGORY_FOOD, listOf("KFC", "YUM BRANDS", "DEVYANI")),
        BuiltInMerchantEntry("BURGER KING", "Burger King", CATEGORY_FOOD, listOf("BURGER KING", "BURGERKING")),
        BuiltInMerchantEntry("CHAAYOS", "Chaayos", CATEGORY_FOOD, listOf("CHAAYOS", "SUNSHINE TEAHOUSE")),
        BuiltInMerchantEntry("DUNZO", "Dunzo", CATEGORY_FOOD, listOf("DUNZO", "DUNZO DIGITAL")),

        // Shopping
        BuiltInMerchantEntry("AMAZON", "Amazon", CATEGORY_SHOPPING, listOf("AMAZON", "AMZN", "AMAZON PAY", "AMAZON SELLER")),
        BuiltInMerchantEntry("FLIPKART", "Flipkart", CATEGORY_SHOPPING, listOf("FLIPKART", "FLIPKART PAYMENTS")),
        BuiltInMerchantEntry("MYNTRA", "Myntra", CATEGORY_SHOPPING, listOf("MYNTRA", "MYNTRA DESIGNS")),
        BuiltInMerchantEntry("AJIO", "Ajio", CATEGORY_SHOPPING, listOf("AJIO", "RELIANCE RETAIL AJIO")),
        BuiltInMerchantEntry("MEESHO", "Meesho", CATEGORY_SHOPPING, listOf("MEESHO", "FASHNEAR")),
        BuiltInMerchantEntry("NYKAA", "Nykaa", CATEGORY_SHOPPING, listOf("NYKAA", "FSN E-COMMERCE")),
        BuiltInMerchantEntry("TATA CLIQ", "Tata CLiQ", CATEGORY_SHOPPING, listOf("TATA CLIQ", "TATACLIQ", "TATA UNISTORE")),
        BuiltInMerchantEntry("RELIANCE DIGITAL", "Reliance Digital", CATEGORY_SHOPPING, listOf("RELIANCE DIGITAL", "RELIANCE RETAIL DIGITAL")),

        // Travel
        BuiltInMerchantEntry("UBER", "Uber", CATEGORY_TRAVEL, listOf("UBER", "UBER INDIA", "UBER BV")),
        BuiltInMerchantEntry("OLA", "Ola", CATEGORY_TRAVEL, listOf("OLA", "ANI TECHNOLOGIES", "OLA CABS")),
        BuiltInMerchantEntry("RAPIDO", "Rapido", CATEGORY_TRAVEL, listOf("RAPIDO", "ROPPEN TRANSPORTATION")),
        BuiltInMerchantEntry("IRCTC", "IRCTC", CATEGORY_TRAVEL, listOf("IRCTC", "INDIAN RAILWAYS")),
        BuiltInMerchantEntry("MAKEMYTRIP", "MakeMyTrip", CATEGORY_TRAVEL, listOf("MAKEMYTRIP", "MMT")),
        BuiltInMerchantEntry("GOIBIBO", "Goibibo", CATEGORY_TRAVEL, listOf("GOIBIBO", "IBIBO GROUP")),
        BuiltInMerchantEntry("REDBUS", "redBus", CATEGORY_TRAVEL, listOf("REDBUS", "RED BUS")),
        BuiltInMerchantEntry("BLUSMART", "BluSmart", CATEGORY_TRAVEL, listOf("BLUSMART", "BLU SMART", "GENSOL")),

        // Fuel
        BuiltInMerchantEntry("HPCL", "HPCL", CATEGORY_FUEL, listOf("HPCL", "HINDUSTAN PETROLEUM", "HP PETROL")),
        BuiltInMerchantEntry("IOCL", "IOCL", CATEGORY_FUEL, listOf("IOCL", "INDIAN OIL", "INDIAN OIL CORP", "INDIANOIL")),
        BuiltInMerchantEntry("BPCL", "BPCL", CATEGORY_FUEL, listOf("BPCL", "BHARAT PETROLEUM", "BHARAT PETROLEUM CORP")),
        BuiltInMerchantEntry("INDIAN OIL", "Indian Oil", CATEGORY_FUEL, listOf("INDIAN OIL", "INDIANOIL", "IOCL")),
        BuiltInMerchantEntry("BHARAT PETROLEUM", "Bharat Petroleum", CATEGORY_FUEL, listOf("BHARAT PETROLEUM", "BPCL")),
        BuiltInMerchantEntry("SHELL", "Shell", CATEGORY_FUEL, listOf("SHELL", "SHELL INDIA", "PIL")),

        // Groceries
        BuiltInMerchantEntry("BLINKIT", "Blinkit", CATEGORY_GROCERIES, listOf("BLINKIT", "GROFERS")),
        BuiltInMerchantEntry("ZEPTO", "Zepto", CATEGORY_GROCERIES, listOf("ZEPTO", "ZEPTO INDIA", "KIRANAKART")),
        BuiltInMerchantEntry("INSTAMART", "Instamart", CATEGORY_GROCERIES, listOf("INSTAMART", "SWIGGY INSTAMART")),
        BuiltInMerchantEntry("BIGBASKET", "BigBasket", CATEGORY_GROCERIES, listOf("BIGBASKET", "BIG BASKET", "INNOVATIVE RETAIL")),
        BuiltInMerchantEntry("DMART", "DMart", CATEGORY_GROCERIES, listOf("DMART", "D-MART", "AVENUE SUPERMARTS")),
        BuiltInMerchantEntry("NATURES BASKET", "Nature's Basket", CATEGORY_GROCERIES, listOf("NATURES BASKET", "NATURE'S BASKET")),

        // Bills / Utilities
        BuiltInMerchantEntry("BESCOM", "BESCOM", CATEGORY_BILLS, listOf("BESCOM")),
        BuiltInMerchantEntry("TNEB", "TNEB", CATEGORY_BILLS, listOf("TNEB", "TANGEDCO")),
        BuiltInMerchantEntry("TATA POWER", "Tata Power", CATEGORY_BILLS, listOf("TATA POWER", "TATAPOWER")),
        BuiltInMerchantEntry("AIRTEL", "Airtel", CATEGORY_BILLS, listOf("AIRTEL", "BHARTI AIRTEL", "AIRTEL PREPAID")),
        BuiltInMerchantEntry("JIO", "Jio", CATEGORY_BILLS, listOf("JIO", "RELIANCE JIO", "JIO PREPAID", "JIO FIBER")),
        BuiltInMerchantEntry("VI", "Vi", CATEGORY_BILLS, listOf("VI", "VODAFONE", "IDEA", "VODAFONE IDEA")),
        BuiltInMerchantEntry("ACT FIBERNET", "ACT Fibernet", CATEGORY_BILLS, listOf("ACT FIBERNET", "ACT TELEVISION", "ACT BROADBAND", "ACT FIBER")),
        BuiltInMerchantEntry("CRED", "CRED", CATEGORY_BILLS, listOf("CRED", "DREAMPLUG")),

        // Entertainment
        BuiltInMerchantEntry("NETFLIX", "Netflix", CATEGORY_ENTERTAINMENT, listOf("NETFLIX", "NETFLIX ENTERTAINMENT")),
        BuiltInMerchantEntry("SPOTIFY", "Spotify", CATEGORY_ENTERTAINMENT, listOf("SPOTIFY", "SPOTIFY INDIA")),
        BuiltInMerchantEntry("PRIME VIDEO", "Prime Video", CATEGORY_ENTERTAINMENT, listOf("PRIME VIDEO", "AMAZON PRIME", "PRIMEVIDEO")),
        BuiltInMerchantEntry("BOOKMYSHOW", "BookMyShow", CATEGORY_ENTERTAINMENT, listOf("BOOKMYSHOW", "BIGTREE ENTERTAINMENT")),
        BuiltInMerchantEntry("HOTSTAR", "Hotstar", CATEGORY_ENTERTAINMENT, listOf("HOTSTAR", "DISNEY HOTSTAR", "DISNEY+ HOTSTAR", "NOVI DIGITAL")),
        BuiltInMerchantEntry("YOUTUBE", "YouTube", CATEGORY_ENTERTAINMENT, listOf("YOUTUBE", "GOOGLE YOUTUBE")),

        // Health
        BuiltInMerchantEntry("PHARMEASY", "PharmEasy", CATEGORY_HEALTH, listOf("PHARMEASY", "API HOLDINGS")),
        BuiltInMerchantEntry("1MG", "1mg", CATEGORY_HEALTH, listOf("1MG", "TATA 1MG")),
        BuiltInMerchantEntry("APOLLO PHARMACY", "Apollo Pharmacy", CATEGORY_HEALTH, listOf("APOLLO PHARMACY", "APOLLO HOSPITALS", "APOLLO")),
        BuiltInMerchantEntry("MEDPLUS", "MedPlus", CATEGORY_HEALTH, listOf("MEDPLUS", "OPTIVAL HEALTH")),
        BuiltInMerchantEntry("PRACTO", "Practo", CATEGORY_HEALTH, listOf("PRACTO", "PRACTO TECHNOLOGIES"))
    )

    /**
     * Normalizes noisy merchant strings:
     * - Strips UPI prefixes (UPI-PAYTO, VPA:, TO:, etc.)
     * - Strips UPI handle domains (@okaxis, @okhdfcbank, etc.)
     * - Strips payment gateway noise (*PAYU, *RAZORPAY, etc.)
     * - Strips trailing city/store codes (Bangalore, BLR, Store #12, *1234, etc.)
     */
    fun normalizeMerchant(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) return ""

        var s = rawMerchant.trim()

        // 1. Remove UPI prefix e.g. "UPI-PAYTO-*-OKAXIS", "UPI/1234/PAYTO/", "VPA: ..."
        s = UPI_PREFIX_REGEX.matcher(s).replaceFirst("")

        // 2. Remove gateway prefixes (e.g. "PAYU*", "RAZORPAY*", "*PAYU*")
        s = GATEWAY_PREFIX_REGEX.matcher(s).replaceFirst("")
        s = GATEWAY_SUFFIX_REGEX.matcher(s).replaceFirst("")

        // 3. Remove UPI handles (e.g. "@okhdfcbank", "@okaxis")
        s = UPI_HANDLE_REGEX.matcher(s).replaceAll("")

        // 4. Remove UPI bank suffixes like "-OKAXIS", "-PAYTM"
        s = UPI_BANK_SUFFIX_REGEX.matcher(s).replaceAll("")

        // 5. Remove store/terminal indicators
        s = STORE_CODE_REGEX.matcher(s).replaceAll("")

        // 6. Remove trailing pin codes
        s = PIN_CODE_REGEX.matcher(s).replaceAll("")

        // 7. Remove trailing hash or star identifiers (e.g. *1234, #123)
        s = TRAILING_HASH_STAR_REGEX.matcher(s).replaceAll("")

        // 8. Remove trailing city/state codes
        s = CITY_CODE_REGEX.matcher(s).replaceAll("")

        // 9. Remove trailing numbers
        s = TRAILING_NUMBERS_REGEX.matcher(s).replaceAll("")

        // 10. Clean up symbols, underscores, asterisks, hyphens, and multiple spaces
        s = s.replace(Regex("[*_~#]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '-', '/', '.', ':')

        return s
    }

    /**
     * Converts a cleaned merchant string into a human-friendly display name.
     */
    fun formatDisplayName(normalized: String): String {
        if (normalized.isBlank()) return ""

        val words = normalized.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            val upper = word.uppercase(Locale.ROOT)
            if (UPPERCASE_ACRONYMS.contains(upper)) {
                upper
            } else if (upper.equals("1MG", ignoreCase = true)) {
                "1mg"
            } else if (upper.equals("MCDONALDS", ignoreCase = true) || upper.equals("MCDONALD", ignoreCase = true)) {
                "McDonald's"
            } else {
                word.lowercase(Locale.ROOT).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }
    }

    /**
     * Recognizes merchant and infers category and confidence.
     * Priority:
     * 1. User-defined rules from MerchantRuleDao (confidence = 1.0f)
     * 2. Built-in local dictionary (confidence = 0.95f)
     * 3. Keyword / heuristic patterns (confidence = 0.70f - 0.85f)
     * 4. Unknown fallback (confidence = 0.0f)
     */
    suspend fun recognizeMerchant(rawMerchant: String?): MerchantRecognitionResult {
        if (rawMerchant.isNullOrBlank()) {
            return MerchantRecognitionResult(
                rawMerchant = "",
                cleanName = "",
                categoryId = null,
                confidence = 0.0f,
                isUserDefined = false
            )
        }

        val raw = rawMerchant.trim()
        val normalized = normalizeMerchant(raw)
        val defaultCleanName = if (normalized.isNotBlank()) formatDisplayName(normalized) else raw

        // Step 1: Prioritize user-defined rules from MerchantRuleDao
        try {
            val userRules = merchantRuleDao.getUserDefinedRules().first()
            for (rule in userRules) {
                if (matchesRulePattern(rule.merchantPattern, raw, normalized)) {
                    return MerchantRecognitionResult(
                        rawMerchant = raw,
                        cleanName = rule.preferredName?.ifBlank { null } ?: defaultCleanName,
                        categoryId = rule.categoryId,
                        confidence = 1.0f,
                        isUserDefined = true
                    )
                }
            }
        } catch (_: Exception) {
            // Proceed to dictionary if DB read fails
        }

        // Step 2: Check built-in local dictionary
        val dictMatch = findInDictionary(normalized, raw)
        if (dictMatch != null) {
            return MerchantRecognitionResult(
                rawMerchant = raw,
                cleanName = dictMatch.displayName,
                categoryId = dictMatch.categoryId,
                confidence = 0.95f,
                isUserDefined = false
            )
        }

        // Step 3: Check heuristic category keywords
        val heuristicResult = detectCategoryHeuristically(normalized.ifBlank { raw })
        if (heuristicResult != null) {
            return MerchantRecognitionResult(
                rawMerchant = raw,
                cleanName = defaultCleanName,
                categoryId = heuristicResult.first,
                confidence = heuristicResult.second,
                isUserDefined = false
            )
        }

        // Step 4: Unknown merchant fallback
        return MerchantRecognitionResult(
            rawMerchant = raw,
            cleanName = defaultCleanName,
            categoryId = null,
            confidence = 0.0f,
            isUserDefined = false
        )
    }

    /**
     * Learning mechanism:
     * Updates or inserts a user-defined MerchantRuleEntity with confidence = 1.0f.
     */
    suspend fun learnUserCorrection(
        rawMerchant: String,
        categoryId: String,
        preferredName: String? = null
    ) {
        if (rawMerchant.isBlank()) return

        val normalized = normalizeMerchant(rawMerchant)
        val pattern = if (normalized.isNotBlank()) normalized else rawMerchant.trim()
        val now = System.currentTimeMillis()

        // Check if an existing rule exists for this pattern
        val existingRules = try {
            merchantRuleDao.getUserDefinedRules().first()
        } catch (_: Exception) {
            emptyList()
        }

        val existing = existingRules.firstOrNull {
            it.merchantPattern.equals(pattern, ignoreCase = true) ||
                    it.merchantPattern.equals(rawMerchant.trim(), ignoreCase = true)
        }

        if (existing != null) {
            val updated = existing.copy(
                categoryId = categoryId,
                preferredName = preferredName?.ifBlank { null } ?: existing.preferredName,
                confidence = 1.0f,
                isUserDefined = true,
                updatedAt = now
            )
            merchantRuleDao.update(updated)
        } else {
            val newRule = MerchantRuleEntity(
                id = UUID.randomUUID().toString(),
                merchantPattern = pattern,
                categoryId = categoryId,
                preferredName = preferredName?.ifBlank { null } ?: formatDisplayName(pattern),
                confidence = 1.0f,
                isUserDefined = true,
                createdAt = now,
                updatedAt = now
            )
            merchantRuleDao.insert(newRule)
        }
    }

    private fun matchesRulePattern(pattern: String, raw: String, normalized: String): Boolean {
        val p = pattern.trim()
        if (p.isBlank()) return false

        // Exact match against raw or normalized
        if (raw.equals(p, ignoreCase = true) || normalized.equals(p, ignoreCase = true)) {
            return true
        }

        // Substring boundary match
        val regex = Regex("(?i)(^|[^a-zA-Z0-9])${Pattern.quote(p)}([^a-zA-Z0-9]|$)")
        return regex.containsMatchIn(raw) || regex.containsMatchIn(normalized)
    }

    private fun findInDictionary(normalized: String, raw: String): BuiltInMerchantEntry? {
        val normUpper = normalized.uppercase(Locale.ROOT)
        val rawUpper = raw.uppercase(Locale.ROOT)

        for (entry in builtInDictionary) {
            for (alias in entry.aliases) {
                val aliasUpper = alias.uppercase(Locale.ROOT)
                // Exact match
                if (normUpper == aliasUpper || rawUpper == aliasUpper) {
                    return entry
                }

                // Word boundary substring match
                val aliasPattern = Regex("(?i)(^|[^a-zA-Z0-9])${Pattern.quote(aliasUpper)}([^a-zA-Z0-9]|$)")
                if (aliasPattern.containsMatchIn(normUpper) || aliasPattern.containsMatchIn(rawUpper)) {
                    return entry
                }
            }
        }
        return null
    }

    private fun detectCategoryHeuristically(text: String): Pair<String, Float>? {
        val upper = text.uppercase(Locale.ROOT)

        return when {
            upper.contains(Regex("\\b(?:PETROL|DIESEL|PUMP|CNG|FUEL)\\b")) ->
                Pair(CATEGORY_FUEL, 0.85f)
            upper.contains(Regex("\\b(?:RESTAURANT|CAFE|PIZZA|BURGER|BAKERY|BIRYANI|DHABA|SWEETS|HOTEL|FOOD|DINE)\\b")) ->
                Pair(CATEGORY_FOOD, 0.80f)
            upper.contains(Regex("\\b(?:SUPERMARKET|KIRANA|PROVISION|VEGETABLES|FRUITS|GROCERY|MART)\\b")) ->
                Pair(CATEGORY_GROCERIES, 0.80f)
            upper.contains(Regex("\\b(?:PHARMACY|MEDICINE|DRUGS|HOSPITAL|CLINIC|DIAGNOSTICS|HEALTHCARE|LAB)\\b")) ->
                Pair(CATEGORY_HEALTH, 0.80f)
            upper.contains(Regex("\\b(?:CINEMA|MOVIES|THEATRE|MULTIPLEX|ENTERTAINMENT|SHOW)\\b")) ->
                Pair(CATEGORY_ENTERTAINMENT, 0.80f)
            upper.contains(Regex("\\b(?:ELECTRICITY|POWER|WATER|BROADBAND|FIBER|DTH|GAS|UTILITY|RECHARGE|BILL)\\b")) ->
                Pair(CATEGORY_BILLS, 0.80f)
            upper.contains(Regex("\\b(?:TAXI|CAB|METRO|AIRWAYS|AIRLINES|TRAVELS|TOURS|BUS)\\b")) ->
                Pair(CATEGORY_TRAVEL, 0.80f)
            upper.contains(Regex("\\b(?:CLOTHING|APPAREL|FASHION|MALL|RETAIL|STORE|BOUTIQUE)\\b")) ->
                Pair(CATEGORY_SHOPPING, 0.75f)
            else -> null
        }
    }
}
