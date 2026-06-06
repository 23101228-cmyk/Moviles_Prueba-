package com.soltis.mya.ofertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.R
import com.soltis.mya.data.LocalOfferStore
import com.soltis.mya.data.P2pOffer
import com.soltis.mya.databinding.FragmentOfertasBinding
import com.soltis.mya.databinding.ItemMyOfferBinding
import com.soltis.mya.wallet.WalletViewModel

data class MyOffer(val type: String, val asset: String, val amount: String, val price: String, val status: String)

class OfertasFragment : Fragment() {
    private var _binding: FragmentOfertasBinding? = null
    private val binding get() = _binding!!
    private val walletViewModel: WalletViewModel by activityViewModels()
    private lateinit var offerStore: LocalOfferStore
    private lateinit var adapter: MyOfferAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOfertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offerStore = LocalOfferStore(requireContext())
        walletViewModel.initialize(requireContext())

        adapter = MyOfferAdapter(emptyList()) { offer -> confirmCancelOffer(offer) }
        binding.rvOfertas.layoutManager = LinearLayoutManager(context)
        binding.rvOfertas.adapter = adapter

        binding.fabAddOffer.setOnClickListener {
            findNavController().navigate(R.id.action_nav_ofertas_to_publishOfferFragment)
        }

        loadOffers()
    }

    override fun onResume() {
        super.onResume()
        if (::offerStore.isInitialized) loadOffers()
    }

    private fun loadOffers() {
        val offers = offerStore.getMyOffers()
        binding.emptyState.visibility = if (offers.isEmpty()) View.VISIBLE else View.GONE
        binding.rvOfertas.visibility = if (offers.isEmpty()) View.GONE else View.VISIBLE
        adapter.updateData(offers)
    }

    private fun confirmCancelOffer(offer: P2pOffer) {
        if (offer.status != P2pOffer.STATUS_ACTIVE) {
            Toast.makeText(context, "La oferta ya no esta activa", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Cancelar oferta")
            .setMessage("Se cancelara ${offer.id}. Si era una venta, se liberara el saldo retenido.")
            .setNegativeButton("No", null)
            .setPositiveButton("Si, cancelar") { _, _ ->
                val cancelled = offerStore.cancelOffer(offer.id)
                if (cancelled != null && cancelled.type == "VENTA") {
                    walletViewModel.releaseOfferHold(cancelled.fromCurrency, cancelled.amount)
                }
                Toast.makeText(context, "Oferta cancelada", Toast.LENGTH_SHORT).show()
                loadOffers()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MyOfferAdapter(
    private var items: List<P2pOffer>,
    private val onCancelClick: (P2pOffer) -> Unit
) : RecyclerView.Adapter<MyOfferAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMyOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val amountSymbol = currencySymbol(item.fromCurrency)
        val priceSymbol = currencySymbol(item.toCurrency)
        with(holder.binding) {
            tvOfferType.text = item.type
            tvOfferAsset.text = "${item.fromCurrency} / ${item.toCurrency}"
            tvOfferAmount.text = "$amountSymbol ${"%,.2f".format(item.amount)} ${item.fromCurrency}"
            tvOfferPrice.text = "$priceSymbol ${"%.4f".format(item.rate)}"
            tvOfferStatus.text = item.status
            tvOfferMethods.text = "Metodos: ${item.paymentMethods.joinToString(", ")}"
            btnCancelOffer.visibility = if (item.status == P2pOffer.STATUS_ACTIVE) View.VISIBLE else View.GONE
            btnCancelOffer.setOnClickListener { onCancelClick(item) }

            if (item.type == "VENTA") {
                tvOfferType.setTextColor(0xFFF6465D.toInt())
                tvOfferType.setBackgroundColor(0xFFFFEBEE.toInt())
            } else {
                tvOfferType.setTextColor(0xFF0ECB81.toInt())
                tvOfferType.setBackgroundColor(0xFFE8F5E9.toInt())
            }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<P2pOffer>) {
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
