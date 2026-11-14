package com.spendly.app.core.security

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendly.app.core.database.SpendlyDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedDatabaseManager @Inject constructor(
    private val keyManager: DatabaseKeyManager
) {

    companion object {
        private const val DB_NAME = "spendly.db"
        private const val PREFS_NAME = "spendly_db_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        private const val PASSPHRASE_LENGTH = 32

        init {
            System.loadLibrary("sqlcipher")
        }
    }

    @Volatile
    private var database: SpendlyDatabase? = null

    fun getDatabase(context: Context): SpendlyDatabase {
        return database ?: synchronized(this) {
            database ?: buildDatabase(context).also { database = it }
        }
    }

    private fun buildDatabase(context: Context): SpendlyDatabase {
        val passphrase = getOrCreatePassphrase(context)
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            SpendlyDatabase::class.java,
            DB_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    seedDatabase(db)
                }
            })
            .build()
    }

    private fun seedDatabase(db: SupportSQLiteDatabase) {
        val currentTime = System.currentTimeMillis()

        // 12 default categories
        val defaultCategories = listOf(
            Triple("Food", "restaurant", 0xFFFF5722L),
            Triple("Shopping", "shopping_bag", 0xFFE91E63L),
            Triple("Travel", "flight", 0xFF2196F3L),
            Triple("Fuel", "local_gas_station", 0xFFFF9800L),
            Triple("Bills", "receipt_long", 0xFF9C27B0L),
            Triple("Entertainment", "movie", 0xFF673AB7L),
            Triple("Health", "medical_services", 0xFF4CAF50L),
            Triple("Education", "school", 0xFF00BCD4L),
            Triple("Rent", "home", 0xFF795548L),
            Triple("Groceries", "local_grocery_store", 0xFF8BC34AL),
            Triple("Transfers", "swap_horiz", 0xFF009688L),
            Triple("Other", "more_horiz", 0xFF607D8BL)
        )

        for ((name, icon, color) in defaultCategories) {
            val id = name.lowercase()
            db.execSQL(
                "INSERT OR IGNORE INTO categories (id, name, icon, color, is_default, created_at) " +
                        "VALUES ('$id', '$name', '$icon', $color, 1, $currentTime)"
            )
        }

        // Default UPI apps reminder preferences
        val defaultApps = listOf(
            Pair("com.google.android.apps.nbu.paisa.user", "Google Pay"),
            Pair("com.phonepe.app", "PhonePe"),
            Pair("net.one97.paytm", "Paytm"),
            Pair("in.org.npci.upiapp", "BHIM")
        )

        for ((pkg, appName) in defaultApps) {
            db.execSQL(
                "INSERT OR IGNORE INTO reminder_preferences (id, package_name, app_name, enabled, cooldown_minutes, maximum_daily_reminders, created_at, updated_at) " +
                        "VALUES ('$pkg', '$pkg', '$appName', 1, 30, 5, $currentTime, $currentTime)"
            )
        }

        // Default app settings
        db.execSQL(
            "INSERT OR IGNORE INTO app_settings (id, currency, currency_symbol, theme, biometric_enabled, auto_lock_on_background, usage_monitoring_enabled, notification_monitoring_enabled, onboarding_completed, updated_at) " +
                    "VALUES ('default', 'INR', '₹', 'system', 0, 0, 1, 1, 0, $currentTime)"
        )
    }

    private fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)

        if (encryptedBase64 != null) {
            val encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            return keyManager.decrypt(encryptedBytes)
        }

        // Generate a new random passphrase
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)

        // Encrypt and store it
        val encrypted = keyManager.encrypt(passphrase)
        val base64 = Base64.encodeToString(encrypted, Base64.DEFAULT)
        prefs.edit().putString(KEY_ENCRYPTED_PASSPHRASE, base64).apply()

        return passphrase
    }
}
