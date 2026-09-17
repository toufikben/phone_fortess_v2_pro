package com.phonefortress.app.data.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * غلاف مبسط يخفي تفاصيل Keystore عن باقي الطبقات.
 */
@Singleton
class CryptoHelper @Inject constructor(
    private val keystoreManager: KeystoreManager
) {
    fun encrypt(plain: String): String = keystoreManager.encrypt(plain)
    fun decrypt(encoded: String): String = keystoreManager.decrypt(encoded)
}
