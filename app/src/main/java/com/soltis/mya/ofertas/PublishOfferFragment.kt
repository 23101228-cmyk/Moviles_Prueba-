package com.soltis.mya.ofertas

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentPublishOfferBinding

import androidx.fragment.app.activityViewModels
import com.soltis.mya.wallet.WalletViewModel

class PublishOfferFragment : Fragment() {

    private var _binding: FragmentPublishOfferBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OfferViewModel by activityViewModels()
    private val walletViewModel: WalletViewModel by activityViewModels()

    // Estado
    private var isBuyMode = true                          // true = Compra, false = Venta
    private var selectedCurrencyFrom = "USD"
    private var selectedCurrencyTo = "PEN"
    private val selectedPaymentMethods = mutableSetOf<String>()

    // Simulación de actualización en tiempo real
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val ratesUpdater = object : Runnable {
        override fun run() {
            updateExchangeRate()
            handler.postDelayed(this, 10000) // Actualizar cada 10 seg
        }
    }

    // Monedas disponibles
    private val currencies = listOf(
        CurrencyInfo("PEN", "Soles", R.drawable.ic_flag_pen, "S/"),
        CurrencyInfo("USD", "Dólares", R.drawable.ic_flag_usd, "$"),
        CurrencyInfo("EUR", "Euros", R.drawable.ic_flag_eur, "€")
    )

    // Tipos de cambio base (simulados - representarían datos de internet)
    private val exchangeRates = mapOf(
        "USD_PEN" to 3.7540,
        "PEN_USD" to 1 / 3.7540,
        "EUR_PEN" to 4.0520,
        "PEN_EUR" to 1 / 4.0520,
        "EUR_USD" to 1.0850,
        "USD_EUR" to 1 / 1.0850
    )

    // Saldo simulado del usuario por moneda
    private val userBalance = mapOf("PEN" to 1345.78, "USD" to 2150.00, "EUR" to 66.28)

    data class CurrencyInfo(val code: String, val name: String, val flagRes: Int, val symbol: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPublishOfferBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToggle()
        setupCurrencySelectors()
        setupPaymentChips()
        setupAmountWatcher()
        setupClickListeners()
        applyMode()
        
        // Iniciar actualizaciones automáticas
        handler.post(ratesUpdater)
        
        // Bloquear edición manual del tipo de cambio
        binding.etTipoCambio.isEnabled = false
        binding.etTipoCambio.isFocusable = false
        binding.etTipoCambio.alpha = 0.8f
    }

    // ── Currency Selection ────────────────────────────────────────────────────

    private fun setupCurrencySelectors() {
        binding.layoutCurrencyFrom.setOnClickListener {
            showCurrencySelector(true)
        }
        binding.layoutCurrencyTo.setOnClickListener {
            showCurrencySelector(false)
        }
        binding.imgSwapCurrencies?.setOnClickListener {
            val temp = selectedCurrencyFrom
            selectedCurrencyFrom = selectedCurrencyTo
            selectedCurrencyTo = temp
            applyMode()
        }
    }

    private fun showCurrencySelector(isFrom: Boolean) {
        val codes = currencies.map { "${it.code} - ${it.name}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar moneda")
            .setItems(codes) { _, which ->
                val selected = currencies[which].code
                if (isFrom) {
                    if (selected == selectedCurrencyTo) {
                        selectedCurrencyTo = selectedCurrencyFrom
                    }
                    selectedCurrencyFrom = selected
                } else {
                    if (selected == selectedCurrencyFrom) {
                        selectedCurrencyFrom = selectedCurrencyTo
                    }
                    selectedCurrencyTo = selected
                }
                applyMode()
            }
            .show()
    }

    private fun updateExchangeRate() {
        val pair = "${selectedCurrencyFrom}_${selectedCurrencyTo}"
        val rate = exchangeRates[pair] ?: 1.0
        
        // En un escenario real, aquí se llamaría a una API de mercado.
        // Simulamos una fluctuación para dar la sensación de "tiempo real".
        val fluctuation = (Math.random() - 0.5) * 0.0006
        val finalRate = rate + fluctuation
        
        binding.etTipoCambio.setText("%.4f".format(finalRate))
        binding.tilTipoCambio.suffixText = "${selectedCurrencyTo}/${selectedCurrencyFrom}"
        binding.tilTipoCambio.helperText = "⚡ Tipo de cambio actualizado en vivo"
        
        calculateTotal()
    }

    private fun calculateTotal() {
        val amount = binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
        val rate = binding.etTipoCambio.text.toString().toDoubleOrNull() ?: 0.0
        val total = amount * rate
        
        val toInfo = currencies.find { it.code == selectedCurrencyTo }!!
        
        if (amount > 0) {
            binding.tvTotalCalculado.visibility = View.VISIBLE
            if (isBuyMode) {
                // Si publico COMPRA de USD usando PEN: El total es lo que voy a PAGAR en PEN.
                binding.tvTotalCalculado.text = "Total a pagar: ${toInfo.symbol} ${"%.2f".format(total)} ${toInfo.code}"
                binding.tvTotalCalculado.setTextColor(Color.parseColor("#C62828")) // Rojo para egreso
            } else {
                // Si publico VENTA de USD por PEN: El total es lo que voy a RECIBIR en PEN.
                binding.tvTotalCalculado.text = "Total a recibir: ${toInfo.symbol} ${"%.2f".format(total)} ${toInfo.code}"
                binding.tvTotalCalculado.setTextColor(Color.parseColor("#2E7D32")) // Verde para ingreso
            }
        } else {
            binding.tvTotalCalculado.visibility = View.GONE
        }
    }

    // ── Compra / Venta toggle ─────────────────────────────────────────────────

    private fun setupToggle() {
        binding.tabCompra.setOnClickListener {
            isBuyMode = true
            applyMode()
        }
        binding.tabVenta.setOnClickListener {
            isBuyMode = false
            applyMode()
        }
    }

    private fun applyMode() {
        val fromInfo = currencies.find { it.code == selectedCurrencyFrom }!!
        val toInfo = currencies.find { it.code == selectedCurrencyTo }!!

        if (isBuyMode) {
            // Tab visual
            binding.tabCompra.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabCompra.setTextColor(Color.WHITE)
            binding.tabCompra.typeface = android.graphics.Typeface.DEFAULT_BOLD
            binding.tabVenta.setBackgroundResource(android.R.color.transparent)
            binding.tabVenta.setTextColor(Color.parseColor("#888888"))
            binding.tabVenta.typeface = android.graphics.Typeface.DEFAULT

            binding.btnPublicar.text = "Publicar oferta de compra"
        } else {
            // Tab visual
            binding.tabVenta.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabVenta.setTextColor(Color.WHITE)
            binding.tabVenta.typeface = android.graphics.Typeface.DEFAULT_BOLD
            binding.tabCompra.setBackgroundResource(android.R.color.transparent)
            binding.tabCompra.setTextColor(Color.parseColor("#888888"))
            binding.tabCompra.typeface = android.graphics.Typeface.DEFAULT

            binding.btnPublicar.text = "Publicar oferta de venta"
        }

        // Actualizar UI de monedas
        binding.imgFlagFrom.setImageResource(fromInfo.flagRes)
        binding.tvCurrencyFromCode.text = fromInfo.code
        binding.tvCurrencyFromName.text = fromInfo.name

        binding.imgFlagTo.setImageResource(toInfo.flagRes)
        binding.tvCurrencyToCode.text = toInfo.code
        binding.tvCurrencyToName.text = toInfo.name

        // Actualizar prefijos y avisos
        binding.tilMonto.prefixText = fromInfo.symbol
        binding.tilLimiteMin.prefixText = fromInfo.symbol
        binding.tilLimiteMax.prefixText = fromInfo.symbol
        binding.tilTipoCambio.prefixText = toInfo.symbol

        val balanceToUse = if (isBuyMode) toInfo.code else fromInfo.code
        val bal = userBalance[balanceToUse] ?: 0.0
        binding.tvInfoBanner.text = if (isBuyMode) {
            "Saldo necesario en ${toInfo.code}: ${toInfo.symbol} ${"%.2f".format(bal)}\nEste es el saldo que usarás para pagar la compra."
        } else {
            "Saldo disponible en ${fromInfo.code}: ${fromInfo.symbol} ${"%.2f".format(bal)}\nEste es el saldo que estás ofreciendo vender."
        }

        updateExchangeRate()
        updateAmountSuffix()
    }

    // ── Amount watcher: actualiza suffix y total en tiempo real ───────────────

    private fun setupAmountWatcher() {
        binding.etMonto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { 
                updateAmountSuffix()
                calculateTotal()
            }
        })
    }

    private fun updateAmountSuffix() {
        val raw  = binding.etMonto.text.toString().toDoubleOrNull() ?: 0.0
        val code = selectedCurrencyFrom
        binding.tilMonto.suffixText = "${"%.2f".format(raw)} $code"
    }

    // ── Payment method chips ──────────────────────────────────────────────────

    private fun setupPaymentChips() {
        val chips = listOf(
            "Yape"          to binding.chipYape,
            "Plin"          to binding.chipPlin,
            "Transferencia" to binding.chipTransferencia,
            "Wallet"        to binding.chipWallet
        )

        chips.forEach { (method, chip) ->
            chip.setOnClickListener {
                // Deseleccionar todos
                selectedPaymentMethods.clear()
                chips.forEach { it.second.setBackgroundResource(R.drawable.bg_chip_unselected) }

                // Seleccionar el actual
                selectedPaymentMethods.add(method)
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
            }
        }
        // Yape seleccionado por defecto
        selectedPaymentMethods.add("Yape")
        binding.chipYape.setBackgroundResource(R.drawable.bg_chip_selected)
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private fun setupClickListeners() {

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnPublicar.setOnClickListener {
            if (validateFields()) {
                val monto      = binding.etMonto.text.toString().toDouble()
                val tipoCambio = binding.etTipoCambio.text.toString().toDouble()
                val mode       = if (isBuyMode) "COMPRA" else "VENTA"
                val fromInfo = currencies.find { it.code == selectedCurrencyFrom }!!
                val toInfo = currencies.find { it.code == selectedCurrencyTo }!!

                val newOffer = MyOffer(
                    type = mode,
                    asset = fromInfo.code,
                    amount = "${fromInfo.symbol} ${"%.2f".format(monto)}",
                    price = "${toInfo.symbol} ${"%.4f".format(tipoCambio)}",
                    status = "ACTIVA"
                )

                viewModel.addOffer(newOffer)

                // Registrar impacto en la Wallet (Descuentos y Retenciones)
                if (isBuyMode) {
                    // Si compro USD con PEN, el compromiso es en PEN (monto * tipoCambio)
                    walletViewModel.registerOfferImpact("COMPRA", toInfo.code, monto * tipoCambio)
                } else {
                    // Si vendo USD por PEN, el descuento/retención es en USD (monto)
                    walletViewModel.registerOfferImpact("VENTA", fromInfo.code, monto)
                }

                Toast.makeText(
                    context,
                    "Oferta de $mode publicada exitosamente",
                    Toast.LENGTH_LONG
                ).show()

                parentFragmentManager.popBackStack()
            }
        }
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private fun validateFields(): Boolean {
        var isValid = true

        val monto = binding.etMonto.text.toString().toDoubleOrNull()
        when {
            monto == null || monto <= 0 -> {
                binding.tilMonto.error = "Ingresa un monto válido"
                isValid = false
            }
            monto < 10 -> {
                binding.tilMonto.error = "Monto mínimo S/ 10.00"
                isValid = false
            }
            monto > 90000 -> {
                binding.tilMonto.error = "Monto máximo S/ 90,000.00"
                isValid = false
            }
            else -> binding.tilMonto.error = null
        }

        val tipoCambio = binding.etTipoCambio.text.toString().toDoubleOrNull()
        if (tipoCambio == null || tipoCambio <= 0) {
            binding.tilTipoCambio.error = "Ingresa un tipo de cambio válido"
            isValid = false
        } else {
            binding.tilTipoCambio.error = null
        }

        if (selectedPaymentMethods.isEmpty()) {
            Toast.makeText(context, "Selecciona al menos un método de pago", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(ratesUpdater)
        _binding = null
    }
}
