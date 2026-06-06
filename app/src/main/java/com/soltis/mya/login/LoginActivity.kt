package com.soltis.mya.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soltis.mya.data.LocalUserStore
import com.soltis.mya.databinding.ActivityLoginBinding
import com.soltis.mya.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userStore: LocalUserStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userStore = LocalUserStore(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.tvOlvideContrasena.setOnClickListener {
            Toast.makeText(this, "Recuperando contrasena, verifique su email", Toast.LENGTH_LONG).show()
        }

        binding.btnIngresar.setOnClickListener {
            if (validateFields()) {
                val correo = binding.etCorreo.text.toString().trim()
                val password = binding.etContrasena.text.toString()
                if (userStore.login(correo, password)) {
                    val currentUser = userStore.getCurrentUser()
                    Toast.makeText(this, "Iniciando sesion...", Toast.LENGTH_SHORT).show()
                    val destination = if (currentUser?.role == LocalUserStore.ROLE_ADMIN) {
                        com.soltis.mya.admin.AdminActivity::class.java
                    } else {
                        com.soltis.mya.MainActivity::class.java
                    }
                    startActivity(Intent(this, destination))
                    finish()
                } else {
                    binding.tilContrasena.error = "Correo o contrasena incorrectos"
                }
            }
        }

        binding.tvRegistrate.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val correo = binding.etCorreo.text.toString().trim()
        if (correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Ingresa un correo valido"
            isValid = false
        } else {
            binding.tilCorreo.error = null
        }

        val password = binding.etContrasena.text.toString()
        if (password.isEmpty()) {
            binding.tilContrasena.error = "Ingresa tu contrasena"
            isValid = false
        } else {
            binding.tilContrasena.error = null
        }

        return isValid
    }
}
