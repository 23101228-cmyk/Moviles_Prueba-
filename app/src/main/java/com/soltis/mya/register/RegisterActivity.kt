package com.soltis.mya.register

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.soltis.mya.R
import com.soltis.mya.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupTerminosText()
    }

    private fun setupClickListeners() {

        // Register button
        binding.btnRegistrar.setOnClickListener {
            if (validateFields()) {
                // TODO: conectar con tu lógica de backend / Firebase / API
                val usuario  = binding.etUsuario.text.toString().trim()

                Toast.makeText(this, "Registrando a $usuario...", Toast.LENGTH_SHORT).show()
                // Navega a la siguiente pantalla o llama a tu ViewModel aquí
            }
        }

        // Link "Inicia sesión"
        binding.tvIniciarSesion.setOnClickListener {
            // Navegar a LoginActivity
            val intent = android.content.Intent(this, com.soltis.mya.login.LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // ── Validaciones básicas ──────────────────────────────────────────────────

    private fun validateFields(): Boolean {
        var isValid = true

        isValid = validateNotEmpty(binding.etNombreCompleto, binding.tilNombreCompleto,
            "Ingresa tu nombre completo") && isValid

        val correo = binding.etCorreo.text.toString().trim()
        if (correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Ingresa un correo válido"
            isValid = false
        } else {
            binding.tilCorreo.error = null
        }

        isValid = validateNotEmpty(binding.etUsuario, binding.tilUsuario,
            "Ingresa un nombre de usuario") && isValid

        val pass    = binding.etContrasena.text.toString()
        val confirm = binding.etConfirmarContrasena.text.toString()

        if (pass.length < 6) {
            binding.tilContrasena.error = "Mínimo 6 caracteres"
            isValid = false
        } else {
            binding.tilContrasena.error = null
        }

        if (pass != confirm) {
            binding.tilConfirmarContrasena.error = "Las contraseñas no coinciden"
            isValid = false
        } else {
            binding.tilConfirmarContrasena.error = null
        }

        if (!binding.cbTerminos.isChecked) {
            Toast.makeText(this, "Debes aceptar los Términos y Condiciones", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun validateNotEmpty(
        editText: TextInputEditText,
        layout: TextInputLayout,
        errorMsg: String
    ): Boolean {
        return if (editText.text.toString().trim().isEmpty()) {
            layout.error = errorMsg
            false
        } else {
            layout.error = null
            true
        }
    }

    // ── Términos con spans clicables ──────────────────────────────────────────

    private fun setupTerminosText() {
        val accent = ContextCompat.getColor(this, R.color.accent_yellow) // #F5A623

        // "Términos y Condiciones"
        binding.tvLinkTerminos.setOnClickListener {
            // TODO: abrir WebView o Activity de términos
            Toast.makeText(this, "Términos y Condiciones", Toast.LENGTH_SHORT).show()
        }

        // "Política de Privacidad"
        binding.tvLinkPrivacidad.setOnClickListener {
            // TODO: abrir WebView o Activity de privacidad
            Toast.makeText(this, "Política de Privacidad", Toast.LENGTH_SHORT).show()
        }
    }
}
