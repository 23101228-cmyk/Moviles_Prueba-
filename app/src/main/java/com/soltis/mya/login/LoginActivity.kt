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
            Toast.makeText(this, "Recuperar contrasena", Toast.LENGTH_SHORT).show()
        }

        binding.btnIngresar.setOnClickListener {
            if (validateFields()) {
                val correo = binding.etCorreo.text.toString().trim()
                val password = binding.etContrasena.text.toString()
                if (userStore.login(correo, password)) {
                    Toast.makeText(this, "Iniciando sesion...", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, com.soltis.mya.MainActivity::class.java))
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
