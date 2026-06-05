package com.soltis.mya.wallet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.soltis.mya.R

class WalletViewModel : ViewModel() {

    // Saldos Disponibles (Lo que el usuario puede usar)
    private val _balances = MutableLiveData<Map<String, Double>>(
        mapOf(
            "PEN" to 8240.50,
            "USD" to 2150.00,
            "EUR" to 66.28
        )
    )
    val balances: LiveData<Map<String, Double>> = _balances

    // Saldos Retenidos (Dinero en garantía por transacciones P2P)
    private val _heldBalances = MutableLiveData<Map<String, Double>>(
        mapOf(
            "PEN" to 390.00,
            "USD" to 45.00,
            "EUR" to 0.0
        )
    )
    val heldBalances: LiveData<Map<String, Double>> = _heldBalances

    // Métricas acumuladas (Recarga total y Liberaciones totales)
    private val _totalRecharged = MutableLiveData<Map<String, Double>>(
        mapOf("PEN" to 12500.0, "USD" to 3000.0, "EUR" to 100.0)
    )
    val totalRecharged: LiveData<Map<String, Double>> = _totalRecharged

    private val _totalReleased = MutableLiveData<Map<String, Double>>(
        mapOf("PEN" to 4500.0, "USD" to 850.0, "EUR" to 33.0)
    )
    val totalReleased: LiveData<Map<String, Double>> = _totalReleased

    private val _totalWithdrawn = MutableLiveData<Map<String, Double>>(
        mapOf("PEN" to 0.0, "USD" to 0.0, "EUR" to 0.0)
    )
    val totalWithdrawn: LiveData<Map<String, Double>> = _totalWithdrawn

    private val _movements = MutableLiveData<List<MovementItem>>(
        listOf(
            MovementItem(R.drawable.ic_deposit, android.graphics.Color.parseColor("#27AE60"),
                "Recarga", "Depósito bancario · BBVA",
                "20 may 2025, 10:35 a. m.", "+ S/ 500.00", "PEN", true, "recarga"),
            MovementItem(R.drawable.ic_lock, android.graphics.Color.parseColor("#E67E22"),
                "Retención", "Operación #P2P-84521",
                "20 may 2025, 10:18 a. m.", "- S/ 320.00", "PEN", false, "retencion"),
            MovementItem(R.drawable.ic_lock, android.graphics.Color.parseColor("#27AE60"),
                "Liberación", "Operación #P2P-84521",
                "20 may 2025, 10:42 a. m.", "+ S/ 320.00", "PEN", true, "liberacion")
        )
    )
    val movements: LiveData<List<MovementItem>> = _movements

    fun addDeposit(currency: String, amount: Double) {
        // Actualizar saldo disponible
        val currentBalances = _balances.value?.toMutableMap() ?: mutableMapOf()
        currentBalances[currency] = (currentBalances[currency] ?: 0.0) + amount
        _balances.value = currentBalances

        // Actualizar métrica de recarga total
        val currentRecharged = _totalRecharged.value?.toMutableMap() ?: mutableMapOf()
        currentRecharged[currency] = (currentRecharged[currency] ?: 0.0) + amount
        _totalRecharged.value = currentRecharged

        // Agregar al historial
        val symbol = if (currency == "PEN") "S/" else if (currency == "USD") "$" else "€"
        val newMovement = MovementItem(
            R.drawable.ic_deposit, android.graphics.Color.parseColor("#27AE60"),
            "Recarga", "Depósito manual",
            "Hoy", "+ $symbol ${"%.2f".format(amount)}", currency, true, "recarga"
        )
        val currentMovs = _movements.value?.toMutableList() ?: mutableListOf()
        currentMovs.add(0, newMovement)
        _movements.value = currentMovs
    }

    fun withdraw(currency: String, amount: Double, destination: String): Boolean {
        val currentBalances = _balances.value?.toMutableMap() ?: mutableMapOf()
        val available = currentBalances[currency] ?: 0.0

        if (amount <= 0 || amount > available) return false

        currentBalances[currency] = available - amount
        _balances.value = currentBalances

        val currentWithdrawn = _totalWithdrawn.value?.toMutableMap() ?: mutableMapOf()
        currentWithdrawn[currency] = (currentWithdrawn[currency] ?: 0.0) + amount
        _totalWithdrawn.value = currentWithdrawn

        val symbol = currencySymbol(currency)
        val newMovement = MovementItem(
            R.drawable.ic_withdraw,
            android.graphics.Color.parseColor("#E74C3C"),
            "Retiro",
            "Destino simulado: $destination",
            "Hoy",
            "- $symbol ${"%.2f".format(amount)}",
            currency,
            false,
            "retiro"
        )
        val currentMovs = _movements.value?.toMutableList() ?: mutableListOf()
        currentMovs.add(0, newMovement)
        _movements.value = currentMovs

        return true
    }

    fun registerOfferImpact(type: String, currency: String, amount: Double) {
        val currentBalances = _balances.value?.toMutableMap() ?: mutableMapOf()
        val currentHeld = _heldBalances.value?.toMutableMap() ?: mutableMapOf()
        
        val symbol = if (currency == "PEN") "S/" else if (currency == "USD") "$" else "€"
        
        if (type == "VENTA") {
            // En una VENTA, el saldo se descuenta del disponible y pasa a retenido
            val available = currentBalances[currency] ?: 0.0
            if (available >= amount) {
                currentBalances[currency] = available - amount
                currentHeld[currency] = (currentHeld[currency] ?: 0.0) + amount
                
                _balances.value = currentBalances
                _heldBalances.value = currentHeld

                // Registrar movimiento de retención
                val movement = MovementItem(
                    R.drawable.ic_lock, android.graphics.Color.parseColor("#E67E22"),
                    "Retención", "Oferta de venta publicada",
                    "Hoy", "- $symbol ${"%.2f".format(amount)}", currency, false, "retencion"
                )
                val currentMovs = _movements.value?.toMutableList() ?: mutableListOf()
                currentMovs.add(0, movement)
                _movements.value = currentMovs
            }
        } else {
            // En una COMPRA, registramos el compromiso en el historial (opcionalmente reteniendo el contra-valor si fuera wallet interna)
            // Por simplicidad de la simulación, registraremos el inicio de la operación
            val movement = MovementItem(
                R.drawable.ic_offers, android.graphics.Color.parseColor("#2980B9"),
                "Pendiente", "Oferta de compra publicada",
                "Hoy", "En espera", currency, true, "pago"
            )
            val currentMovs = _movements.value?.toMutableList() ?: mutableListOf()
            currentMovs.add(0, movement)
            _movements.value = currentMovs
        }
    }
}

fun currencySymbol(currency: String): String {
    return when (currency) {
        "PEN" -> "S/"
        "USD" -> "$"
        "EUR" -> "EUR"
        else -> currency
    }
}
