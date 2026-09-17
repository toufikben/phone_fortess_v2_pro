package com.phonefortress.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.phonefortress.app.data.prefs.ProPrefs
import com.phonefortress.app.util.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

 data class BillingState(
    val connected: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val purchasedProducts: Set<String> = emptySet()
)

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proPrefs: ProPrefs
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(BillingState(purchasedProducts = proPrefs.purchasedProducts()))
    val state: StateFlow<BillingState> = _state.asStateFlow()
    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().enablePrepaidPlans().build())
        .build()

    fun connect() {
        if (billingClient.isReady) { queryProducts(); return }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                _state.value = _state.value.copy(connected = ok, error = if (ok) null else result.debugMessage)
                if (ok) { queryProducts(); refreshPurchases() }
            }
            override fun onBillingServiceDisconnected() { _state.value = _state.value.copy(connected = false) }
        })
    }

    fun queryProducts() {
        if (!billingClient.isReady) return
        val productList = BillingProducts.ALL.map { id ->
            QueryProductDetailsParams.Product.newBuilder().setProductId(id)
                .setProductType(if (id in BillingProducts.ALL_SUBSCRIPTIONS) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP).build()
        }
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(productList).build()) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _products.value = details
            else _state.value = _state.value.copy(error = result.debugMessage)
        }
    }

    fun purchase(activity: Activity, product: ProductDetails) {
        val offer = product.subscriptionOfferDetails?.firstOrNull()
        val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).apply {
            offer?.let { setOfferToken(it.offerToken) }
        }.build()
        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
    }

    fun restorePurchases() { if (billingClient.isReady) refreshPurchases() }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) handlePurchases(purchases)
        else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) _state.value = _state.value.copy(error = result.debugMessage)
    }

    private fun refreshPurchases() {
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach { type ->
            billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) handlePurchases(purchases)
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val owned = purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.flatMap { it.products }.toSet()
        scope.launch {
            proPrefs.setPurchasedProducts(owned)
            val tier = when {
                BillingProducts.FAMILY_MONTHLY in owned -> ProTier.FAMILY
                BillingProducts.PRO_MONTHLY in owned || BillingProducts.PRO_YEARLY in owned -> ProTier.PRO
                else -> ProTier.FREE
            }
            proPrefs.setTier(tier)
            _state.value = _state.value.copy(purchasedProducts = owned, loading = false)
        }
    }

    fun close() { if (billingClient.isReady) billingClient.endConnection() }
}
