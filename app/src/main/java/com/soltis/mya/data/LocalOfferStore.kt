package com.soltis.mya.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class P2pOffer(
    val id: String,
    val ownerEmail: String,
    val ownerName: String,
    val type: String,
    val fromCurrency: String,
    val toCurrency: String,
    val amount: Double,
    val rate: Double,
    val minAmount: Double,
    val maxAmount: Double,
    val paymentMethods: List<String>,
    val terms: String,
    val status: String = STATUS_ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVA"
        const val STATUS_IN_OPERATION = "EN_OPERACION"
        const val STATUS_CLOSED = "CERRADA"
        const val STATUS_CANCELLED = "CANCELADA"
    }
}

class LocalOfferStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val userStore = LocalUserStore(appContext)

    init {
        seedDemoOffersIfNeeded()
    }

    fun addOffer(offer: P2pOffer) {
        saveOffers(getOffers() + offer)
    }

    fun getMyOffers(): List<P2pOffer> {
        val currentEmail = userStore.getCurrentUser()?.email ?: return emptyList()
        return getOffers()
            .filter { it.ownerEmail.equals(currentEmail, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }

    fun getMarketOffersForCurrentUser(): List<P2pOffer> {
        val currentEmail = userStore.getCurrentUser()?.email.orEmpty()
        return getOffers()
            .filter { it.status == P2pOffer.STATUS_ACTIVE && !it.ownerEmail.equals(currentEmail, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }

    fun findById(id: String): P2pOffer? = getOffers().firstOrNull { it.id == id }

    fun cancelOffer(id: String): P2pOffer? {
        var cancelled: P2pOffer? = null
        val updated = getOffers().map { offer ->
            if (offer.id == id && offer.status == P2pOffer.STATUS_ACTIVE) {
                cancelled = offer.copy(status = P2pOffer.STATUS_CANCELLED)
                cancelled!!
            } else {
                offer
            }
        }
        saveOffers(updated)
        return cancelled
    }

    fun closeForOperation(id: String): P2pOffer? {
        var closed: P2pOffer? = null
        val updated = getOffers().map { offer ->
            if (offer.id == id && offer.status == P2pOffer.STATUS_ACTIVE) {
                closed = offer.copy(status = P2pOffer.STATUS_CLOSED)
                closed!!
            } else {
                offer
            }
        }
        saveOffers(updated)
        return closed
    }

    fun reopenOffer(id: String): P2pOffer? {
        var reopened: P2pOffer? = null
        val updated = getOffers().map { offer ->
            if (offer.id == id && offer.status == P2pOffer.STATUS_CLOSED) {
                reopened = offer.copy(status = P2pOffer.STATUS_ACTIVE)
                reopened!!
            } else {
                offer
            }
        }
        saveOffers(updated)
        return reopened
    }

    fun nextOfferId(): String {
        val next = (getOffers().size + 2001).coerceAtLeast(2001)
        return "USR-$next"
    }

    private fun seedDemoOffersIfNeeded() {
        val current = getOffers()
        val demoOffers = listOf(
            P2pOffer("DEMO-V001", "carlos.demo@email.com", "Carlos_P2P", "VENTA", "USD", "PEN", 1200.0, 3.75, 100.0, 1200.0, listOf("Yape", "Plin", "Wallet"), "Pago inmediato.", ordersSeedStatus()),
            P2pOffer("DEMO-V002", "maria.demo@email.com", "Maria_Crypto", "VENTA", "USD", "PEN", 500.0, 3.76, 50.0, 500.0, listOf("Transferencia", "Yape"), "Transferencia desde cuenta propia.", ordersSeedStatus()),
            P2pOffer("DEMO-V003", "exchange.demo@email.com", "Exchange_Fast", "VENTA", "EUR", "PEN", 2000.0, 4.05, 500.0, 2000.0, listOf("Transferencia", "Wallet"), "Operacion en menos de 30 minutos.", ordersSeedStatus()),
            P2pOffer("DEMO-V004", "globalfx.demo@email.com", "GlobalFX", "VENTA", "USD", "PEN", 850.0, 3.74, 100.0, 850.0, listOf("Plin", "Wallet"), "Atencion rapida por Wallet.", ordersSeedStatus()),
            P2pOffer("DEMO-V005", "eurofast.demo@email.com", "EuroFast", "VENTA", "EUR", "USD", 650.0, 1.08, 100.0, 650.0, listOf("Transferencia", "Yape"), "Solo titulares verificados.", ordersSeedStatus()),
            P2pOffer("DEMO-V006", "soles.demo@email.com", "SolesYa", "VENTA", "PEN", "USD", 2400.0, 0.2670, 300.0, 2400.0, listOf("Wallet", "Transferencia"), "Venta de soles con confirmacion rapida.", ordersSeedStatus()),
            P2pOffer("DEMO-V007", "norte.demo@email.com", "CambioNorte", "VENTA", "USD", "PEN", 430.0, 3.77, 50.0, 430.0, listOf("Yape"), "Acepta solo Yape del titular.", ordersSeedStatus()),
            P2pOffer("DEMO-V008", "andes.demo@email.com", "AndesFX", "VENTA", "EUR", "PEN", 300.0, 4.06, 50.0, 300.0, listOf("Plin", "Transferencia"), "Operacion con comprobante obligatorio.", ordersSeedStatus()),
            P2pOffer("DEMO-V009", "rapid.demo@email.com", "RapidCash", "VENTA", "USD", "EUR", 780.0, 0.9220, 100.0, 780.0, listOf("Wallet"), "Solo wallet interno.", ordersSeedStatus()),
            P2pOffer("DEMO-V010", "prime.demo@email.com", "PrimeP2P", "VENTA", "PEN", "EUR", 1600.0, 0.2460, 200.0, 1600.0, listOf("Transferencia", "Wallet"), "Transferencia inmediata.", ordersSeedStatus()),
            P2pOffer("DEMO-C001", "ana.demo@email.com", "AnaCambios", "COMPRA", "USD", "PEN", 900.0, 3.72, 100.0, 900.0, listOf("Plin", "Transferencia"), "Compra por una sola operacion.", ordersSeedStatus()),
            P2pOffer("DEMO-C002", "luis.demo@email.com", "Luis_P2P", "COMPRA", "EUR", "USD", 700.0, 1.07, 100.0, 700.0, listOf("Wallet", "Transferencia"), "Confirmacion inmediata.", ordersSeedStatus()),
            P2pOffer("DEMO-C003", "valeria.demo@email.com", "ValeriaFX", "COMPRA", "USD", "PEN", 350.0, 3.71, 50.0, 350.0, listOf("Yape", "Plin"), "Pago por aplicativo movil.", ordersSeedStatus()),
            P2pOffer("DEMO-C004", "banker.demo@email.com", "BankerPro", "COMPRA", "PEN", "USD", 1800.0, 0.2660, 200.0, 1800.0, listOf("Transferencia"), "Transferencia bancaria verificada.", ordersSeedStatus()),
            P2pOffer("DEMO-C005", "quick.demo@email.com", "QuickChange", "COMPRA", "USD", "PEN", 1000.0, 3.73, 100.0, 1000.0, listOf("Wallet", "Yape"), "Operacion en maximo 15 minutos.", ordersSeedStatus()),
            P2pOffer("DEMO-C006", "lucia.demo@email.com", "LuciaTrade", "COMPRA", "EUR", "PEN", 420.0, 4.02, 80.0, 420.0, listOf("Yape", "Transferencia"), "Pago inmediato tras comprobante.", ordersSeedStatus()),
            P2pOffer("DEMO-C007", "fintech.demo@email.com", "FintechPeru", "COMPRA", "USD", "PEN", 1500.0, 3.70, 200.0, 1500.0, listOf("Plin", "Wallet"), "Compra de alto volumen.", ordersSeedStatus()),
            P2pOffer("DEMO-C008", "eurobuyer.demo@email.com", "EuroBuyer", "COMPRA", "PEN", "EUR", 2200.0, 0.2440, 300.0, 2200.0, listOf("Transferencia"), "Solo transferencia bancaria.", ordersSeedStatus()),
            P2pOffer("DEMO-C009", "mype.demo@email.com", "MypeCambio", "COMPRA", "USD", "EUR", 600.0, 0.9180, 100.0, 600.0, listOf("Wallet", "Transferencia"), "Operacion para cuenta empresa.", ordersSeedStatus()),
            P2pOffer("DEMO-C010", "fastbuy.demo@email.com", "FastBuy", "COMPRA", "EUR", "USD", 980.0, 1.06, 150.0, 980.0, listOf("Yape", "Plin", "Wallet"), "Compra rapida con reputacion alta.", ordersSeedStatus())
        )
        val missing = demoOffers.filter { demo -> current.none { it.id == demo.id } }
        if (missing.isNotEmpty()) saveOffers(current + missing)
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private fun ordersSeedStatus() = P2pOffer.STATUS_ACTIVE

    private fun getOffers(): List<P2pOffer> {
        val raw = prefs.getString(KEY_OFFERS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index -> array.getJSONObject(index).toP2pOffer() }
    }

    private fun saveOffers(offers: List<P2pOffer>) {
        val array = JSONArray()
        offers.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_OFFERS, array.toString()).apply()
    }

    private fun P2pOffer.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("ownerEmail", ownerEmail)
            .put("ownerName", ownerName)
            .put("type", type)
            .put("fromCurrency", fromCurrency)
            .put("toCurrency", toCurrency)
            .put("amount", amount)
            .put("rate", rate)
            .put("minAmount", minAmount)
            .put("maxAmount", maxAmount)
            .put("paymentMethods", JSONArray(paymentMethods))
            .put("terms", terms)
            .put("status", status)
            .put("createdAt", createdAt)
    }

    private fun JSONObject.toP2pOffer(): P2pOffer {
        return P2pOffer(
            id = optString("id"),
            ownerEmail = optString("ownerEmail"),
            ownerName = optString("ownerName"),
            type = optString("type"),
            fromCurrency = optString("fromCurrency"),
            toCurrency = optString("toCurrency"),
            amount = optDouble("amount", 0.0),
            rate = optDouble("rate", 0.0),
            minAmount = optDouble("minAmount", 0.0),
            maxAmount = optDouble("maxAmount", optDouble("amount", 0.0)),
            paymentMethods = optJSONArray("paymentMethods").toStringList(),
            terms = optString("terms"),
            status = optString("status", P2pOffer.STATUS_ACTIVE),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length()).map { index -> optString(index) }.filter { it.isNotBlank() }
    }

    companion object {
        private const val PREFS_NAME = "p2p_local_offers"
        private const val KEY_OFFERS = "offers"
        private const val KEY_SEEDED = "seeded"
    }
}
