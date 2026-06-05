package com.soltis.mya.mercado

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.soltis.mya.databinding.FragmentOfferDetailBinding

class OfferDetailFragment : Fragment() {
    private var _binding: FragmentOfferDetailBinding? = null
    private val binding get() = _binding!!
    private var offer: MarketOffer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOfferDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val offerId = requireArguments().getString(ARG_OFFER_ID).orEmpty()
        offer = MarketOfferRepository.findById(offerId)

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
                Toast.makeText(
                    context,
                    "Transaccion simulada iniciada con ${selectedPaymentMethod()}",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_OFFER_ID = "offerId"
    }
}
