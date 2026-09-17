package com.phonefortress.app.data.prefs

import android.content.Context
import com.phonefortress.app.data.billing.ProTier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProPrefs @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("pro_prefs", Context.MODE_PRIVATE)
    private val _tier = MutableStateFlow(loadTier())
    val tier: StateFlow<ProTier> = _tier.asStateFlow()

    fun setTier(tier: ProTier) { _tier.value = tier; prefs.edit().putString(KEY_TIER, tier.name).apply() }
    fun setPurchasedProducts(products: Set<String>) { prefs.edit().putStringSet(KEY_PRODUCTS, products).apply() }
    fun purchasedProducts(): Set<String> = prefs.getStringSet(KEY_PRODUCTS, emptySet()).orEmpty()
    fun hasFeature(feature: com.phonefortress.app.data.billing.ProFeature): Boolean = when (tier.value) {
        ProTier.FAMILY, ProTier.LIFETIME -> true
        ProTier.PRO -> feature.requiredTier == ProTier.PRO
        ProTier.FREE -> false
    }
    private fun loadTier() = runCatching { ProTier.valueOf(prefs.getString(KEY_TIER, ProTier.FREE.name)!!) }.getOrDefault(ProTier.FREE)
    private companion object { const val KEY_TIER = "tier"; const val KEY_PRODUCTS = "purchased_products" }
}
