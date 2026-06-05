package com.soltis.mya.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentWalletBinding
import com.soltis.mya.databinding.ItemMovementBinding

// Modelo de datos para movimientos
data class Movement(
    val iconRes: Int,
    val type: String,
    val detail: String,
    val amount: String,
    val status: String,
    val isPositive: Boolean
)

class WalletFragment : Fragment() {

    private var _binding: FragmentWalletBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Observar saldos disponibles
        viewModel.balances.observe(viewLifecycleOwner) { balances ->
            updateTotalBalanceUI(balances)
            updateIndividualBalancesUI()
        }

        // Observar saldos retenidos
        viewModel.heldBalances.observe(viewLifecycleOwner) { held ->
            updateIndividualBalancesUI()
            updateMetricsUI()
        }

        // Observar métricas de recarga y liberación
        viewModel.totalRecharged.observe(viewLifecycleOwner) { updateMetricsUI() }
        viewModel.totalReleased.observe(viewLifecycleOwner) { updateMetricsUI() }
        
        setupMovements()
        setupClickListeners()
    }

    private fun updateMetricsUI() {
        val recharged = viewModel.totalRecharged.value ?: emptyMap()
        val released = viewModel.totalReleased.value ?: emptyMap()
        val held = viewModel.heldBalances.value ?: emptyMap()

        // Para el resumen, mostramos el total equivalente en soles (PEN)
        val totalRechargedPen = (recharged["PEN"] ?: 0.0) + (recharged["USD"] ?: 0.0) * 3.75
        val totalHeldPen = (held["PEN"] ?: 0.0) + (held["USD"] ?: 0.0) * 3.75
        val totalReleasedPen = (released["PEN"] ?: 0.0) + (released["USD"] ?: 0.0) * 3.75

        binding.tvMetricRecharge.text = "S/ ${"%,.2f".format(totalRechargedPen)}"
        binding.tvMetricHeld.text = "S/ ${"%,.2f".format(totalHeldPen)}"
        binding.tvMetricRelease.text = "S/ ${"%,.2f".format(totalReleasedPen)}"
    }

    private fun updateTotalBalanceUI(balances: Map<String, Double>) {
        val penBalance = balances["PEN"] ?: 0.0
        val usdBalance = balances["USD"] ?: 0.0
        val eurBalance = balances["EUR"] ?: 0.0

        val totalInPen = penBalance + (usdBalance * 3.75) + (eurBalance * 4.05)
        val totalInUsd = totalInPen / 3.75

        binding.tvBalanceAmount.text = "S/ ${"%,.2f".format(totalInPen)}"
        binding.tvBalanceUsd.text = "≈ $ ${"%,.2f".format(totalInUsd)} USD"
    }

    private fun updateIndividualBalancesUI() {
        val balances = viewModel.balances.value ?: return
        val held = viewModel.heldBalances.value ?: return

        // PEN
        binding.itemPen.apply {
            val amount = balances["PEN"] ?: 0.0
            val heldAmount = held["PEN"] ?: 0.0
            imgCurrencyFlag.setImageResource(R.drawable.ic_flag_pen)
            tvCurrencyCode.text = "PEN"
            tvCurrencyName.text = "Sol peruano"
            tvCurrencyAmount.text = "S/ ${"%,.2f".format(amount)}"
            tvCurrencyAmountAlt.text = "Retenido: S/ ${"%,.2f".format(heldAmount)}"
            tvCurrencyAmountAlt.setTextColor(if (heldAmount > 0) 0xFFE67E22.toInt() else 0xFF757575.toInt())
        }

        // USD
        binding.itemUsd.apply {
            val amount = balances["USD"] ?: 0.0
            val heldAmount = held["USD"] ?: 0.0
            imgCurrencyFlag.setImageResource(R.drawable.ic_flag_usd)
            tvCurrencyCode.text = "USD"
            tvCurrencyName.text = "Dólar estadounidense"
            tvCurrencyAmount.text = "$ ${"%,.2f".format(amount)}"
            tvCurrencyAmountAlt.text = "Retenido: $ ${"%,.2f".format(heldAmount)}"
            tvCurrencyAmountAlt.setTextColor(if (heldAmount > 0) 0xFFE67E22.toInt() else 0xFF757575.toInt())
        }
    }

    private fun setupCurrencies() {
        // Este método ya no es necesario o puede quedar vacío
    }

    private fun setupMovements() {
        viewModel.movements.observe(viewLifecycleOwner) { movements ->
            binding.rvMovimientos.apply {
                layoutManager = LinearLayoutManager(context)
                // Adaptar MovementItem de ViewModel a Movement de este Fragment si es necesario, 
                // o mejor, usar el mismo modelo. Por ahora, mapeamos rápido:
                val mappedMovements = movements.take(5).map { 
                    Movement(it.iconRes, it.type, it.operation, it.amount, "Completado", it.isPositive)
                }
                adapter = MovementAdapter(mappedMovements)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnDepositar.setOnClickListener {
            findNavController().navigate(R.id.nav_deposit)
        }
        binding.btnRetirar.setOnClickListener { Toast.makeText(context, "Retirar", Toast.LENGTH_SHORT).show() }
        binding.btnTransferir.setOnClickListener { Toast.makeText(context, "Transferir", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adaptador usando ViewBinding para los ítems de la lista
class MovementAdapter(private val items: List<Movement>) :
    RecyclerView.Adapter<MovementAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMovementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMovementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            imgMovIcon.setImageResource(item.iconRes)
            tvMovType.text = item.type
            tvMovAmount.text = item.amount
            tvMovAmount.setTextColor(if (item.isPositive) 0xFF27AE60.toInt() else 0xFFE74C3C.toInt())
        }
    }

    override fun getItemCount() = items.size
}
