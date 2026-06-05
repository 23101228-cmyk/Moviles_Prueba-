package com.soltis.mya.mercado

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentMercadoBinding
import com.soltis.mya.databinding.ItemMarketOfferBinding

data class MarketOffer(
    val id: String,
    val user: String,
    val reputation: Int,
    val orders: Int,
    val operation: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: Double,
    val amount: Double,
    val minAmount: Double,
    val paymentMethods: List<String>,
    val status: String,
    val timeLimitMinutes: Int,
    val isOwnOffer: Boolean = false
)

object MarketOfferRepository {
    val offers = listOf(
        MarketOffer("OFR-1001", "Carlos_P2P", 99, 250, "VENTA", "USD", "PEN", 3.75, 1200.0, 100.0, listOf("Yape", "Plin", "Wallet"), "ACTIVA", 15),
        MarketOffer("OFR-1002", "Maria_Crypto", 95, 89, "VENTA", "USD", "PEN", 3.76, 500.0, 50.0, listOf("Transferencia", "Yape"), "ACTIVA", 20),
        MarketOffer("OFR-1003", "Exchange_Fast", 100, 1500, "VENTA", "EUR", "PEN", 4.05, 2000.0, 500.0, listOf("Transferencia", "Wallet"), "ACTIVA", 30),
        MarketOffer("OFR-1004", "AnaCambios", 97, 320, "COMPRA", "USD", "PEN", 3.72, 900.0, 100.0, listOf("Plin", "Transferencia"), "ACTIVA", 15),
        MarketOffer("OFR-1005", "Mi_Oferta", 98, 40, "VENTA", "USD", "PEN", 3.79, 300.0, 50.0, listOf("Yape"), "ACTIVA", 15, isOwnOffer = true),
        MarketOffer("OFR-1006", "Luis_P2P", 91, 62, "COMPRA", "EUR", "USD", 1.08, 700.0, 100.0, listOf("Wallet", "Transferencia"), "CERRADA", 20)
    )

    fun findById(id: String): MarketOffer? = offers.firstOrNull { it.id == id }
}

class MercadoFragment : Fragment() {
    private var _binding: FragmentMercadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MarketOfferAdapter
    private var selectedOperation = "VENTA"
    private var selectedCurrency = "TODAS"
    private var selectedPayment = "TODOS"
    private var minReputation = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMercadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupOperationTabs()
        setupFilters()
        applyFilters()
    }

    private fun setupList() {
        adapter = MarketOfferAdapter(emptyList()) { offer ->
            findNavController().navigate(
                R.id.action_nav_mercado_to_offer_detail,
                Bundle().apply { putString(OfferDetailFragment.ARG_OFFER_ID, offer.id) }
            )
        }

        binding.rvMercado.layoutManager = LinearLayoutManager(context)
        binding.rvMercado.adapter = adapter
    }

    private fun setupOperationTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedOperation = if (tab.position == 0) "VENTA" else "COMPRA"
                applyFilters()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupFilters() {
        val currencyChips = mapOf(
            "TODAS" to binding.chipTodas,
            "PEN" to binding.chipPen,
            "USD" to binding.chipUsd,
            "EUR" to binding.chipEur
        )
        currencyChips.forEach { (currency, chip) ->
            chip.setOnClickListener {
                selectedCurrency = currency
                currencyChips.values.forEach { it.isChecked = false }
                chip.isChecked = true
                applyFilters()
            }
        }

        val paymentChips = mapOf(
            "TODOS" to binding.chipMetodoTodos,
            "Yape" to binding.chipYape,
            "Plin" to binding.chipPlin,
            "Transferencia" to binding.chipTransferencia,
            "Wallet" to binding.chipWallet
        )
        paymentChips.forEach { (payment, chip) ->
            chip.setOnClickListener {
                selectedPayment = payment
                paymentChips.values.forEach { it.isChecked = false }
                chip.isChecked = true
                applyFilters()
            }
        }

        binding.switchReputation.setOnCheckedChangeListener { _, checked ->
            minReputation = if (checked) 95 else 0
            applyFilters()
        }

        binding.btnAplicarFiltros.setOnClickListener { applyFilters() }
        binding.btnLimpiarFiltros.setOnClickListener {
            selectedCurrency = "TODAS"
            selectedPayment = "TODOS"
            minReputation = 0
            binding.etMontoFiltro.setText("")
            binding.switchReputation.isChecked = false
            currencyChips.values.forEach { it.isChecked = false }
            paymentChips.values.forEach { it.isChecked = false }
            binding.chipTodas.isChecked = true
            binding.chipMetodoTodos.isChecked = true
            applyFilters()
        }
    }

    private fun applyFilters() {
        val amountFilter = binding.etMontoFiltro.text.toString().toDoubleOrNull()
        val filtered = MarketOfferRepository.offers.filter { offer ->
            val activeAndExternal = offer.status == "ACTIVA" && !offer.isOwnOffer
            val operationMatches = offer.operation == selectedOperation
            val currencyMatches = selectedCurrency == "TODAS" ||
                offer.fromCurrency == selectedCurrency ||
                offer.toCurrency == selectedCurrency
            val paymentMatches = selectedPayment == "TODOS" || selectedPayment in offer.paymentMethods
            val amountMatches = amountFilter == null ||
                (amountFilter >= offer.minAmount && amountFilter <= offer.amount)
            val reputationMatches = offer.reputation >= minReputation

            activeAndExternal && operationMatches && currencyMatches && paymentMatches && amountMatches && reputationMatches
        }

        adapter.updateData(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.tvResultCount.text = "${filtered.size} ofertas disponibles"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MarketOfferAdapter(
    private var items: List<MarketOffer>,
    private val onDetailClick: (MarketOffer) -> Unit
) : RecyclerView.Adapter<MarketOfferAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMarketOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarketOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val symbol = currencySymbol(item.toCurrency)

        with(holder.binding) {
            tvUserName.text = item.user
            tvUserStats.text = "${item.orders} ordenes | ${item.reputation}% exito"
            tvOperation.text = item.operation
            tvOperation.setTextColor(if (item.operation == "VENTA") Color.parseColor("#C62828") else Color.parseColor("#2E7D32"))
            tvPairValue.text = "${item.fromCurrency} / ${item.toCurrency}"
            tvPriceValue.text = "$symbol ${"%.4f".format(item.rate)}"
            tvAmountValue.text = "${currencySymbol(item.fromCurrency)} ${"%,.2f".format(item.amount)} ${item.fromCurrency}"
            tvMinValue.text = "${currencySymbol(item.fromCurrency)} ${"%,.2f".format(item.minAmount)}"
            tvMethodsValue.text = item.paymentMethods.joinToString(", ")
            btnViewDetail.text = "Ver detalle"
            btnViewDetail.setOnClickListener { onDetailClick(item) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MarketOffer>) {
        items = newItems
        notifyDataSetChanged()
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
