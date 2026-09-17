package com.phonefortress.app.data.crypto

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinGate {
    // إنشاء hash لـ PIN دون حفظ الرقم نفسه.
    fun hash(pin: String, salt: ByteArray = randomSalt()): String {
        require(pin.matches(Regex("\\d{6,8}"))) { "PIN must contain 6 to 8 digits" }
        val spec = PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)
        val digest = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return "${encode(salt)}:${encode(digest)}"
    }

    // التحقق من PIN باستخدام المقارنة الثابتة الزمن.
    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return false
        return java.security.MessageDigest.isEqual(decode(parts[1]), decode(hash(pin, decode(parts[0])).substringAfter(":")))
    }

    private fun randomSalt() = ByteArray(16).also { SecureRandom().nextBytes(it) }
    private fun encode(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    private fun decode(value: String): ByteArray = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
