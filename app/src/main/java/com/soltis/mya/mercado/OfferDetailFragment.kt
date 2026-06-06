package com.soltis.mya.mercado

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.soltis.mya.data.LocalOfferStore
import com.soltis.mya.data.LocalTransactionStore
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.data.P2pOffer
import com.soltis.mya.data.P2pTransaction
import com.soltis.mya.databinding.FragmentOfferDetailBinding
import com.soltis.mya.wallet.WalletViewModel

class OfferDetailFragment : Fragment() {
    private var _binding: FragmentOfferDetailBinding? = null
    private val binding get() = _binding!!
    private var offer: MarketOffer? = null
    private var sourceOffer: P2pOffer? = null
    private lateinit var offerStore: LocalOfferStore
    private lateinit var transactionStore: LocalTransactionStore
    private lateinit var userStore: LocalUserStore
    private val walletViewModel: WalletViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOfferDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        offerStore = LocalOfferStore(requireContext())
        transactionStore = LocalTransactionStore(requireContext())
        userStore = LocalUserStore(requireContext())
        walletViewModel.initialize(requireContext())

        val offerId = requireArguments().getString(ARG_OFFER_ID).orEmpty()
        sourceOffer = offerStore.findById(offerId)
        offer = sourceOffer?.toMarketOffer()

        if (offer == null) {
            Toast.makeText(context, "Oferta no disponible", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        bindOffer(offer!!)
        setupListeners()
    }

    private fun bindOffer(item: MarketOffer) {
        binding.tvOfferCode.text = item.id
        binding.tvUserName.text = item.user
        binding.tvUserStats.text = "${item.orders} operaciones | ${item.reputation}% reputacion"
        binding.tvStatus.text = item.status
        binding.tvOperation.text = item.operation
        binding.tvPair.text = "${item.fromCurrency} / ${item.toCurrency}"
        binding.tvRate.text = "${currencySymbol(item.toCurrency)} ${"%.4f".format(item.rate)}"
        binding.tvAmount.text = "${currencySymbol(item.fromCurrency)} ${"%,.2f".format(item.amount)} ${item.fromCurrency}"
        binding.tvMinimum.text = "${currencySymbol(item.fromCurrency)} ${"%,.2f".format(item.minAmount)} ${item.fromCurrency}"
        binding.tvTimeLimit.text = "${item.timeLimitMinutes} minutos"
        binding.tilAmount.prefixText = currencySymbol(item.fromCurrency)
        binding.tilAmount.suffixText = item.fromCurrency

        binding.radioPaymentMethods.removeAllViews()
        item.paymentMethods.forEachIndexed { index, method ->
            val radio = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = method
                textSize = 14f
                isChecked = index == 0
            }
            binding.radioPaymentMethods.addView(radio)
        }

        updatePaymentInfo()
        calculateTotal()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancelBeforeStart.setOnClickListener { findNavController().popBackStack() }
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = calculateTotal()
        })
        binding.radioPaymentMethods.setOnCheckedChangeListener { _, _ -> updatePaymentInfo() }
        binding.btnStartTransaction.setOnClickListener { startTransaction() }
    }

    private fun selectedPaymentMethod(): String {
        val id = binding.radioPaymentMethods.checkedRadioButtonId
        return binding.radioPaymentMethods.findViewById<RadioButton>(id)?.text?.toString().orEmpty()
    }

    private fun updatePaymentInfo() {
        val method = selectedPaymentMethod()
        binding.tvPaymentInfo.text = if (method == "Wallet") {
            "Se validara saldo disponible antes de iniciar la transaccion."
        } else {
            "Los datos de pago externo se mostraran despues de iniciar la transaccion."
        }
    }

    private fun calculateTotal() {
        val item = offer ?: return
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val total = amount * item.rate

        binding.tvTotal.text = "${currencySymbol(item.toCurrency)} ${"%,.2f".format(total)} ${item.toCurrency}"
        binding.tvTotalLabel.text = if (item.operation == "VENTA") "Total a pagar" else "Total a recibir"
    }

    private fun startTransaction() {
        val item = offer ?: return
        val amount = binding.etAmount.text.toString().toDoubleOrNull()

        when {
            amount == null || amount <= 0 -> {
                binding.tilAmount.error = "Ingresa un monto valido"
            }
            amount < item.minAmount -> {
                binding.tilAmount.error = "El monto minimo es ${currencySymbol(item.fromCurrency)} ${"%.2f".format(item.minAmount)}"
            }
            amount > item.amount -> {
                binding.tilAmount.error = "El monto supera lo disponible en la oferta"
            }
            selectedPaymentMethod().isBlank() -> {
                Toast.makeText(context, "Selecciona un metodo de pago", Toast.LENGTH_SHORT).show()
            }
            else -> {
                binding.tilAmount.error = null
                beginTransaction(item, amount)
            }
        }
    }

    private fun beginTransaction(item: MarketOffer, amount: Double) {
        val currentUser = userStore.getCurrentUser()
        val source = sourceOffer
        if (currentUser == null || source == null) {
            Toast.makeText(context, "No se pudo iniciar la operacion", Toast.LENGTH_SHORT).show()
            return
        }

        if (source.status != P2pOffer.STATUS_ACTIVE) {
            Toast.makeText(context, "La oferta ya no esta activa", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        val method = selectedPaymentMethod()
        val total = amount * item.rate
        val isWallet = method == "Wallet"
        val status = if (isWallet) P2pTransaction.STATUS_COMPLETED else P2pTransaction.STATUS_PENDING_PROOF

        val closed = offerStore.closeForOperation(item.id)
        if (closed == null) {
            Toast.makeText(context, "La oferta ya fue tomada por otra operacion", Toast.LENGTH_LONG).show()
            return
        }

        if (isWallet) {
            val success = walletViewModel.completeMarketWalletOperation(
                offerType = item.operation,
                fromCurrency = item.fromCurrency,
                toCurrency = item.toCurrency,
                amount = amount,
                total = total,
                offerId = item.id
            )
            if (!success) {
                offerStore.reopenOffer(item.id)
                Toast.makeText(context, "Saldo insuficiente en Wallet para iniciar esta operacion", Toast.LENGTH_LONG).show()
                return
            }
        } else if (item.operation == "COMPRA") {
            val held = walletViewModel.holdForExternalSale(item.fromCurrency, amount, item.id)
            if (!held) {
                offerStore.reopenOffer(item.id)
                Toast.makeText(context, "Saldo insuficiente para vender ${item.fromCurrency}. Se requiere retener el monto en Wallet.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val transaction = P2pTransaction(
            id = transactionStore.nextTransactionId(),
            offerId = item.id,
            takerEmail = currentUser.email,
            makerEmail = source.ownerEmail,
            operationType = item.operation,
            fromCurrency = item.fromCurrency,
            toCurrency = item.toCurrency,
            amount = amount,
            rate = item.rate,
            total = total,
            paymentMethod = method,
            status = status,
            proofRequired = !isWallet,
            proofAttached = false
        )
        transactionStore.add(transaction)

        if (isWallet) {
            showWalletCompletedDialog(transaction)
        } else {
            showExternalPaymentDialog(transaction, source)
        }
    }

    private fun showWalletCompletedDialog(transaction: P2pTransaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Operacion completada")
            .setMessage("La operacion ${transaction.id} se completo con Wallet.\n\nLa oferta ${transaction.offerId} fue cerrada.")
            .setPositiveButton("Aceptar") { _, _ -> findNavController().popBackStack() }
            .show()
    }

    private fun showExternalPaymentDialog(transaction: P2pTransaction, source: P2pOffer) {
        AlertDialog.Builder(requireContext())
            .setTitle("Pago externo requerido")
            .setMessage(buildPaymentMessage(transaction, source))
            .setNegativeButton("Cancelar operacion") { _, _ ->
                cancelPendingExternal(transaction)
            }
            .setPositiveButton("Adjuntar comprobante") { _, _ ->
                transactionStore.updateStatus(
                    transaction.id,
                    P2pTransaction.STATUS_PENDING_CONFIRMATION,
                    proofAttached = true
                )
                showSimulatedConfirmationDialog(transaction)
            }
            .show()
    }

    private fun cancelPendingExternal(transaction: P2pTransaction) {
        transactionStore.updateStatus(transaction.id, P2pTransaction.STATUS_CANCELLED)
        offerStore.reopenOffer(transaction.offerId)
        if (transaction.operationType == "COMPRA") {
            walletViewModel.releaseOfferHold(transaction.fromCurrency, transaction.amount)
        }
        Toast.makeText(context, "Operacion cancelada. La oferta vuelve al mercado.", Toast.LENGTH_LONG).show()
        findNavController().popBackStack()
    }

    private fun showSimulatedConfirmationDialog(transaction: P2pTransaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Comprobante adjuntado")
            .setMessage("La operacion quedo pendiente de confirmacion.\n\nPara la demo puedes simular que el ofertante confirmo el pago.")
            .setNegativeButton("Terminar luego") { _, _ -> findNavController().popBackStack() }
            .setPositiveButton("Simular confirmacion") { _, _ ->
                transactionStore.updateStatus(transaction.id, P2pTransaction.STATUS_COMPLETED)
                if (transaction.operationType == "VENTA") {
                    walletViewModel.receiveExternalPurchase(transaction.fromCurrency, transaction.amount, transaction.offerId)
                } else {
                    walletViewModel.completeExternalSaleHold(transaction.fromCurrency, transaction.amount, transaction.offerId)
                }
                Toast.makeText(context, "Operacion completada por confirmacion simulada", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
            .show()
    }

    private fun buildPaymentMessage(transaction: P2pTransaction, source: P2pOffer): String {
        val maker = userStore.getUserByEmail(source.ownerEmail)
        val paymentData = when (transaction.paymentMethod) {
            "Yape" -> "Yape del ofertante: ${maker?.yape?.ifBlank { null } ?: "987654321"}"
            "Plin" -> "Plin del ofertante: ${maker?.plin?.ifBlank { null } ?: "987654321"}"
            "Transferencia" -> {
                val card = maker?.bankCards?.firstOrNull()
                if (card != null) {
                    "Banco: ${card.bank}\nCuenta: ${userStore.maskAccount(card.account)}\nCCI: ${userStore.maskCci(card.cci)}"
                } else {
                    "Banco: BCP\nCuenta: 1911********8912\nCCI: ****************9012"
                }
            }
            else -> "Metodo: ${transaction.paymentMethod}"
        }

        return "Operacion ${transaction.id}\nOferta ${transaction.offerId}\nMonto: ${"%.2f".format(transaction.amount)} ${transaction.fromCurrency}\nTotal: ${currencySymbol(transaction.toCurrency)} ${"%.2f".format(transaction.total)} ${transaction.toCurrency}\n\n$paymentData\n\nAdjunta un comprobante simulado para continuar."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_OFFER_ID = "offerId"
    }
}

private fun P2pOffer.toMarketOffer(): MarketOffer {
    return MarketOffer(
        id = id,
        user = ownerName,
        reputation = 98,
        orders = 12,
        operation = type,
        fromCurrency = fromCurrency,
        toCurrency = toCurrency,
        rate = rate,
        amount = amount,
        minAmount = minAmount,
        paymentMethods = paymentMethods,
        status = status,
        timeLimitMinutes = 15
    )
}
