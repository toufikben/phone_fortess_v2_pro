package com.phonefortress.app.domain.model

/**
 * الهويات البصرية المتاحة في التطبيق.
 */
enum class AppTheme(
    val id: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val descriptionAr: String,
    val emoji: String
) {
    FORTRESS_NOIR(
        id = "fortress_noir",
        displayNameAr = "الحصن الليلي",
        displayNameEn = "Fortress Noir",
        descriptionAr = "قوة وغموض بأسلوب عسكري احترافي",
        emoji = "⚔️"
    ),
    LIQUID_SHIELD(
        id = "liquid_shield",
        displayNameAr = "الدرع السائل",
        displayNameEn = "Liquid Shield",
        descriptionAr = "حداثة وانسيابية بلمسة مستقبلية",
        emoji = "🌊"
    ),
    CYBER_TERMINAL(
        id = "cyber_terminal",
        displayNameAr = "الواجهة السيبرانية",
        displayNameEn = "Cyber Terminal",
        descriptionAr = "تكنو-عرقية للمستخدمين المتقدمين",
        emoji = "🔮"
    ),
    GUARDIAN_MINIMAL(
        id = "guardian_minimal",
        displayNameAr = "الحارس البسيط",
        displayNameEn = "Guardian Minimal",
        descriptionAr = "توازن مثالي بين الفخامة والبساطة",
        emoji = "🌟"
    );

    companion object {
        fun fromId(id: String?): AppTheme =
            entries.firstOrNull { it.id == id } ?: GUARDIAN_MINIMAL
    }
}
