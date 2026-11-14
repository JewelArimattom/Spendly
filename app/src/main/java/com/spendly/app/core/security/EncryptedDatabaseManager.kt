package com.spendly.app.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.room.Room
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
            .build()
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
