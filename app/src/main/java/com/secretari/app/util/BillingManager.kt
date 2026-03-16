package com.secretari.app.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.secretari.app.data.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        @Volatile
        private var INSTANCE: BillingManager? = null

        fun getInstance(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BillingManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // Product ID -> price mapping from server
    private val _appProducts = MutableStateFlow<Map<String, Double>>(emptyMap())
    val appProducts: StateFlow<Map<String, Double>> = _appProducts.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val apiService = ApiService.create()

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Purchasing : PurchaseState()
        data class Success(val productId: String, val price: Double) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    private var isConnecting = false

    fun startConnection() {
        if (billingClient.isReady) {
            // Already connected — just ensure products are loaded
            if (_products.value.isEmpty()) {
                loadProductIDsFromServer()
            }
            return
        }
        if (isConnecting) return
        isConnecting = true

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                isConnecting = false
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected")
                    loadProductIDsFromServer()
                    queryExistingPurchases()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnecting = false
                Log.w(TAG, "Billing service disconnected, will reconnect on next use")
            }
        })
    }

    /**
     * Ensure products are loaded. Call this when the Store screen is opened.
     * Reconnects billing client if needed and retries loading products.
     */
    fun ensureProductsLoaded() {
        if (_products.value.isNotEmpty()) return

        if (!billingClient.isReady) {
            startConnection()
        } else {
            loadProductIDsFromServer()
        }
    }

    /**
     * Fetch product IDs and prices from server, then query Google Play for product details.
     * Server returns: {"ver0": {"productIDs": {"890842": 8.99, ...}}}
     */
    private fun loadProductIDsFromServer() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.getProductIDs()
                if (response.isSuccessful) {
                    val body = response.body()
                    val productIDsData = body?.get("ver0")
                    if (productIDsData != null) {
                        val productMap = productIDsData.productIDs
                        _appProducts.value = productMap
                        Log.d(TAG, "Product IDs from server: $productMap")
                        queryProductDetails(productMap.keys.toList())
                    } else {
                        Log.w(TAG, "No ver0 data in product IDs response")
                    }
                } else {
                    Log.e(TAG, "Failed to fetch product IDs: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching product IDs", e)
            }
        }
    }

    private fun queryProductDetails(productIDs: List<String>) {
        val productList = productIDs.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _products.value = productDetailsList.sortedByDescending { it.productId }
                for (details in productDetailsList) {
                    Log.d(TAG, "Product loaded: ${details.productId} - ${details.oneTimePurchaseOfferDetails?.formattedPrice}")
                }
                if (productDetailsList.isEmpty()) {
                    Log.w(TAG, "No products found in Play Console for IDs: $productIDs")
                }
            } else {
                Log.e(TAG, "Failed to query products: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        consumePurchase(purchase)
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        _purchaseState.value = PurchaseState.Purchasing

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        consumePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
                _purchaseState.value = PurchaseState.Idle
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.Error("Purchase failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun consumePurchase(purchase: Purchase) {
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase consumed successfully")
                // Get the product ID and its price from the server-provided mapping
                val productId = purchase.products.firstOrNull() ?: ""
                val price = _appProducts.value[productId] ?: 0.0
                _purchaseState.value = PurchaseState.Success(productId, price)
            } else {
                Log.e(TAG, "Failed to consume purchase: ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseState.Error("Failed to process purchase. Please contact support.")
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseState.Idle
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
