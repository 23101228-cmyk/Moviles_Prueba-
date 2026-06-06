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
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.databinding.FragmentDepositBinding

class DepositFragment : Fragment() {

    private var _binding: FragmentDepositBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by activityViewModels()
    private lateinit var userStore: LocalUserStore
    private var selectedCurrency = "PEN"

    private val currencySymbols = mapOf("PEN" to "S/", "USD" to "$", "EUR" to "EUR")
    private val currencyLabels = mapOf("PEN" to "PEN (Soles)", "USD" to "USD (Dolares)", "EUR" to "EUR (Euros)")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDepositBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userStore = LocalUserStore(requireContext())
        viewModel.initialize(requireContext())

        setupChips()
        setupAmountWatcher()
        setupClickListeners()
        setupCardDefaults()
        updateSummary(0.0)
    }

    private fun setupCardDefaults() {
        binding.etCardHolder.setText(userStore.getCurrentUser()?.fullName.orEmpty())
    }

    private fun setupChips() {
        binding.chipPen.setOnClickListener { selectCurrency("PEN") }
        binding.chipUsd.setOnClickListener { selectCurrency("USD") }
        binding.chipEur.setOnClickListener { selectCurrency("EUR") }
        selectCurrency("PEN")
    }

    private fun selectCurrency(currency: String) {
        selectedCurrency = currency
        listOf(binding.chipPen, binding.chipUsd, binding.chipEur).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
        }

        val selectedChip = when (currency) {
            "PEN" -> binding.chipPen
            "USD" -> binding.chipUsd
            else -> binding.chipEur
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected)

        binding.tilMonto.prefixText = currencySymbols[currency]
        binding.tvResumenMoneda.text = currencyLabels[currency]
        updateSummary(binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0)
    }

    private fun setupAmountWatcher() {
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                updateSummary(s.toString().toDoubleOrNull() ?: 0.0)
            }
        })
    }

    private fun updateSummary(amount: Double) {
        val symbol = currencySymbols[selectedCurrency] ?: "S/"
        val current = viewModel.balances.value?.get(selectedCurrency) ?: 0.0
        val after = current + amount

        binding.tvResumenMonto.text = "$symbol ${"%.2f".format(amount)}"
        binding.tvResumenSaldoFinal.text = "$symbol ${"%.2f".format(after)}"
    }

    private fun setupClickListeners() {
        binding.btnViewMovements.setOnClickListener {
            findNavController().navigate(R.id.action_nav_deposit_to_nav_movements)
        }

        binding.tvCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirmar.setOnClickListener {
            confirmDeposit()
        }
    }

    private fun confirmDeposit() {
        val amount = binding.etMonto.text.toString().toDoubleOrNull()
        val symbol = currencySymbols[selectedCurrency] ?: "S/"

        when {
            amount == null || amount <= 0 -> binding.tilMonto.error = "Ingresa un monto valido"
            amount < 10.0 -> binding.tilMonto.error = "El monto minimo es $symbol 10.00"
            amount > 30000.0 -> binding.tilMonto.error = "El monto maximo es $symbol 30,000.00"
            !validateCardFields() -> Unit
            else -> {
                binding.tilMonto.error = null
                viewModel.addDeposit(selectedCurrency, amount)
                Toast.makeText(
                    context,
                    "Recarga de $symbol ${"%.2f".format(amount)} en $selectedCurrency confirmada",
                    Toast.LENGTH_LONG
                ).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun validateCardFields(): Boolean {
        var valid = true
        val cardNumber = binding.etCardNumber.text.toString().filter { it.isDigit() }
        val holder = binding.etCardHolder.text.toString().trim()
        val expiry = binding.etCardExpiry.text.toString().trim()
        val cvv = binding.etCardCvv.text.toString().filter { it.isDigit() }
        val profileName = userStore.getCurrentUser()?.fullName.orEmpty()

        if (cardNumber.length != 16) {
            binding.tilCardNumber.error = "Ingresa 16 digitos"
            valid = false
        } else {
            binding.tilCardNumber.error = null
        }

        if (holder.isBlank()) {
            binding.tilCardHolder.error = "Ingresa el titular"
            valid = false
        } else if (profileName.isNotBlank() && !holder.equals(profileName, ignoreCase = true)) {
            binding.tilCardHolder.error = "Debe coincidir con el perfil"
            valid = false
        } else {
            binding.tilCardHolder.error = null
        }

        if (!Regex("\\d{2}/\\d{2}").matches(expiry)) {
            binding.tilCardExpiry.error = "Usa MM/AA"
            valid = false
        } else {
            binding.tilCardExpiry.error = null
        }

        if (cvv.length !in 3..4) {
            binding.tilCardCvv.error = "CVV invalido"
            valid = false
        } else {
            binding.tilCardCvv.error = null
        }

        return valid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
