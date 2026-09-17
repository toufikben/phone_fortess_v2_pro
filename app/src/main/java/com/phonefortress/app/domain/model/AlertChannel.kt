package com.phonefortress.app.domain.model

/**
 * واجهة موحدة لكل قنوات التنبيه.
 * كل قناة تُنفذها بشكل مستقل — لا اعتماد بيني.
 */
interface AlertChannel {

    /** معرف فريد للقناة. */
    val id: String

    /** الاسم المعروض للمستخدم. */
    val displayName: String

    /** هل تحتاج القناة إعداداً قبل الاستخدام؟ */
    val requiresConfig: Boolean

    /** هل القناة مفعّلة حالياً في الإعدادات؟ */
    suspend fun isEnabled(): Boolean

    /** هل الإعدادات صحيحة وكاملة؟ */
    suspend fun isConfigured(): Boolean

    /** إرسال التنبيه. */
    suspend fun send(event: SecurityEvent, payload: AlertPayload): AlertResult

    /** اختبار سريع للإعدادات. */
    suspend fun test(): AlertResult
}
