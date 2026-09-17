package com.phonefortress.app.data.crypto

import com.phonefortress.app.util.Logger
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinHasher @Inject constructor() {
    companion object {
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val SALT_LENGTH = 16
    }

    data class HashedPin(val hash: String, val salt: String)

    fun hash(pin: String): HashedPin {
        val saltBytes = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val hashBytes = pbkdf2(pin, saltBytes)
        return HashedPin(
            hash = Base64.getEncoder().withoutPadding().encodeToString(hashBytes),
            salt = Base64.getEncoder().withoutPadding().encodeToString(saltBytes)
        )
    }

    fun verify(pin: String, hashedPin: HashedPin): Boolean = try {
        val saltBytes = Base64.getDecoder().decode(hashedPin.salt)
        val expectedHash = Base64.getDecoder().decode(hashedPin.hash)
        constantTimeEquals(expectedHash, pbkdf2(pin, saltBytes))
    } catch (e: Exception) {
        Logger.e(e, "PIN verify error")
        false
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
