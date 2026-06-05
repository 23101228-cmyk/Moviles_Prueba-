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
import com.soltis.mya.databinding.FragmentDepositBinding

class DepositFragment : Fragment() {

    private var _binding: FragmentDepositBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by activityViewModels()

    // Moneda seleccionada por defecto
    private var selectedCurrency = "PEN"

    // Saldos actuales simulados (sincroniza con tu ViewModel/repo real)
    private val currentBalances = mapOf(
        "PEN" to 8240.50,
        "USD" to 2150.00,
        "EUR" to 66.28
    )

    private val currencySymbols = mapOf(
        "PEN" to "S/",
        "USD" to "$",
        "EUR" to "€"
    )

    private val currencyLabels = mapOf(
        "PEN" to "PEN (Soles)",
        "USD" to "USD (Dólares)",
        "EUR" to "EUR (Euros)"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepositBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChips()
        setupAmountWatcher()
        setupClickListeners()
        updateSummary(0.0)
    }

    // ── Chip selection ────────────────────────────────────────────────────────

    private fun setupChips() {
        binding.chipPen.setOnClickListener { selectCurrency("PEN") }
        binding.chipUsd.setOnClickListener { selectCurrency("USD") }
        binding.chipEur.setOnClickListener { selectCurrency("EUR") }
        selectCurrency("PEN") // default
    }

    private fun selectCurrency(currency: String) {
        selectedCurrency = currency

        // Reset all chips to unselected
        listOf(binding.chipPen, binding.chipUsd, binding.chipEur).forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
        }

        // Highlight selected
        val selectedChip = when (currency) {
            "PEN" -> binding.chipPen
            "USD" -> binding.chipUsd
            "EUR" -> binding.chipEur
            else  -> binding.chipPen
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected)

        // Update prefix and summary
        binding.tilMonto.prefixText = currencySymbols[currency]
        binding.tvResumenMoneda.text = currencyLabels[currency]

        val amount = binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
        updateSummary(amount)
    }

    // ── Live amount watcher ───────────────────────────────────────────────────

    private fun setupAmountWatcher() {
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val amount = s.toString().toDoubleOrNull() ?: 0.0
                updateSummary(amount)
            }
        })
    }

    private fun updateSummary(amount: Double) {
        val symbol  = currencySymbols[selectedCurrency] ?: "S/"
        val current = viewModel.balances.value?.get(selectedCurrency) ?: 0.0
        val after   = current + amount

        binding.tvResumenMonto.text      = "$symbol ${"%.2f".format(amount)}"
        binding.tvResumenSaldoFinal.text = "$symbol ${"%.2f".format(after)}"
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun setupClickListeners() {

        binding.btnViewMovements.setOnClickListener {
            findNavController().navigate(R.id.action_nav_deposit_to_nav_movements)
        }

        binding.tvCancelar.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirmar.setOnClickListener {
            val amountStr = binding.etMonto.text.toString()
            val amount    = amountStr.toDoubleOrNull()
            val symbol    = currencySymbols[selectedCurrency] ?: "S/"

            when {
                amount == null || amount <= 0 -> {
                    binding.tilMonto.error = "Ingresa un monto válido"
                }
                amount < 10.0 -> {
                    binding.tilMonto.error = "El monto mínimo es ${symbol} 10.00"
                }
                amount > 30000.0 -> {
                    binding.tilMonto.error = "El monto máximo es ${symbol} 30,000.00"
                }
                else -> {
                    binding.tilMonto.error = null
                    
                    // Procesar el depósito en el ViewModel
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
