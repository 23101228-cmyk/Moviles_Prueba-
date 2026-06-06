package com.soltis.mya.inicio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentInicioBinding

class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cardBuy.setOnClickListener { findNavController().navigate(R.id.nav_mercado) }
        binding.cardSell.setOnClickListener { findNavController().navigate(R.id.nav_mercado) }
        binding.btnGoToWallet.setOnClickListener { findNavController().navigate(R.id.nav_wallet) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
