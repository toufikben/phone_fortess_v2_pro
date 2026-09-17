package com.phonefortress.app.data.billing

object BillingProducts {
    const val PRO_MONTHLY = "pro_monthly"
    const val PRO_YEARLY = "pro_yearly"
    const val FAMILY_MONTHLY = "family_monthly"
    const val THEME_PACK_PREMIUM = "theme_pack_premium"
    const val TEMPLATES_PACK = "templates_pack"
    const val ANALYTICS_PACK = "analytics_pack"
    val ALL_SUBSCRIPTIONS = listOf(PRO_MONTHLY, PRO_YEARLY, FAMILY_MONTHLY)
    val ALL_INAPP = listOf(THEME_PACK_PREMIUM, TEMPLATES_PACK, ANALYTICS_PACK)
    val ALL = ALL_SUBSCRIPTIONS + ALL_INAPP
}

enum class ProTier { FREE, PRO, FAMILY, LIFETIME }

enum class ProFeature(val requiredTier: ProTier) {
    AI_THREAT_ENGINE(ProTier.PRO), SILENT_GUARDIAN(ProTier.PRO), GEOFENCE_UNLIMITED(ProTier.PRO),
    MULTI_CHANNEL(ProTier.PRO), AUDIO_RECORDING(ProTier.PRO), UNLIMITED_LOGS(ProTier.PRO),
    CLOUD_BACKUP(ProTier.PRO), FAMILY_MANAGEMENT(ProTier.FAMILY), PRIORITY_SUPPORT(ProTier.PRO),
    EXPORT_PDF(ProTier.PRO), PREMIUM_THEMES(ProTier.PRO), ADVANCED_ANALYTICS(ProTier.PRO)
}
