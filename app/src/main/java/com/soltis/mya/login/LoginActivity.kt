package com.soltis.mya.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soltis.mya.databinding.ActivityLoginBinding
import com.soltis.mya.register.RegisterActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Forgot password
        binding.tvOlvideContrasena.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
        }

        // Login button
        binding.btnIngresar.setOnClickListener {
            if (validateFields()) {
                Toast.makeText(this, "Iniciando sesión...", Toast.LENGTH_SHORT).show()
                // Navegar a MainActivity tras login exitoso
                val intent = Intent(this, com.soltis.mya.MainActivity::class.java)
                startActivity(intent)
                finish() // Cerramos login para que no pueda volver atrás con el botón del sistema
            }
        }

        // Register link
        binding.tvRegistrate.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val correo = binding.etCorreo.text.toString().trim()
        if (correo.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            binding.tilCorreo.error = "Ingresa un correo válido"
            isValid = false
        } else {
            binding.tilCorreo.error = null
        }

        val password = binding.etContrasena.text.toString()
        if (password.isEmpty()) {
            binding.tilContrasena.error = "Ingresa tu contraseña"
            isValid = false
        } else {
            binding.tilContrasena.error = null
        }

        return isValid
    }
}
