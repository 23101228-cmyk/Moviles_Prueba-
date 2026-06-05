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

        // Botón de Registro
        binding.btnRegistrar.setOnClickListener {
            if (validateFields()) {
                val usuario = binding.etUsuario.text.toString().trim()

                // 1. Cambiamos el texto para confirmar que el proceso fue exitoso
                Toast.makeText(this, "¡Registro exitoso para $usuario!", Toast.LENGTH_SHORT).show()

                // 2. CORRECCIÓN: Agregamos la redirección automática al Login (HU-01)
                val intent = android.content.Intent(this, com.soltis.mya.login.LoginActivity::class.java)
                startActivity(intent)

                // 3. Destruimos esta actividad para evitar que regrese con el botón de "atrás" del teléfono
                finish()
            }
        }

        // Enlace "¿Ya tienes cuenta? Inicia sesión"
        binding.tvIniciarSesion.setOnClickListener {
            // Navegar a LoginActivity si el usuario decide regresar manualmente
            val intent = android.content.Intent(this, com.soltis.mya.login.LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // ── Validaciones según Criterios de Aceptación (HU-01) ──────────────────────

    private fun validateFields(): Boolean {
        // Usamos variables individuales para evitar el cortocircuito lógico del '&&'
        // De esta forma, el formulario mostrará TODOS los errores en pantalla al mismo tiempo.
        val isNombreValid = validateNotEmpty(binding.etNombreCompleto, binding.tilNombreCompleto, "Ingresa tu nombre completo")
        val isUsuarioValid = validateNotEmpty(binding.etUsuario, binding.tilUsuario, "Ingresa un nombre de usuario")

        // Validación del Correo Electrónico (Placeholder exacto y formato)
        val correo = binding.etCorreo.text.toString().trim()
        val isCorreoValid = if (correo.isEmpty()) {
            binding.tilCorreo.error = "Ingresa tu correo electrónico"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Formato de correo inválido (usuario@dominio.com)"
            false
        } else {
            binding.tilCorreo.error = null
            true
        }

        // Validación de Contraseñas
        val pass = binding.etContrasena.text.toString()
        val confirm = binding.etConfirmarContrasena.text.toString()
        var isPasswordValid = true

        // AJUSTE: Exigir longitud mínima de 8 caracteres según HU-01
        if (pass.isEmpty()) {
            binding.tilContrasena.error = "Crea una contraseña segura"
            isPasswordValid = false
        } else if (pass.length < 8) {
            binding.tilContrasena.error = "La contraseña debe tener al menos 8 caracteres"
            isPasswordValid = false
        } else {
            binding.tilContrasena.error = null
        }

        // Validación de Confirmación de Contraseña
        val isConfirmValid = if (confirm.isEmpty()) {
            binding.tilConfirmarContrasena.error = "Confirma tu contraseña"
            false
        } else if (pass != confirm) {
            binding.tilConfirmarContrasena.error = "Las contraseñas no coinciden"
            false
        } else {
            binding.tilConfirmarContrasena.error = null
            true
        }

        // Validación del Checkbox interactivo obligatorio
        val isCheckboxValid = if (!binding.cbTerminos.isChecked) {
            Toast.makeText(this, "Debes aceptar los Términos y Condiciones y la Política de Privacidad", Toast.LENGTH_LONG).show()
            false
        } else {
            true
        }

        // Retorna verdadero únicamente si todas las secciones pasaron de manera exitosa
        return isNombreValid && isCorreoValid && isUsuarioValid && isPasswordValid && isConfirmValid && isCheckboxValid
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
            Toast.makeText(this, "Abriendo Términos y Condiciones...", Toast.LENGTH_SHORT).show()
        }

        // "Política de Privacidad"
        binding.tvLinkPrivacidad.setOnClickListener {
            Toast.makeText(this, "Abriendo Política de Privacidad...", Toast.LENGTH_SHORT).show()
        }
    }
}