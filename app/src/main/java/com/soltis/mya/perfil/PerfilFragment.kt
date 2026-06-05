package com.soltis.mya.perfil

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentPerfilBinding
import com.soltis.mya.login.LoginActivity
import org.json.JSONArray
import org.json.JSONObject

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadProfile()
        setupProfileActions()
    }

    private fun setupProfileActions() {
        binding.btnEditProfile.setOnClickListener { showEditProfileDialog() }
        binding.cardYape.setOnClickListener { showEditProfileDialog() }
        binding.cardPlin.setOnClickListener { showEditProfileDialog() }
        binding.btnAddPaymentCard.setOnClickListener { showAddBankCardDialog() }
        binding.cardValidation.setOnClickListener { showMessage("Tus datos de pago están validados") }
        binding.btnSaveProfile.setOnClickListener {
            saveCurrentProfile()
            showMessage("Perfil guardado correctamente")
        }
        binding.btnLogout.setOnClickListener { logoutToLogin() }
    }

    private fun loadProfile() {
        val name = prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        val email = prefs.getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL
        val phone = prefs.getString(KEY_PHONE, DEFAULT_PHONE) ?: DEFAULT_PHONE
        val yape = prefs.getString(KEY_YAPE, phone) ?: phone
        val plin = prefs.getString(KEY_PLIN, phone) ?: phone

        binding.tvProfileName.text = name
        binding.tvProfileEmail.text = email
        binding.tvFullName.text = name
        binding.tvPhone.text = phone
        binding.tvYapeValue.text = yape
        binding.tvPlinValue.text = plin
        renderPaymentCards()
    }

    private fun saveCurrentProfile() {
        prefs.edit()
            .putString(KEY_NAME, binding.tvFullName.text.toString())
            .putString(KEY_EMAIL, binding.tvProfileEmail.text.toString())
            .putString(KEY_PHONE, binding.tvPhone.text.toString())
            .putString(KEY_YAPE, binding.tvYapeValue.text.toString())
            .putString(KEY_PLIN, binding.tvPlinValue.text.toString())
            .apply()
    }

    private fun showEditProfileDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val nameInput = createInput("Nombre completo", binding.tvFullName.text.toString(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val emailInput = createInput("Correo", binding.tvProfileEmail.text.toString(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val phoneInput = createInput("Celular", binding.tvPhone.text.toString(), InputType.TYPE_CLASS_PHONE)
        val yapeInput = createInput("Yape", binding.tvYapeValue.text.toString(), InputType.TYPE_CLASS_PHONE)
        val plinInput = createInput("Plin", binding.tvPlinValue.text.toString(), InputType.TYPE_CLASS_PHONE)

        container.addView(nameInput)
        container.addView(emailInput)
        container.addView(phoneInput)
        container.addView(yapeInput)
        container.addView(plinInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar perfil")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (nameInput.text.isNullOrBlank() || emailInput.text.isNullOrBlank() || phoneInput.text.isNullOrBlank()) {
                            showMessage("Completa nombre, correo y celular")
                            return@setOnClickListener
                        }

                        prefs.edit()
                            .putString(KEY_NAME, nameInput.text.toString().trim())
                            .putString(KEY_EMAIL, emailInput.text.toString().trim())
                            .putString(KEY_PHONE, phoneInput.text.toString().trim())
                            .putString(KEY_YAPE, yapeInput.text.toString().trim())
                            .putString(KEY_PLIN, plinInput.text.toString().trim())
                            .apply()

                        loadProfile()
                        showMessage("Cambios guardados")
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun showAddBankCardDialog() {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val bankSpinner = createSpinner(listOf("BCP", "BBVA", "Interbank", "Scotiabank", "Banco de la Nación", "BanBif", "Pichincha"))
        val typeSpinner = createSpinner(listOf("Cuenta de ahorros", "Cuenta corriente"))
        val accountInput = createInput("Número de cuenta", "", InputType.TYPE_CLASS_NUMBER)
        val cciInput = createInput("CCI de 20 dígitos", "", InputType.TYPE_CLASS_NUMBER)

        container.addView(createLabel("Banco"))
        container.addView(bankSpinner)
        container.addView(createLabel("Tipo"))
        container.addView(typeSpinner)
        container.addView(accountInput)
        container.addView(cciInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar banco o CCI")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val bank = bankSpinner.selectedItem.toString()
                        val account = accountInput.text.toString().onlyDigits()
                        val cci = cciInput.text.toString().onlyDigits()
                        val accountLengths = validAccountLengths(bank)

                        if (!accountLengths.isEmpty() && account.length !in accountLengths) {
                            showMessage("La cuenta de $bank debe tener ${formatLengths(accountLengths)} dígitos")
                            return@setOnClickListener
                        }

                        if (accountLengths.isEmpty() && account.length !in 8..20) {
                            showMessage("La cuenta debe tener entre 8 y 20 dígitos")
                            return@setOnClickListener
                        }

                        if (cci.length != CCI_LENGTH) {
                            showMessage("El CCI debe tener 20 dígitos")
                            return@setOnClickListener
                        }

                        val cards = getStoredCards()
                        cards.put(
                            JSONObject()
                                .put("bank", bank)
                                .put("type", typeSpinner.selectedItem.toString())
                                .put("account", account)
                                .put("cci", cci)
                        )
                        prefs.edit().putString(KEY_BANK_CARDS, cards.toString()).apply()
                        renderPaymentCards()
                        showMessage("Nueva tarjeta agregada")
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun renderPaymentCards() {
        binding.paymentCardsContainer.removeAllViews()
        val cards = getStoredCards()

        if (cards.length() == 0) {
            cards.put(
                JSONObject()
                    .put("bank", "BCP")
                    .put("type", "Cuenta de ahorros")
                    .put("account", "19112345678901")
                    .put("cci", "00219100123456789012")
            )
        }

        for (index in 0 until cards.length()) {
            val card = cards.getJSONObject(index)
            binding.paymentCardsContainer.addView(
                createBankCardView(
                    bank = card.optString("bank"),
                    type = card.optString("type"),
                    account = card.optString("account"),
                    cci = card.optString("cci")
                )
            )
        }

        val totalMethods = cards.length() + 2
        binding.tvValidationDescription.text = "$totalMethods medios de pago validados y listos para operar."
    }

    private fun createBankCardView(bank: String, type: String, account: String, cci: String): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(0xFFFFF8E1.toInt())
            strokeColor = 0xFFFFE0B2.toInt()
            strokeWidth = dp(1)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12) }
        }

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val icon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_bank_transfer)
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }

        val texts = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
        }

        val title = TextView(requireContext()).apply {
            text = bank
            setTextColor(0xFF263238.toInt())
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "$type\nCuenta: ${maskNumber(account)}\nCCI: ${maskNumber(cci)}"
            setTextColor(0xFF6D6D6D.toInt())
            textSize = 13f
        }

        texts.addView(title)
        texts.addView(subtitle)
        row.addView(icon)
        row.addView(texts)
        card.addView(row)
        card.setOnClickListener { showBankCardDetails(bank, type, account, cci) }
        return card
    }

    private fun getStoredCards(): JSONArray {
        val saved = prefs.getString(KEY_BANK_CARDS, null)
        return if (saved.isNullOrBlank()) JSONArray() else JSONArray(saved)
    }

    private fun showBankCardDetails(bank: String, type: String, account: String, cci: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(bank)
            .setMessage("$type\n\nNúmero de cuenta:\n$account\n\nCCI:\n$cci")
            .setPositiveButton("Cerrar", null)
            .show()
    }

    private fun maskNumber(value: String): String {
        val digits = value.onlyDigits()
        if (digits.length <= 8) return digits
        return "${digits.take(4)}••••••${digits.takeLast(4)}"
    }

    private fun String.onlyDigits(): String = filter { it.isDigit() }

    private fun validAccountLengths(bank: String): IntRange {
        return when (bank) {
            "BCP" -> 13..14
            "BBVA" -> 18..18
            "Interbank" -> 13..13
            "Scotiabank" -> 10..10
            "Banco de la Nación" -> 11..11
            "BanBif" -> 12..12
            "Pichincha" -> 10..10
            else -> 0..-1
        }
    }

    private fun formatLengths(lengths: IntRange): String {
        return if (lengths.first == lengths.last) {
            lengths.first.toString()
        } else {
            "${lengths.first} o ${lengths.last}"
        }
    }

    private fun logoutToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun createInput(hint: String, value: String, inputType: Int): EditText {
        return EditText(requireContext()).apply {
            this.hint = hint
            setText(value)
            this.inputType = inputType
            setSingleLine(true)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(10) }
        }
    }

    private fun createSpinner(options: List<String>): Spinner {
        return Spinner(requireContext()).apply {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
    }

    private fun createLabel(text: String): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            setTextColor(0xFF757575.toInt())
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREFS_NAME = "perfil_usuario"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_YAPE = "yape"
        private const val KEY_PLIN = "plin"
        private const val KEY_BANK_CARDS = "bank_cards"
        private const val CCI_LENGTH = 20
        private const val DEFAULT_NAME = "Usuario de prueba"
        private const val DEFAULT_EMAIL = "usuario@ejemplo.com"
        private const val DEFAULT_PHONE = "+51 999 888 777"
    }
}
