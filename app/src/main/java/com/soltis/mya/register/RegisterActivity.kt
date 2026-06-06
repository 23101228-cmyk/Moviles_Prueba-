package com.soltis.mya.register

import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var userStore: LocalUserStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userStore = LocalUserStore(this)

        setupClickListeners()
        setupTerminosText()
    }

    private fun setupClickListeners() {
        binding.btnRegistrar.setOnClickListener {
            if (validateFields()) {
                val nombre = binding.etNombreCompleto.text.toString().trim()
                val usuario = binding.etUsuario.text.toString().trim()
                val correo = binding.etCorreo.text.toString().trim()
                val password = binding.etContrasena.text.toString()

                when (userStore.registerUser(nombre, usuario, correo, password)) {
                    LocalUserStore.RegisterResult.Success -> {
                        Toast.makeText(this, "Cuenta creada para $usuario. Inicia sesion.", Toast.LENGTH_SHORT).show()
                        startActivity(android.content.Intent(this, com.soltis.mya.login.LoginActivity::class.java))
                        finish()
                    }

                    LocalUserStore.RegisterResult.EmailAlreadyExists -> {
                        binding.tilCorreo.error = "Este correo ya esta registrado"
                    }
                }
            }
        }

        binding.tvIniciarSesion.setOnClickListener {
            startActivity(android.content.Intent(this, com.soltis.mya.login.LoginActivity::class.java))
            finish()
        }
    }

    private fun validateFields(): Boolean {
        val isNombreValid = validateNotEmpty(binding.etNombreCompleto, binding.tilNombreCompleto, "Ingresa tu nombre completo")
        val isUsuarioValid = validateNotEmpty(binding.etUsuario, binding.tilUsuario, "Ingresa un nombre de usuario")

        val correo = binding.etCorreo.text.toString().trim()
        val isCorreoValid = if (correo.isEmpty()) {
            binding.tilCorreo.error = "Ingresa tu correo electronico"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Formato de correo invalido"
            false
        } else {
            binding.tilCorreo.error = null
            true
        }

        val pass = binding.etContrasena.text.toString()
        val confirm = binding.etConfirmarContrasena.text.toString()
        val isPasswordValid = when {
            pass.isEmpty() -> {
                binding.tilContrasena.error = "Crea una contrasena segura"
                false
            }

            pass.length < 8 -> {
                binding.tilContrasena.error = "La contrasena debe tener al menos 8 caracteres"
                false
            }

            else -> {
                binding.tilContrasena.error = null
                true
            }
        }

        val isConfirmValid = when {
            confirm.isEmpty() -> {
                binding.tilConfirmarContrasena.error = "Confirma tu contrasena"
                false
            }

            pass != confirm -> {
                binding.tilConfirmarContrasena.error = "Las contrasenas no coinciden"
                false
            }

            else -> {
                binding.tilConfirmarContrasena.error = null
                true
            }
        }

        val isCheckboxValid = if (!binding.cbTerminos.isChecked) {
            Toast.makeText(this, "Debes aceptar los terminos y la politica de privacidad", Toast.LENGTH_LONG).show()
            false
        } else {
            true
        }

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

    private fun setupTerminosText() {
        binding.tvLinkTerminos.setOnClickListener {
            showPolicyDialog(
                title = "Terminos y condiciones",
                items = listOf(
                    "El usuario debe registrar datos reales y verificables.",
                    "Las ofertas publicadas deben tener montos y tasas claras.",
                    "El usuario debe contar con saldo suficiente antes de vender.",
                    "Los pagos externos deben realizarse solo al titular registrado.",
                    "Todo comprobante adjunto debe corresponder a la operacion.",
                    "Las operaciones iniciadas pueden pasar a revision si hay disputa.",
                    "El administrador puede resolver disputas de forma simulada.",
                    "Las ofertas se cierran al iniciar una operacion.",
                    "El wallet es interno y se usa con fines academicos.",
                    "La plataforma no realiza cobros reales."
                )
            )
        }

        binding.tvLinkPrivacidad.setOnClickListener {
            showPolicyDialog(
                title = "Privacidad",
                items = listOf(
                    "Los datos personales se usan para identificar al usuario dentro de la app.",
                    "El correo se usa para iniciar sesion y simular recuperacion de cuenta.",
                    "Los medios de pago se muestran de forma parcial cuando corresponde.",
                    "El CCI y las cuentas bancarias se enmascaran para proteger al usuario.",
                    "Los comprobantes se registran solo como simulacion academica.",
                    "Las operaciones y disputas quedan disponibles para revision del administrador.",
                    "No se comparte informacion con servicios externos.",
                    "No se procesan pagos reales desde esta aplicacion.",
                    "Los datos se guardan localmente para la demostracion del proyecto.",
                    "El usuario puede editar sus datos desde la pantalla Perfil."
                )
            )
        }
    }

    private fun showPolicyDialog(title: String, items: List<String>) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(12))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 22f
            setTextColor(0xFFEAECEF.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(12))
        }

        val rulesView = TextView(this).apply {
            text = items.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n\n")
            textSize = 15f
            setTextColor(0xFF444444.toInt())
            setLineSpacing(2f, 1.0f)
        }

        val scroll = ScrollView(this).apply {
            addView(rulesView)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(360)
            )
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(container)
            .create()

        val backButton = MaterialButton(this).apply {
            text = "Regresar"
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFFF0B90B.toInt())
            setOnClickListener { dialog.dismiss() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply { topMargin = dp(16) }
        }

        container.addView(titleView)
        container.addView(scroll)
        container.addView(backButton)
        dialog.show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
