package com.soltis.mya.wallet

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentWithdrawBinding

class WithdrawFragment : Fragment() {

    private var _binding: FragmentWithdrawBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletViewModel by activityViewModels()

    private var selectedCurrency = "PEN"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWithdrawBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrencyChips()
        setupAmountWatcher()
        setupClickListeners()

        viewModel.balances.observe(viewLifecycleOwner) { updateSummary() }
        viewModel.heldBalances.observe(viewLifecycleOwner) { updateSummary() }
        selectCurrency("PEN")
    }

    private fun setupCurrencyChips() {
        binding.chipPen.setOnClickListener { selectCurrency("PEN") }
        binding.chipUsd.setOnClickListener { selectCurrency("USD") }
        binding.chipEur.setOnClickListener { selectCurrency("EUR") }
    }

    private fun selectCurrency(currency: String) {
        selectedCurrency = currency
        listOf(binding.chipPen, binding.chipUsd, binding.chipEur).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
        }
        when (currency) {
            "PEN" -> binding.chipPen
            "USD" -> binding.chipUsd
            else -> binding.chipEur
        }.setBackgroundResource(R.drawable.bg_chip_selected)

        binding.tilMonto.prefixText = currencySymbol(currency)
        updateSummary()
    }

    private fun setupAmountWatcher() {
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = updateSummary()
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvCancelar.setOnClickListener { findNavController().popBackStack() }
        binding.btnConfirmar.setOnClickListener { confirmWithdraw() }
    }

    private fun updateSummary() {
        val balances = viewModel.balances.value ?: emptyMap()
        val held = viewModel.heldBalances.value ?: emptyMap()
        val available = balances[selectedCurrency] ?: 0.0
        val heldAmount = held[selectedCurrency] ?: 0.0
        val amount = binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
        val remaining = available - amount
        val symbol = currencySymbol(selectedCurrency)

        binding.tvAvailable.text = "$symbol ${"%,.2f".format(available)}"
        binding.tvHeld.text = "$symbol ${"%,.2f".format(heldAmount)}"
        binding.tvWithdrawAmount.text = "$symbol ${"%,.2f".format(amount)}"
        binding.tvRemaining.text = "$symbol ${"%,.2f".format(remaining.coerceAtLeast(0.0))}"
    }

    private fun confirmWithdraw() {
        val amount = binding.etMonto.text.toString().toDoubleOrNull()
        val destination = binding.etDestino.text.toString().trim()
        val available = viewModel.balances.value?.get(selectedCurrency) ?: 0.0
        val symbol = currencySymbol(selectedCurrency)

        when {
            amount == null || amount <= 0 -> binding.tilMonto.error = "Ingresa un monto valido"
            amount > available -> binding.tilMonto.error = "No puedes retirar mas que tu saldo disponible"
            destination.isBlank() -> binding.tilDestino.error = "Ingresa un destino simulado"
            else -> {
                binding.tilMonto.error = null
                binding.tilDestino.error = null
                val success = viewModel.withdraw(selectedCurrency, amount, destination)
                if (success) {
                    Toast.makeText(context, "Retiro de $symbol ${"%.2f".format(amount)} confirmado", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
