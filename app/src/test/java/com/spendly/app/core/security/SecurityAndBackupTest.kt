package com.spendly.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class SecurityAndBackupTest {

    @Test
    fun pbkdf2KeyDerivationAndAesGcmEncryptionDecryption() {
        val password = "StrongUserPassword#2026"
        val originalData = "{\"transactions\":[{\"id\":\"1\",\"amountMinorUnits\":15000,\"merchantName\":\"Zomato\"}]}"

        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val iv = ByteArray(12)
        random.nextBytes(iv)

        // Key derivation
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = skf.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encrypt with AES-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(originalData.toByteArray(Charsets.UTF_8))

        // Decrypt with correct password
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val decryptedBytes = decryptCipher.doFinal(ciphertext)
        val decryptedString = String(decryptedBytes, Charsets.UTF_8)

        assertEquals("Decrypted plaintext must match original data", originalData, decryptedString)
    }

    @Test(expected = Exception::class)
    fun wrongPasswordFailsDecryption() {
        val correctPassword = "CorrectPassword123"
        val wrongPassword = "WrongPassword456"
        val originalData = "TopSecretFinancialRecord"

        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val iv = ByteArray(12)
        random.nextBytes(iv)

        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val correctKey = SecretKeySpec(skf.generateSecret(PBEKeySpec(correctPassword.toCharArray(), salt, 100_000, 256)).encoded, "AES")
        val wrongKey = SecretKeySpec(skf.generateSecret(PBEKeySpec(wrongPassword.toCharArray(), salt, 100_000, 256)).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, correctKey, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(originalData.toByteArray(Charsets.UTF_8))

        // Attempt decryption with wrong key -> must throw AEADBadTagException / BadPaddingException
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, wrongKey, GCMParameterSpec(128, iv))
        decryptCipher.doFinal(ciphertext)
    }

    @Test(expected = Exception::class)
    fun tamperedCiphertextFailsIntegrityCheck() {
        val password = "SecurePassword#99"
        val originalData = "UntamperedPayload"

        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        val iv = ByteArray(12)
        random.nextBytes(iv)

        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = SecretKeySpec(skf.generateSecret(PBEKeySpec(password.toCharArray(), salt, 100_000, 256)).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(originalData.toByteArray(Charsets.UTF_8))

        // Tamper with the ciphertext byte
        ciphertext[0] = (ciphertext[0].toInt() xor 0xFF).toByte()

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        decryptCipher.doFinal(ciphertext)
    }

    @Test
    fun pinHashVerificationWithSalt() {
        val pin = "1234"
        val wrongPin = "5678"

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val expectedHash = md.digest(pin.toByteArray(Charsets.UTF_8))

        // Verify correct PIN
        val mdVerify = MessageDigest.getInstance("SHA-256")
        mdVerify.update(salt)
        val computedHash = mdVerify.digest(pin.toByteArray(Charsets.UTF_8))
        assertTrue(MessageDigest.isEqual(expectedHash, computedHash))

        // Verify wrong PIN fails
        val mdWrong = MessageDigest.getInstance("SHA-256")
        mdWrong.update(salt)
        val wrongComputed = mdWrong.digest(wrongPin.toByteArray(Charsets.UTF_8))
        assertFalse(MessageDigest.isEqual(expectedHash, wrongComputed))
    }
}
