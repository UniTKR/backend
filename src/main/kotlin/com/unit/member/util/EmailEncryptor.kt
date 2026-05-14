package com.unit.member.util

import com.unit.member.config.EmailEncryptionProperties
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class EmailEncryptor(
    properties: EmailEncryptionProperties,
) {

    private val keySpec = SecretKeySpec(decodeKey(properties.keyBase64), "AES")
    private val secureRandom = SecureRandom()

    fun encrypt(email: String): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        val cipherText = cipher.doFinal(email.toByteArray(Charsets.UTF_8))
        return iv + cipherText
    }

    fun decrypt(encryptedEmail: ByteArray): String {
        require(encryptedEmail.size > IV_LENGTH_BYTES) {
            "Encrypted email payload is too short."
        }

        val iv = encryptedEmail.copyOfRange(0, IV_LENGTH_BYTES)
        val cipherText = encryptedEmail.copyOfRange(IV_LENGTH_BYTES, encryptedEmail.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(TAG_LENGTH_BITS, iv))

        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun decodeKey(keyBase64: String): ByteArray {
        val key = Base64.getDecoder().decode(keyBase64)
        require(key.size == AES_256_KEY_LENGTH_BYTES) {
            "Email encryption key must be a 32-byte Base64 encoded value."
        }
        return key
    }

    private companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BITS = 128
        const val AES_256_KEY_LENGTH_BYTES = 32
    }
}