package com.soltis.mya.perfil

import android.content.Intent
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
import com.soltis.mya.data.BankCard
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.data.UserProfile
import com.soltis.mya.databinding.FragmentPerfilBinding
import com.soltis.mya.login.LoginActivity

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private lateinit var userStore: LocalUserStore
    private var currentUser: UserProfile? = null
    private var isEditingProfile = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        userStore = LocalUserStore(requireContext())
        loadProfile()
        setupProfileActions()
    }

    private fun setupProfileActions() {
        binding.btnEditProfile.setOnClickListener {
            if (isEditingProfile) saveInlineProfile() else setProfileEditMode(true)
        }
        binding.cardYape.setOnClickListener { setProfileEditMode(true) }
        binding.cardPlin.setOnClickListener { setProfileEditMode(true) }
        binding.btnAddPaymentCard.setOnClickListener { showAddBankCardDialog() }
        binding.cardValidation.setOnClickListener { showMessage("Tus datos de pago estan validados") }
        binding.btnSaveProfile.setOnClickListener {
            if (isEditingProfile) {
                setProfileEditMode(false)
            } else {
                setProfileEditMode(true)
            }
        }
        binding.btnLogout.setOnClickListener { logoutToLogin() }
    }

    private fun loadProfile() {
        val user = userStore.getCurrentUser()
        if (user == null) {
            logoutToLogin()
            return
        }

        currentUser = user
        binding.tvProfileName.text = user.username.ifBlank { "Usuario P2P" }
        binding.tvProfileEmail.text = user.email
        binding.tvFullName.text = user.fullName.ifBlank { "Pendiente" }
        binding.tvPhone.text = user.phone.ifBlank { "Pendiente" }
        binding.tvYapeValue.text = user.yape.ifBlank { "Pendiente" }
        binding.tvPlinValue.text = user.plin.ifBlank { "Pendiente" }
        renderPaymentCards(user.bankCards)
        if (isEditingProfile) fillInlineInputs(user)
    }

    private fun setProfileEditMode(enabled: Boolean) {
        val user = currentUser ?: return
        isEditingProfile = enabled
        if (enabled) fillInlineInputs(user)

        val readVisibility = if (enabled) View.GONE else View.VISIBLE
        val editVisibility = if (enabled) View.VISIBLE else View.GONE

        binding.tvFullName.visibility = readVisibility
        binding.tvPhone.visibility = readVisibility
        binding.tvYapeValue.visibility = readVisibility
        binding.tvPlinValue.visibility = readVisibility

        binding.etFullNameInline.visibility = editVisibility
        binding.etPhoneInline.visibility = editVisibility
        binding.etYapeInline.visibility = editVisibility
        binding.etPlinInline.visibility = editVisibility

        binding.btnEditProfile.text = if (enabled) "Guardar" else "Editar"
        binding.btnSaveProfile.text = if (enabled) "Cancelar edicion" else "Editar datos"
    }

    private fun fillInlineInputs(user: UserProfile) {
        binding.etFullNameInline.setText(user.fullName)
        binding.etPhoneInline.setText(user.phone)
        binding.etYapeInline.setText(user.yape)
        binding.etPlinInline.setText(user.plin)
    }

    private fun saveInlineProfile() {
        val user = currentUser ?: return
        val name = binding.etFullNameInline.text.toString().trim()
        if (name.isBlank()) {
            binding.etFullNameInline.error = "Completa el nombre legal"
            return
        }

        val updated = user.copy(
            fullName = name,
            phone = binding.etPhoneInline.text.toString().trim(),
            yape = binding.etYapeInline.text.toString().trim(),
            plin = binding.etPlinInline.text.toString().trim()
        )
        userStore.updateCurrentUser(updated)
        loadProfile()
        setProfileEditMode(false)
        showMessage("Perfil actualizado")
    }

    private fun showEditProfileDialog() {
        val user = currentUser ?: return
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val nameInput = createInput("Nombre legal", user.fullName, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val phoneInput = createInput("Telefono", user.phone, InputType.TYPE_CLASS_PHONE)
        val yapeInput = createInput("Yape", user.yape, InputType.TYPE_CLASS_PHONE)
        val plinInput = createInput("Plin", user.plin, InputType.TYPE_CLASS_PHONE)

        container.addView(nameInput)
        container.addView(createReadOnlyText("Correo: ${user.email}"))
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
                        if (nameInput.text.isNullOrBlank()) {
                            showMessage("Completa el nombre legal")
                            return@setOnClickListener
                        }

                        val updated = user.copy(
                            fullName = nameInput.text.toString().trim(),
                            phone = phoneInput.text.toString().trim(),
                            yape = yapeInput.text.toString().trim(),
                            plin = plinInput.text.toString().trim()
                        )
                        userStore.updateCurrentUser(updated)
                        loadProfile()
                        showMessage("Cambios guardados")
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun showAddBankCardDialog() {
        val user = currentUser ?: return
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val bankSpinner = createSpinner(BANK_OPTIONS)
        val typeSpinner = createSpinner(ACCOUNT_TYPE_OPTIONS)
        val accountInput = createInput("Numero de cuenta", "", InputType.TYPE_CLASS_NUMBER)
        val cciInput = createInput("CCI de 20 digitos", "", InputType.TYPE_CLASS_NUMBER)

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
                            showMessage("La cuenta de $bank debe tener ${formatLengths(accountLengths)} digitos")
                            return@setOnClickListener
                        }

                        if (accountLengths.isEmpty() && account.length !in 8..20) {
                            showMessage("La cuenta debe tener entre 8 y 20 digitos")
                            return@setOnClickListener
                        }

                        if (cci.length != CCI_LENGTH) {
                            showMessage("El CCI debe tener 20 digitos")
                            return@setOnClickListener
                        }

                        val updated = user.copy(
                            bankCards = user.bankCards + BankCard(
                                bank = bank,
                                type = typeSpinner.selectedItem.toString(),
                                account = account,
                                cci = cci
                            )
                        )
                        userStore.updateCurrentUser(updated)
                        loadProfile()
                        showMessage("Nueva cuenta agregada")
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun renderPaymentCards(cards: List<BankCard>) {
        binding.paymentCardsContainer.removeAllViews()

        if (cards.isEmpty()) {
            binding.paymentCardsContainer.addView(createPendingCard())
            binding.tvValidationTitle.text = "Datos de pago pendientes"
            binding.tvValidationDescription.text = "Agrega Yape, Plin o una cuenta bancaria para operar."
            return
        }

        cards.forEachIndexed { index, card ->
            binding.paymentCardsContainer.addView(createBankCardView(index, card))
        }

        val user = currentUser
        val yapeCount = if (user?.yape.isNullOrBlank()) 0 else 1
        val plinCount = if (user?.plin.isNullOrBlank()) 0 else 1
        val totalMethods = cards.size + yapeCount + plinCount
        binding.tvValidationTitle.text = "Datos de pago validados"
        binding.tvValidationDescription.text = "$totalMethods medios de pago listos para operar."
    }

    private fun createPendingCard(): View {
        return TextView(requireContext()).apply {
            text = "Transferencia bancaria: Pendiente"
            setTextColor(0xFF757575.toInt())
            textSize = 14f
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
    }

    private fun createBankCardView(index: Int, cardData: BankCard): View {
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
            text = cardData.bank
            setTextColor(0xFF263238.toInt())
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
        }

        val subtitle = TextView(requireContext()).apply {
            text = "${cardData.type}\nCuenta: ${userStore.maskAccount(cardData.account)}\nCCI: ${userStore.maskCci(cardData.cci)}"
            setTextColor(0xFF6D6D6D.toInt())
            textSize = 13f
        }

        val editIcon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_edit)
            contentDescription = "Editar cuenta bancaria"
            layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply {
                marginStart = dp(12)
            }
            setPadding(dp(4), dp(4), dp(4), dp(4))
            isClickable = true
            isFocusable = true
            setOnClickListener { showEditBankCardDialog(index, cardData) }
        }

        texts.addView(title)
        texts.addView(subtitle)
        row.addView(icon)
        row.addView(texts)
        row.addView(editIcon)
        card.addView(row)
        card.setOnClickListener { showEditBankCardDialog(index, cardData) }
        return card
    }

    private fun showEditBankCardDialog(index: Int, card: BankCard) {
        val user = currentUser ?: return
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), 0)
        }

        val bankSpinner = createSpinner(BANK_OPTIONS).apply {
            setSelection(BANK_OPTIONS.indexOf(card.bank).coerceAtLeast(0))
        }
        val typeSpinner = createSpinner(ACCOUNT_TYPE_OPTIONS).apply {
            setSelection(ACCOUNT_TYPE_OPTIONS.indexOf(card.type).coerceAtLeast(0))
        }
        val accountInput = createInput("Numero de cuenta", card.account, InputType.TYPE_CLASS_NUMBER)
        val cciInput = createInput("CCI de 20 digitos", card.cci, InputType.TYPE_CLASS_NUMBER)

        container.addView(createLabel("Banco"))
        container.addView(bankSpinner)
        container.addView(createLabel("Tipo"))
        container.addView(typeSpinner)
        container.addView(accountInput)
        container.addView(cciInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar cuenta bancaria")
            .setView(container)
            .setNegativeButton("Eliminar") { _, _ ->
                val updatedCards = user.bankCards.toMutableList().apply {
                    if (index in indices) removeAt(index)
                }
                userStore.updateCurrentUser(user.copy(bankCards = updatedCards))
                loadProfile()
                showMessage("Cuenta eliminada")
            }
            .setNeutralButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val bank = bankSpinner.selectedItem.toString()
                        val account = accountInput.text.toString().onlyDigits()
                        val cci = cciInput.text.toString().onlyDigits()
                        val accountLengths = validAccountLengths(bank)

                        if (!isBankCardValid(bank, account, cci, accountLengths)) {
                            return@setOnClickListener
                        }

                        val updatedCards = user.bankCards.toMutableList().apply {
                            if (index in indices) {
                                this[index] = BankCard(
                                    bank = bank,
                                    type = typeSpinner.selectedItem.toString(),
                                    account = account,
                                    cci = cci
                                )
                            }
                        }
                        userStore.updateCurrentUser(user.copy(bankCards = updatedCards))
                        loadProfile()
                        showMessage("Cuenta actualizada")
                        dismiss()
                    }
                }
                show()
            }
    }

    private fun logoutToLogin() {
        userStore.logout()
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

    private fun createReadOnlyText(value: String): TextView {
        return TextView(requireContext()).apply {
            text = value
            setTextColor(0xFF757575.toInt())
            textSize = 13f
            setPadding(0, 0, 0, dp(10))
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

    private fun String.onlyDigits(): String = filter { it.isDigit() }

    private fun validAccountLengths(bank: String): IntRange {
        return when (bank) {
            "BCP" -> 13..14
            "BBVA" -> 18..18
            "Interbank" -> 13..13
            "Scotiabank" -> 10..10
            "Banco de la Nacion" -> 11..11
            "BanBif" -> 12..12
            "Pichincha" -> 10..10
            else -> 0..-1
        }
    }

    private fun isBankCardValid(bank: String, account: String, cci: String, accountLengths: IntRange): Boolean {
        if (!accountLengths.isEmpty() && account.length !in accountLengths) {
            showMessage("La cuenta de $bank debe tener ${formatLengths(accountLengths)} digitos")
            return false
        }

        if (accountLengths.isEmpty() && account.length !in 8..20) {
            showMessage("La cuenta debe tener entre 8 y 20 digitos")
            return false
        }

        if (cci.length != CCI_LENGTH) {
            showMessage("El CCI debe tener 20 digitos")
            return false
        }

        return true
    }

    private fun formatLengths(lengths: IntRange): String {
        return if (lengths.first == lengths.last) {
            lengths.first.toString()
        } else {
            "${lengths.first} o ${lengths.last}"
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
        private const val CCI_LENGTH = 20
        private val BANK_OPTIONS = listOf("BCP", "BBVA", "Interbank", "Scotiabank", "Banco de la Nacion", "BanBif", "Pichincha")
        private val ACCOUNT_TYPE_OPTIONS = listOf("Cuenta de ahorros", "Cuenta corriente")
    }
}
