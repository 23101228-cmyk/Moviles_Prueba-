package com.soltis.mya.mercado

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
import android.os.Bundle
import com.soltis.mya.data.LocalOfferStore
import com.soltis.mya.data.LocalTransactionStore
import com.soltis.mya.data.P2pTransaction
import com.soltis.mya.databinding.FragmentOperationsBinding
import com.soltis.mya.databinding.ItemOperationBinding
import com.soltis.mya.wallet.WalletViewModel

class OperationsFragment : Fragment() {
    private var _binding: FragmentOperationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var transactionStore: LocalTransactionStore
    private lateinit var offerStore: LocalOfferStore
    private val walletViewModel: WalletViewModel by activityViewModels()
    private lateinit var adapter: OperationsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOperationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionStore = LocalTransactionStore(requireContext())
        offerStore = LocalOfferStore(requireContext())
        walletViewModel.initialize(requireContext())
        adapter = OperationsAdapter(emptyList(), ::handlePrimary, ::handleSecondary)
        binding.rvOperations.layoutManager = LinearLayoutManager(context)
        binding.rvOperations.adapter = adapter
        binding.btnBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(com.soltis.mya.R.id.nav_mercado)
            }
        }
        load()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) load()
    }

    private fun load() {
        val items = transactionStore.getMine()
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        adapter.update(items)
    }

    private fun handlePrimary(tx: P2pTransaction) {
        when (tx.status) {
            P2pTransaction.STATUS_PENDING_PROOF -> {
                transactionStore.updateStatus(tx.id, P2pTransaction.STATUS_PENDING_CONFIRMATION, proofAttached = true)
                Toast.makeText(context, "Comprobante simulado adjuntado", Toast.LENGTH_SHORT).show()
                load()
            }
            P2pTransaction.STATUS_PENDING_CONFIRMATION -> confirmTransaction(tx)
            else -> Toast.makeText(context, "Operacion sin acciones pendientes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmTransaction(tx: P2pTransaction) {
        val ok = if (tx.operationType == "VENTA") {
            walletViewModel.receiveExternalPurchase(tx.fromCurrency, tx.amount, tx.offerId)
        } else {
            walletViewModel.completeExternalSaleHold(tx.fromCurrency, tx.amount, tx.offerId)
        }
        if (ok) {
            transactionStore.updateStatus(tx.id, P2pTransaction.STATUS_COMPLETED)
            Toast.makeText(context, "Operacion completada", Toast.LENGTH_SHORT).show()
            load()
        } else {
            Toast.makeText(context, "No se pudo completar la operacion", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSecondary(tx: P2pTransaction) {
        if (tx.status == P2pTransaction.STATUS_COMPLETED || tx.status == P2pTransaction.STATUS_CANCELLED) {
            Toast.makeText(context, "Operacion cerrada", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Gestionar operacion")
            .setMessage("Puedes cancelar la operacion o abrir una disputa si el pago no llego.")
            .setNegativeButton("Cancelar operacion") { _, _ -> cancelTransaction(tx) }
            .setPositiveButton("Abrir disputa") { _, _ ->
                transactionStore.updateStatus(
                    tx.id,
                    P2pTransaction.STATUS_DISPUTE,
                    disputeReason = "Pago no recibido o divisa no liberada"
                )
                Toast.makeText(context, "Disputa registrada", Toast.LENGTH_SHORT).show()
                load()
            }
            .show()
    }

    private fun cancelTransaction(tx: P2pTransaction) {
        transactionStore.updateStatus(tx.id, P2pTransaction.STATUS_CANCELLED)
        offerStore.reopenOffer(tx.offerId)
        if (tx.operationType == "COMPRA") {
            walletViewModel.releaseOfferHold(tx.fromCurrency, tx.amount)
        }
        Toast.makeText(context, "Operacion cancelada. La oferta vuelve al mercado.", Toast.LENGTH_LONG).show()
        load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class OperationsAdapter(
    private var items: List<P2pTransaction>,
    private val onPrimary: (P2pTransaction) -> Unit,
    private val onSecondary: (P2pTransaction) -> Unit
) : RecyclerView.Adapter<OperationsAdapter.VH>() {

    class VH(val binding: ItemOperationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(ItemOperationBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tx = items[position]
        with(holder.binding) {
            tvOperationId.text = tx.id
            tvStatus.text = tx.status
            tvSummary.text = "${if (tx.operationType == "VENTA") "Compra" else "Venta"} ${tx.fromCurrency}/${tx.toCurrency} - ${"%.2f".format(tx.amount)} ${tx.fromCurrency}"
            tvPayment.text = "Metodo: ${tx.paymentMethod} | Total: ${currencySymbol(tx.toCurrency)} ${"%.2f".format(tx.total)}"
            btnPrimary.visibility = View.VISIBLE
            btnPrimary.text = when (tx.status) {
                P2pTransaction.STATUS_PENDING_PROOF -> "Adjuntar comprobante"
                P2pTransaction.STATUS_PENDING_CONFIRMATION -> "Simular confirmacion"
                else -> "Ver resumen"
            }
            btnSecondary.text = if (tx.status == P2pTransaction.STATUS_DISPUTE) "En disputa" else "Cancelar/Disputa"
            btnPrimary.setOnClickListener { onPrimary(tx) }
            btnSecondary.setOnClickListener { onSecondary(tx) }
        }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<P2pTransaction>) {
        items = newItems
        notifyDataSetChanged()
    }
}
