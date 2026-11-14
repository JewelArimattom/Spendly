package com.spendly.app.core.security

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.spendly.app.core.database.SpendlyDatabase
import com.spendly.app.core.database.entity.AppSettingsEntity
import com.spendly.app.core.database.entity.BudgetEntity
import com.spendly.app.core.database.entity.CategoryEntity
import com.spendly.app.core.database.entity.MerchantRuleEntity
import com.spendly.app.core.database.entity.ReminderPreferenceEntity
import com.spendly.app.core.database.entity.TransactionEntity
import com.spendly.app.core.database.entity.UpiSessionEntity
import com.spendly.app.domain.repository.AppSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

data class BackupRestoreSummary(
    val transactionCount: Int,
    val categoryCount: Int,
    val merchantRuleCount: Int,
    val budgetCount: Int,
    val settingsRestored: Boolean,
    val upiSessionCount: Int = 0,
    val reminderPreferenceCount: Int = 0,
    val restoredAt: Long = System.currentTimeMillis()
)

@Singleton
class LocalBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SpendlyDatabase,
    private val appSettingsRepository: AppSettingsRepository
) {

    companion object {
        private const val MAGIC_HEADER = "SPNDBK01"
        private val MAGIC_BYTES = MAGIC_HEADER.toByteArray(Charsets.US_ASCII)
        private const val BACKUP_VERSION = 1
        private const val SALT_LENGTH_BYTES = 16
        private const val IV_LENGTH_BYTES = 12
        private const val CHECKSUM_LENGTH_BYTES = 32
        private const val PBKDF2_ITERATIONS = 100_000
        private const val AES_KEY_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    }

    /**
     * Exports an encrypted, zero-cloud local backup to the destination URI.
     * Uses PBKDF2WithHmacSHA256 key derivation and AES/GCM/NoPadding encryption.
     */
    suspend fun exportBackup(
        destinationUri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotEmpty()) { "Backup password cannot be empty" }

            // 1. Gather all data from database
            val transactions = database.transactionDao().getAllSync()
            val categories = database.categoryDao().getAllSync()
            val merchantRules = database.merchantRuleDao().getAllSync()
            val budgets = database.budgetDao().getAllSync()
            val settings = database.appSettingsDao().getSettingsSync()
            val sessions = database.upiSessionDao().getAllSync()
            val reminderPrefs = database.reminderPreferenceDao().getAllSync()

            // 2. Build JSON payload
            val rootJson = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())

                put("transactions", JSONArray().apply {
                    transactions.forEach { put(it.toJson()) }
                })
                put("categories", JSONArray().apply {
                    categories.forEach { put(it.toJson()) }
                })
                put("merchantRules", JSONArray().apply {
                    merchantRules.forEach { put(it.toJson()) }
                })
                put("budgets", JSONArray().apply {
                    budgets.forEach { put(it.toJson()) }
                })
                put("settings", settings?.toJson() ?: JSONObject.NULL)
                put("upiSessions", JSONArray().apply {
                    sessions.forEach { put(it.toJson()) }
                })
                put("reminderPreferences", JSONArray().apply {
                    reminderPrefs.forEach { put(it.toJson()) }
                })
            }

            val plaintextBytes = rootJson.toString().toByteArray(Charsets.UTF_8)

            // 3. Compute SHA-256 checksum of plaintext
            val sha256 = MessageDigest.getInstance("SHA-256")
            val checksum = sha256.digest(plaintextBytes)

            // 4. Generate random salt and IV
            val random = SecureRandom()
            val salt = ByteArray(SALT_LENGTH_BYTES).also { random.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH_BYTES).also { random.nextBytes(it) }

            // 5. Derive 256-bit AES key using PBKDF2WithHmacSHA256
            val secretKey = deriveAesKey(password, salt)

            // 6. Encrypt plaintext payload with AES/GCM/NoPadding
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            val ciphertext = cipher.doFinal(plaintextBytes)

            // 7. Write versioned binary backup format to destination Uri
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: throw IllegalStateException("Could not open output stream for destination: $destinationUri")

            outputStream.use { rawOut ->
                val dos = DataOutputStream(rawOut)
                dos.write(MAGIC_BYTES)                  // 8 bytes
                dos.writeInt(BACKUP_VERSION)            // 4 bytes
                dos.write(salt)                         // 16 bytes
                dos.write(iv)                           // 12 bytes
                dos.write(checksum)                     // 32 bytes
                dos.write(ciphertext)                   // Remaining bytes
                dos.flush()
            }
        }
    }

    /**
     * Restores an encrypted local backup from the source URI.
     * Validates magic bytes, decrypts payload, verifies checksum, validates JSON schema,
     * and atomically replaces or merges records into the Room database.
     */
    suspend fun restoreBackup(
        sourceUri: Uri,
        password: CharArray,
        merge: Boolean = false
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotEmpty()) { "Password cannot be empty" }

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: throw IllegalStateException("Could not open input stream for source: $sourceUri")

            // 1. Read binary header
            val magic = ByteArray(MAGIC_BYTES.size)
            val version: Int
            val salt = ByteArray(SALT_LENGTH_BYTES)
            val iv = ByteArray(IV_LENGTH_BYTES)
            val expectedChecksum = ByteArray(CHECKSUM_LENGTH_BYTES)
            val ciphertext: ByteArray

            inputStream.use { rawIn ->
                val dis = DataInputStream(rawIn)
                dis.readFully(magic)
                if (!magic.contentEquals(MAGIC_BYTES)) {
                    throw IllegalArgumentException("Invalid backup file: header magic bytes do not match Spendly format.")
                }

                version = dis.readInt()
                if (version != BACKUP_VERSION) {
                    throw IllegalArgumentException("Unsupported backup version: $version (expected $BACKUP_VERSION).")
                }

                dis.readFully(salt)
                dis.readFully(iv)
                dis.readFully(expectedChecksum)

                // Read remaining bytes as ciphertext
                val buffer = ByteArrayOutputStream()
                val chunk = ByteArray(4096)
                var bytesRead: Int
                while (dis.read(chunk).also { bytesRead = it } != -1) {
                    buffer.write(chunk, 0, bytesRead)
                }
                ciphertext = buffer.toByteArray()
            }

            if (ciphertext.isEmpty()) {
                throw IllegalArgumentException("Corrupted backup file: ciphertext is empty.")
            }

            // 2. Derive key and decrypt payload
            val secretKey = deriveAesKey(password, salt)
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val decryptedBytes = try {
                cipher.doFinal(ciphertext)
            } catch (e: Exception) {
                throw SecurityException("Decryption failed: Incorrect password or corrupted backup payload.", e)
            }

            // 3. Verify SHA-256 checksum of decrypted plaintext
            val actualChecksum = MessageDigest.getInstance("SHA-256").digest(decryptedBytes)
            if (!MessageDigest.isEqual(actualChecksum, expectedChecksum)) {
                throw SecurityException("Integrity check failed: Plaintext checksum mismatch.")
            }

            // 4. Validate JSON Schema
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val rootJson = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JSON format in decrypted backup.", e)
            }

            if (!rootJson.has("version") || !rootJson.has("transactions") || !rootJson.has("categories")) {
                throw IllegalArgumentException("Invalid backup schema: Missing required top-level fields.")
            }

            // Parse entities
            val transactionsList = mutableListOf<TransactionEntity>()
            val transactionsArray = rootJson.optJSONArray("transactions")
            if (transactionsArray != null) {
                for (i in 0 until transactionsArray.length()) {
                    transactionsList.add(transactionsArray.getJSONObject(i).toTransactionEntity())
                }
            }

            val categoriesList = mutableListOf<CategoryEntity>()
            val categoriesArray = rootJson.optJSONArray("categories")
            if (categoriesArray != null) {
                for (i in 0 until categoriesArray.length()) {
                    categoriesList.add(categoriesArray.getJSONObject(i).toCategoryEntity())
                }
            }

            val merchantRulesList = mutableListOf<MerchantRuleEntity>()
            val rulesArray = rootJson.optJSONArray("merchantRules")
            if (rulesArray != null) {
                for (i in 0 until rulesArray.length()) {
                    merchantRulesList.add(rulesArray.getJSONObject(i).toMerchantRuleEntity())
                }
            }

            val budgetsList = mutableListOf<BudgetEntity>()
            val budgetsArray = rootJson.optJSONArray("budgets")
            if (budgetsArray != null) {
                for (i in 0 until budgetsArray.length()) {
                    budgetsList.add(budgetsArray.getJSONObject(i).toBudgetEntity())
                }
            }

            val sessionsList = mutableListOf<UpiSessionEntity>()
            val sessionsArray = rootJson.optJSONArray("upiSessions")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    sessionsList.add(sessionsArray.getJSONObject(i).toUpiSessionEntity())
                }
            }

            val reminderPrefsList = mutableListOf<ReminderPreferenceEntity>()
            val reminderPrefsArray = rootJson.optJSONArray("reminderPreferences")
            if (reminderPrefsArray != null) {
                for (i in 0 until reminderPrefsArray.length()) {
                    reminderPrefsList.add(reminderPrefsArray.getJSONObject(i).toReminderPreferenceEntity())
                }
            }

            val settingsEntity = if (rootJson.has("settings") && !rootJson.isNull("settings")) {
                rootJson.getJSONObject("settings").toAppSettingsEntity()
            } else null

            // 5. Atomic Room database transaction
            database.withTransaction {
                if (!merge) {
                    database.transactionDao().deleteAll()
                    database.categoryDao().deleteAll()
                    database.merchantRuleDao().deleteAll()
                    database.budgetDao().deleteAll()
                    database.upiSessionDao().deleteAll()
                    database.reminderPreferenceDao().deleteAll()
                    if (settingsEntity != null) {
                        database.appSettingsDao().deleteAll()
                    }
                }

                if (categoriesList.isNotEmpty()) {
                    database.categoryDao().insertAll(categoriesList)
                }
                if (transactionsList.isNotEmpty()) {
                    database.transactionDao().insertAll(transactionsList)
                }
                if (merchantRulesList.isNotEmpty()) {
                    database.merchantRuleDao().insertAll(merchantRulesList)
                }
                if (budgetsList.isNotEmpty()) {
                    database.budgetDao().insertAll(budgetsList)
                }
                if (sessionsList.isNotEmpty()) {
                    database.upiSessionDao().insertAll(sessionsList)
                }
                if (reminderPrefsList.isNotEmpty()) {
                    database.reminderPreferenceDao().insertAll(reminderPrefsList)
                }
                if (settingsEntity != null) {
                    database.appSettingsDao().insertOrUpdate(settingsEntity)
                }
            }

            BackupRestoreSummary(
                transactionCount = transactionsList.size,
                categoryCount = categoriesList.size,
                merchantRuleCount = merchantRulesList.size,
                budgetCount = budgetsList.size,
                settingsRestored = settingsEntity != null,
                upiSessionCount = sessionsList.size,
                reminderPreferenceCount = reminderPrefsList.size,
                restoredAt = System.currentTimeMillis()
            )
        }
    }

    private fun deriveAesKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        val keyFactory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val keyBytes = keyFactory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    // JSON Mappers for Entities

    private fun TransactionEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("amountMinorUnits", amountMinorUnits)
        put("currency", currency)
        put("transactionType", transactionType)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("merchantName", merchantName ?: JSONObject.NULL)
        put("purpose", purpose ?: JSONObject.NULL)
        put("paymentMethod", paymentMethod ?: JSONObject.NULL)
        put("source", source)
        put("sourceReference", sourceReference ?: JSONObject.NULL)
        put("confidence", confidence.toDouble())
        put("status", status)
        put("transactionDate", transactionDate)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
        put("note", note ?: JSONObject.NULL)
    }

    private fun JSONObject.toTransactionEntity(): TransactionEntity = TransactionEntity(
        id = getString("id"),
        amountMinorUnits = getLong("amountMinorUnits"),
        currency = optString("currency", "INR"),
        transactionType = getString("transactionType"),
        merchantName = optNullableString("merchantName"),
        categoryId = optNullableString("categoryId"),
        purpose = optNullableString("purpose"),
        paymentMethod = optNullableString("paymentMethod"),
        source = optString("source", "MANUAL"),
        sourceReference = optNullableString("sourceReference"),
        confidence = optDouble("confidence", 1.0).toFloat(),
        status = optString("status", "CONFIRMED"),
        transactionDate = getLong("transactionDate"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
        note = optNullableString("note")
    )

    private fun CategoryEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("icon", icon)
        put("color", color)
        put("isDefault", isDefault)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toCategoryEntity(): CategoryEntity = CategoryEntity(
        id = getString("id"),
        name = getString("name"),
        icon = getString("icon"),
        color = getLong("color"),
        isDefault = optBoolean("isDefault", false),
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )

    private fun MerchantRuleEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("merchantPattern", merchantPattern)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("preferredName", preferredName ?: JSONObject.NULL)
        put("confidence", confidence.toDouble())
        put("isUserDefined", isUserDefined)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toMerchantRuleEntity(): MerchantRuleEntity = MerchantRuleEntity(
        id = getString("id"),
        merchantPattern = getString("merchantPattern"),
        categoryId = optNullableString("categoryId"),
        preferredName = optNullableString("preferredName"),
        confidence = optDouble("confidence", 1.0).toFloat(),
        isUserDefined = optBoolean("isUserDefined", true),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun BudgetEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("categoryId", categoryId ?: JSONObject.NULL)
        put("amountMinorUnits", amountMinorUnits)
        put("period", period)
        put("startDate", startDate)
        put("endDate", endDate ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toBudgetEntity(): BudgetEntity = BudgetEntity(
        id = getString("id"),
        categoryId = optNullableString("categoryId"),
        amountMinorUnits = getLong("amountMinorUnits"),
        period = getString("period"),
        startDate = getLong("startDate"),
        endDate = if (has("endDate") && !isNull("endDate")) getLong("endDate") else null,
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun AppSettingsEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("currency", currency)
        put("currencySymbol", currencySymbol)
        put("theme", theme)
        put("biometricEnabled", biometricEnabled)
        put("pinHash", pinHash ?: JSONObject.NULL)
        put("autoLockOnBackground", autoLockOnBackground)
        put("usageMonitoringEnabled", usageMonitoringEnabled)
        put("notificationMonitoringEnabled", notificationMonitoringEnabled)
        put("onboardingCompleted", onboardingCompleted)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toAppSettingsEntity(): AppSettingsEntity = AppSettingsEntity(
        id = optString("id", "default"),
        currency = optString("currency", "INR"),
        currencySymbol = optString("currencySymbol", "₹"),
        theme = optString("theme", "system"),
        biometricEnabled = optBoolean("biometricEnabled", false),
        pinHash = optNullableString("pinHash"),
        autoLockOnBackground = optBoolean("autoLockOnBackground", false),
        usageMonitoringEnabled = optBoolean("usageMonitoringEnabled", true),
        notificationMonitoringEnabled = optBoolean("notificationMonitoringEnabled", true),
        onboardingCompleted = optBoolean("onboardingCompleted", false),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun UpiSessionEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("packageName", packageName)
        put("startedAt", startedAt)
        put("endedAt", endedAt ?: JSONObject.NULL)
        put("sessionStatus", sessionStatus)
        put("reminderStatus", reminderStatus)
        put("associatedTransactionIds", associatedTransactionIds ?: JSONObject.NULL)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toUpiSessionEntity(): UpiSessionEntity = UpiSessionEntity(
        id = getString("id"),
        packageName = getString("packageName"),
        startedAt = getLong("startedAt"),
        endedAt = if (has("endedAt") && !isNull("endedAt")) getLong("endedAt") else null,
        sessionStatus = optString("sessionStatus", "ENDED"),
        reminderStatus = optString("reminderStatus", "NOT_NEEDED"),
        associatedTransactionIds = optNullableString("associatedTransactionIds"),
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )

    private fun ReminderPreferenceEntity.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("packageName", packageName)
        put("appName", appName)
        put("enabled", enabled)
        put("cooldownMinutes", cooldownMinutes)
        put("maximumDailyReminders", maximumDailyReminders)
        put("quietHoursStart", quietHoursStart ?: JSONObject.NULL)
        put("quietHoursEnd", quietHoursEnd ?: JSONObject.NULL)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toReminderPreferenceEntity(): ReminderPreferenceEntity = ReminderPreferenceEntity(
        id = getString("id"),
        packageName = getString("packageName"),
        appName = getString("appName"),
        enabled = optBoolean("enabled", true),
        cooldownMinutes = optInt("cooldownMinutes", 30),
        maximumDailyReminders = optInt("maximumDailyReminders", 5),
        quietHoursStart = if (has("quietHoursStart") && !isNull("quietHoursStart")) getInt("quietHoursStart") else null,
        quietHoursEnd = if (has("quietHoursEnd") && !isNull("quietHoursEnd")) getInt("quietHoursEnd") else null,
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key) else null
    }
}
