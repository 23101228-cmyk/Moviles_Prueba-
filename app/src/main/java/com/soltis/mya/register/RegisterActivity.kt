package com.soltis.mya.register

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
            Toast.makeText(this, "Abriendo terminos y condiciones...", Toast.LENGTH_SHORT).show()
        }

        binding.tvLinkPrivacidad.setOnClickListener {
            Toast.makeText(this, "Abriendo politica de privacidad...", Toast.LENGTH_SHORT).show()
        }
    }
}
