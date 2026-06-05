package com.soltis.mya.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.soltis.mya.databinding.FragmentPerfilBinding

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProfileActions()
    }

    private fun setupProfileActions() {
        binding.rowYape.setOnClickListener { showMessage("Yape verificado y listo para recibir pagos") }
        binding.rowPlin.setOnClickListener { showMessage("Plin verificado y listo para recibir pagos") }
        binding.rowBank.setOnClickListener { showMessage("Banco registrado correctamente") }
        binding.rowCci.setOnClickListener { showMessage("CCI validado correctamente") }
        binding.cardValidation.setOnClickListener { showMessage("Todos los datos de pago están validados") }
        binding.btnSaveProfile.setOnClickListener { showMessage("Perfil guardado correctamente") }
        binding.btnLogout.setOnClickListener { showMessage("Cierre de sesión simulado") }
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}