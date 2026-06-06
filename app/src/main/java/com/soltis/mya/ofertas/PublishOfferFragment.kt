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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentPublishOfferBinding
import com.soltis.mya.wallet.WalletViewModel
import java.util.Locale

class PublishOfferFragment : Fragment() {

    private var _binding: FragmentPublishOfferBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OfferViewModel by activityViewModels()
    private val walletViewModel: WalletViewModel by activityViewModels()

    private var isBuyMode = true
    private var selectedCurrencyFrom = "USD"
    private var selectedCurrencyTo = "PEN"
    private val selectedPaymentMethods = mutableSetOf<String>()

    private val currencies = listOf(
        CurrencyInfo("PEN", "Soles", R.drawable.ic_flag_pen, "S/"),
        CurrencyInfo("USD", "Dolares", R.drawable.ic_flag_usd, "$"),
        CurrencyInfo("EUR", "Euros", R.drawable.ic_flag_eur, "EUR")
    )

    private val exchangeRates = mapOf(
        "USD_PEN" to 3.7540,
        "PEN_USD" to 1 / 3.7540,
        "EUR_PEN" to 4.0520,
        "PEN_EUR" to 1 / 4.0520,
        "EUR_USD" to 1.0850,
        "USD_EUR" to 1 / 1.0850
    )

    data class CurrencyInfo(val code: String, val name: String, val flagRes: Int, val symbol: String)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
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
        setupFieldWatchers()
        setupClickListeners()
        applyMode()

        binding.etTipoCambio.isEnabled = true
        binding.etTipoCambio.isFocusableInTouchMode = true
        binding.etTipoCambio.alpha = 1f
    }

    private fun setupCurrencySelectors() {
        val openFromSelector = View.OnClickListener { showCurrencySelector(true) }
        val openToSelector = View.OnClickListener { showCurrencySelector(false) }
        binding.layoutCurrencyFrom.setOnClickListener(openFromSelector)
        binding.imgCurrencyFromDropdown.setOnClickListener(openFromSelector)
        binding.layoutCurrencyTo.setOnClickListener(openToSelector)
        binding.imgCurrencyToDropdown.setOnClickListener(openToSelector)
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
            .setTitle(if (isFrom) "Moneda que entregas" else "Moneda que recibes")
            .setItems(codes) { _, which ->
                val selected = currencies[which].code
                if (isFrom) {
                    if (selected == selectedCurrencyTo) {
                        Toast.makeText(context, "Las monedas deben ser diferentes", Toast.LENGTH_SHORT).show()
                        return@setItems
                    }
                    selectedCurrencyFrom = selected
                } else {
                    if (selected == selectedCurrencyFrom) {
                        Toast.makeText(context, "Las monedas deben ser diferentes", Toast.LENGTH_SHORT).show()
                        return@setItems
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

        binding.etTipoCambio.setText(formatNumber(rate, 4))
        binding.tilTipoCambio.suffixText = "${selectedCurrencyTo}/${selectedCurrencyFrom}"
        binding.tilTipoCambio.helperText = "Puedes editar el tipo de cambio sugerido"

        calculateTotal()
    }

    private fun calculateTotal() {
        val amount = parseAmount(binding.etMonto.text?.toString()) ?: 0.0
        val rate = parseAmount(binding.etTipoCambio.text?.toString()) ?: 0.0
        val total = amount * rate
        val toInfo = currencies.find { it.code == selectedCurrencyTo }!!

        if (amount > 0 && rate > 0) {
            binding.tvTotalCalculado.visibility = View.VISIBLE
            if (isBuyMode) {
                binding.tvTotalCalculado.text =
                    "Total a pagar: ${toInfo.symbol} ${formatNumber(total, 2)} ${toInfo.code}"
                binding.tvTotalCalculado.setTextColor(Color.parseColor("#C62828"))
            } else {
                binding.tvTotalCalculado.text =
                    "Total a recibir: ${toInfo.symbol} ${formatNumber(total, 2)} ${toInfo.code}"
                binding.tvTotalCalculado.setTextColor(Color.parseColor("#2E7D32"))
            }
        } else {
            binding.tvTotalCalculado.visibility = View.GONE
        }
    }

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
            binding.tabCompra.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabCompra.setTextColor(Color.WHITE)
            binding.tabCompra.typeface = android.graphics.Typeface.DEFAULT_BOLD
            binding.tabVenta.setBackgroundResource(android.R.color.transparent)
            binding.tabVenta.setTextColor(Color.parseColor("#888888"))
            binding.tabVenta.typeface = android.graphics.Typeface.DEFAULT
            binding.btnPublicar.text = "Publicar oferta de compra"
        } else {
            binding.tabVenta.setBackgroundResource(R.drawable.bg_tab_selected)
            binding.tabVenta.setTextColor(Color.WHITE)
            binding.tabVenta.typeface = android.graphics.Typeface.DEFAULT_BOLD
            binding.tabCompra.setBackgroundResource(android.R.color.transparent)
            binding.tabCompra.setTextColor(Color.parseColor("#888888"))
            binding.tabCompra.typeface = android.graphics.Typeface.DEFAULT
            binding.btnPublicar.text = "Publicar oferta de venta"
        }

        binding.imgFlagFrom.setImageResource(fromInfo.flagRes)
        binding.tvCurrencyFromCode.text = fromInfo.code
        binding.tvCurrencyFromName.text = fromInfo.name

        binding.imgFlagTo.setImageResource(toInfo.flagRes)
        binding.tvCurrencyToCode.text = toInfo.code
        binding.tvCurrencyToName.text = toInfo.name

        binding.tilMonto.prefixText = fromInfo.symbol
        binding.tilLimiteMin.prefixText = fromInfo.symbol
        binding.tilLimiteMax.prefixText = fromInfo.symbol
        binding.tilTipoCambio.prefixText = toInfo.symbol

        val balanceToUse = if (isBuyMode) toInfo.code else fromInfo.code
        val bal = walletViewModel.balances.value?.get(balanceToUse) ?: 0.0
        binding.tvInfoBanner.text = if (isBuyMode) {
            "Saldo necesario en ${toInfo.code}: ${toInfo.symbol} ${formatNumber(bal, 2)}\nEste es el saldo que usaras para pagar la compra."
        } else {
            "Saldo disponible en ${fromInfo.code}: ${fromInfo.symbol} ${formatNumber(bal, 2)}\nEste es el saldo que estas ofreciendo vender."
        }

        updateExchangeRate()
        updateAmountSuffix()
    }

    private fun setupFieldWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                updateAmountSuffix()
                calculateTotal()
                clearInputErrors()
            }
        }

        binding.etMonto.addTextChangedListener(watcher)
        binding.etTipoCambio.addTextChangedListener(watcher)
        binding.etLimiteMin.addTextChangedListener(watcher)
        binding.etLimiteMax.addTextChangedListener(watcher)
    }

    private fun updateAmountSuffix() {
        val raw = parseAmount(binding.etMonto.text?.toString()) ?: 0.0
        binding.tilMonto.suffixText = "${formatNumber(raw, 2)} $selectedCurrencyFrom"
    }

    private fun setupPaymentChips() {
        val chips = listOf(
            "Yape" to binding.chipYape,
            "Plin" to binding.chipPlin,
            "Transferencia" to binding.chipTransferencia,
            "Wallet" to binding.chipWallet
        )

        chips.forEach { (method, chip) ->
            chip.setOnClickListener {
                if (selectedPaymentMethods.contains(method)) {
                    selectedPaymentMethods.remove(method)
                    chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                } else {
                    selectedPaymentMethods.add(method)
                    chip.setBackgroundResource(R.drawable.bg_chip_selected)
                }
            }
        }

        selectedPaymentMethods.add("Yape")
        binding.chipYape.setBackgroundResource(R.drawable.bg_chip_selected)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnPublicar.setOnClickListener {
            if (validateFields()) {
                publishCurrentOffer()
            } else {
                Toast.makeText(context, "Revisa los campos marcados antes de publicar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun publishCurrentOffer() {
        val monto = parseAmount(binding.etMonto.text?.toString()) ?: return
        val tipoCambio = parseAmount(binding.etTipoCambio.text?.toString()) ?: return
        val mode = if (isBuyMode) "COMPRA" else "VENTA"
        val fromInfo = currencies.find { it.code == selectedCurrencyFrom }!!
        val toInfo = currencies.find { it.code == selectedCurrencyTo }!!

        publishOffer(monto, tipoCambio, mode, fromInfo, toInfo)
    }

    private fun publishOffer(
        monto: Double,
        tipoCambio: Double,
        mode: String,
        fromInfo: CurrencyInfo,
        toInfo: CurrencyInfo
    ) {
        val newOffer = MyOffer(
            type = mode,
            asset = fromInfo.code,
            amount = "${fromInfo.symbol} ${formatNumber(monto, 2)}",
            price = "${toInfo.symbol} ${formatNumber(tipoCambio, 4)}",
            status = "ACTIVA"
        )

        viewModel.addOffer(newOffer)

        if (isBuyMode) {
            walletViewModel.registerOfferImpact("COMPRA", toInfo.code, monto * tipoCambio)
        } else {
            walletViewModel.registerOfferImpact("VENTA", fromInfo.code, monto)
        }

        Toast.makeText(
            context,
            "Oferta de $mode publicada exitosamente",
            Toast.LENGTH_LONG
        ).show()

        returnToMyOffers()
    }

    private fun returnToMyOffers() {
        val navController = findNavController()
        val returned = navController.popBackStack(R.id.nav_ofertas, false)
        if (!returned) {
            navController.navigate(R.id.nav_ofertas)
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val monto = parseAmount(binding.etMonto.text?.toString())
        when {
            monto == null || monto <= 0 -> {
                binding.tilMonto.error = "Ingresa un monto valido"
                isValid = false
            }
            monto < 10 -> {
                binding.tilMonto.error = "Monto minimo S/ 10.00"
                isValid = false
            }
            monto > 90000 -> {
                binding.tilMonto.error = "Monto maximo S/ 90,000.00"
                isValid = false
            }
            else -> binding.tilMonto.error = null
        }

        val tipoCambio = parseAmount(binding.etTipoCambio.text?.toString())
        if (tipoCambio == null || tipoCambio <= 0) {
            binding.tilTipoCambio.error = "Ingresa un tipo de cambio valido"
            isValid = false
        } else {
            binding.tilTipoCambio.error = null
        }

        val limiteMin = parseAmount(binding.etLimiteMin.text?.toString())
        val limiteMax = parseAmount(binding.etLimiteMax.text?.toString())
        when {
            limiteMin != null && limiteMin <= 0 -> {
                binding.tilLimiteMin.error = "Minimo invalido"
                isValid = false
            }
            monto != null && limiteMin != null && limiteMin > monto -> {
                binding.tilLimiteMin.error = "No debe superar el monto"
                isValid = false
            }
            else -> binding.tilLimiteMin.error = null
        }

        when {
            limiteMax != null && limiteMax <= 0 -> {
                binding.tilLimiteMax.error = "Maximo invalido"
                isValid = false
            }
            monto != null && limiteMax != null && limiteMax > monto -> {
                binding.tilLimiteMax.error = "No debe superar el monto"
                isValid = false
            }
            limiteMin != null && limiteMax != null && limiteMax < limiteMin -> {
                binding.tilLimiteMax.error = "Debe ser mayor al minimo"
                isValid = false
            }
            else -> binding.tilLimiteMax.error = null
        }

        if (selectedPaymentMethods.isEmpty()) {
            Toast.makeText(context, "Selecciona al menos un metodo de pago", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (isValid) {
            val amount = monto ?: 0.0
            val rate = tipoCambio ?: 0.0
            val walletRequiredCurrency = if (isBuyMode) selectedCurrencyTo else selectedCurrencyFrom
            val walletRequiredAmount = if (isBuyMode) amount * rate else amount
            val available = walletViewModel.balances.value?.get(walletRequiredCurrency) ?: 0.0
            val requiresWalletValidation = selectedPaymentMethods.contains("Wallet") || !isBuyMode

            if (requiresWalletValidation && walletRequiredAmount > available) {
                Toast.makeText(
                    context,
                    "Saldo insuficiente en $walletRequiredCurrency para publicar con Wallet interno",
                    Toast.LENGTH_LONG
                ).show()
                isValid = false
            }
        }

        return isValid
    }

    private fun clearInputErrors() {
        binding.tilMonto.error = null
        binding.tilTipoCambio.error = null
        binding.tilLimiteMin.error = null
        binding.tilLimiteMax.error = null
    }

    private fun parseAmount(value: String?): Double? {
        return value
            ?.trim()
            ?.replace(",", ".")
            ?.takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
    }

    private fun formatNumber(value: Double, decimals: Int): String {
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
