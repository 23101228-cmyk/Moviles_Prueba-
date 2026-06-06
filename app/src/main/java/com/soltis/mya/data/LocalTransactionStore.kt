package com.soltis.mya.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class P2pTransaction(
    val id: String,
    val offerId: String,
    val takerEmail: String,
    val makerEmail: String,
    val operationType: String,
    val fromCurrency: String,
    val toCurrency: String,
    val amount: Double,
    val rate: Double,
    val total: Double,
    val paymentMethod: String,
    val status: String,
    val proofRequired: Boolean,
    val proofAttached: Boolean = false,
    val disputeReason: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING_PROOF = "PENDIENTE_COMPROBANTE"
        const val STATUS_PENDING_CONFIRMATION = "PENDIENTE_CONFIRMACION"
        const val STATUS_COMPLETED = "COMPLETADA"
        const val STATUS_DISPUTE = "EN_DISPUTA"
        const val STATUS_CANCELLED = "CANCELADA"
    }
}

class LocalTransactionStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val userStore = LocalUserStore(appContext)

    fun add(transaction: P2pTransaction) {
        save(getAll() + transaction)
    }

    fun updateStatus(id: String, status: String, proofAttached: Boolean? = null, disputeReason: String? = null) {
        save(getAll().map { tx ->
            if (tx.id == id) {
                tx.copy(
                    status = status,
                    proofAttached = proofAttached ?: tx.proofAttached,
                    disputeReason = disputeReason ?: tx.disputeReason
                )
            } else {
                tx
            }
        })
    }

    fun findById(id: String): P2pTransaction? = getAll().firstOrNull { it.id == id }

    fun getMine(): List<P2pTransaction> {
        val email = userStore.getCurrentUser()?.email ?: return emptyList()
        return getAll()
            .filter { it.takerEmail.equals(email, ignoreCase = true) || it.makerEmail.equals(email, ignoreCase = true) }
            .sortedByDescending { it.createdAt }
    }

    fun nextTransactionId(): String = "OPE-${getAll().size + 1001}"

    fun getAll(): List<P2pTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index -> array.getJSONObject(index).toTransaction() }
    }

    private fun save(transactions: List<P2pTransaction>) {
        val array = JSONArray()
        transactions.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_TRANSACTIONS, array.toString()).apply()
    }

    private fun P2pTransaction.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("offerId", offerId)
            .put("takerEmail", takerEmail)
            .put("makerEmail", makerEmail)
            .put("operationType", operationType)
            .put("fromCurrency", fromCurrency)
            .put("toCurrency", toCurrency)
            .put("amount", amount)
            .put("rate", rate)
            .put("total", total)
            .put("paymentMethod", paymentMethod)
            .put("status", status)
            .put("proofRequired", proofRequired)
            .put("proofAttached", proofAttached)
            .put("disputeReason", disputeReason)
            .put("createdAt", createdAt)
    }

    private fun JSONObject.toTransaction(): P2pTransaction {
        return P2pTransaction(
            id = optString("id"),
            offerId = optString("offerId"),
            takerEmail = optString("takerEmail"),
            makerEmail = optString("makerEmail"),
            operationType = optString("operationType"),
            fromCurrency = optString("fromCurrency"),
            toCurrency = optString("toCurrency"),
            amount = optDouble("amount", 0.0),
            rate = optDouble("rate", 0.0),
            total = optDouble("total", 0.0),
            paymentMethod = optString("paymentMethod"),
            status = optString("status"),
            proofRequired = optBoolean("proofRequired", false),
            proofAttached = optBoolean("proofAttached", false),
            disputeReason = optString("disputeReason"),
            createdAt = optLong("createdAt", System.currentTimeMillis())
        )
    }

    companion object {
        private const val PREFS_NAME = "p2p_local_transactions"
        private const val KEY_TRANSACTIONS = "transactions"
    }
}
