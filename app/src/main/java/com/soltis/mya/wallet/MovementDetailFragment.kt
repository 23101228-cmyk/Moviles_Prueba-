package com.soltis.mya.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.soltis.mya.databinding.FragmentMovementDetailScreenBinding

class MovementDetailFragment : Fragment() {

    private var _binding: FragmentMovementDetailScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel: WalletViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovementDetailScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val index = requireArguments().getInt(ARG_MOVEMENT_INDEX, -1)
        val movement = viewModel.movements.value?.getOrNull(index)
        if (movement == null) {
            Toast.makeText(context, "Movimiento no disponible", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        bindMovement(index, movement)
    }

    private fun bindMovement(index: Int, movement: MovementItem) {
        binding.tvCode.text = "MOV-${1000 + index}"
        binding.tvType.text = movement.type
        binding.tvOperation.text = movement.operation
        binding.tvDate.text = movement.date
        binding.tvCurrency.text = movement.currency
        binding.tvAmount.text = movement.amount
        binding.tvStatus.text = "Completado"
        binding.tvAffectedBalance.text = if (movement.isPositive) "Saldo disponible aumenta" else "Saldo disponible disminuye"
        binding.tvReference.text = when (movement.typeCategory) {
            "retencion", "liberacion", "pago" -> "Referencia P2P simulada"
            "retiro" -> "Retiro hacia destino simulado"
            else -> "Movimiento de wallet"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_MOVEMENT_INDEX = "movementIndex"
    }
}
