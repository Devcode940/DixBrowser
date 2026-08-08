package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Password-vault cryptography backed by Android Keystore.
 *
 * Ciphertext format v1: "v1:" + Base64(12-byte IV || ciphertext+GCM tag)
 * The legacy unprefixed format remains readable for existing installations.
 */
object PasswordSecurity {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTES = 12
    private const val KEY_ALIAS = "BrowserPasswordKeyAlias"
    private const val VERSION_PREFIX = "v1:"

    class PasswordCryptoException(message: String, cause: Throwable? = null) :
        IllegalStateException(message, cause)

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null)
            return (entry as? KeyStore.SecretKeyEntry)?.secretKey
                ?: throw PasswordCryptoException("Password vault key is unavailable")
        }

        return try {
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            ).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
            }.generateKey()
        } catch (e: Exception) {
            throw PasswordCryptoException("Unable to create password vault key", e)
        }
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            require(iv.size == IV_LENGTH_BYTES) { "Unexpected GCM IV length" }
            val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            iv.copyInto(combined, destinationOffset = 0)
            ciphertext.copyInto(combined, destinationOffset = iv.size)
            VERSION_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: PasswordCryptoException) {
            throw e
        } catch (e: Exception) {
            throw PasswordCryptoException("Unable to encrypt password", e)
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val encoded = encryptedText.removePrefix(VERSION_PREFIX)
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            if (combined.size <= IV_LENGTH_BYTES) {
                throw PasswordCryptoException("Invalid password ciphertext")
            }

            val iv = combined.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertext = combined.copyOfRange(IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                GCMParameterSpec(TAG_LENGTH_BIT, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: PasswordCryptoException) {
            throw e
        } catch (e: Exception) {
            throw PasswordCryptoException(
                "Unable to decrypt password; the vault key or ciphertext may be invalid",
                e
            )
        }
    }

    fun generatePassword(
        length: Int = 16,
        includeUppercase: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        require(length >= 1) { "Password length must be positive" }

        val lowercase = "abcdefghijklmnopqrstuvwxyz"
        val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val numbers = "0123456789"
        val symbols = "!@#$%^&*()_+-=[]{}|;:,.<>?"
        val pools = buildList {
            add(lowercase)
            if (includeUppercase) add(uppercase)
            if (includeNumbers) add(numbers)
            if (includeSymbols) add(symbols)
        }
        require(length >= pools.size) {
            "Password length must be at least ${pools.size} when all character classes are enabled"
        }

        val random = SecureRandom()
        val chars = ArrayList<Char>(length)
        pools.forEach { pool -> chars += pool[random.nextInt(pool.length)] }
        val combinedPool = pools.joinToString("")
        repeat(length - chars.size) {
            chars += combinedPool[random.nextInt(combinedPool.length)]
        }
        chars.shuffle(random)
        return chars.joinToString("")
    }
}
