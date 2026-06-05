package com.soltis.mya.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentWalletBinding
import com.soltis.mya.databinding.ItemMovementBinding

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
    private var accountsExpanded = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.balances.observe(viewLifecycleOwner) { balances ->
            updateTotalBalanceUI(balances)
            updateIndividualBalancesUI()
        }
        viewModel.heldBalances.observe(viewLifecycleOwner) {
            updateIndividualBalancesUI()
            updateMetricsUI()
        }
        viewModel.totalRecharged.observe(viewLifecycleOwner) { updateMetricsUI() }
        viewModel.totalReleased.observe(viewLifecycleOwner) { updateMetricsUI() }
        viewModel.totalWithdrawn.observe(viewLifecycleOwner) { updateMetricsUI() }

        setupMovements()
        setupClickListeners()
    }

    private fun updateMetricsUI() {
        val recharged = viewModel.totalRecharged.value ?: emptyMap()
        val released = viewModel.totalReleased.value ?: emptyMap()
        val held = viewModel.heldBalances.value ?: emptyMap()
        val withdrawn = viewModel.totalWithdrawn.value ?: emptyMap()

        binding.tvMetricRecharge.text = "S/ ${"%,.2f".format(toPen(recharged))}"
        binding.tvMetricHeld.text = "S/ ${"%,.2f".format(toPen(held))}"
        binding.tvMetricRelease.text = "S/ ${"%,.2f".format(toPen(released) + toPen(withdrawn))}"
    }

    private fun toPen(values: Map<String, Double>): Double {
        return (values["PEN"] ?: 0.0) + (values["USD"] ?: 0.0) * 3.75 + (values["EUR"] ?: 0.0) * 4.05
    }

    private fun updateTotalBalanceUI(balances: Map<String, Double>) {
        val totalInPen = toPen(balances)
        val totalInUsd = totalInPen / 3.75

        binding.tvBalanceAmount.text = "S/ ${"%,.2f".format(totalInPen)}"
        binding.tvBalanceUsd.text = "≈ $ ${"%,.2f".format(totalInUsd)} USD"
    }

    private fun updateIndividualBalancesUI() {
        val balances = viewModel.balances.value ?: return
        val held = viewModel.heldBalances.value ?: return

        bindCurrency("PEN", "Sol peruano", R.drawable.ic_flag_pen, "S/", balances, held, binding.itemPen)
        bindCurrency("USD", "Dolar estadounidense", R.drawable.ic_flag_usd, "$", balances, held, binding.itemUsd)
        bindCurrency("EUR", "Euro", R.drawable.ic_flag_eur, "EUR", balances, held, binding.itemEur)
    }

    private fun bindCurrency(
        code: String,
        name: String,
        flagRes: Int,
        symbol: String,
        balances: Map<String, Double>,
        held: Map<String, Double>,
        item: com.soltis.mya.databinding.ItemCurrencyBinding
    ) {
        val amount = balances[code] ?: 0.0
        val heldAmount = held[code] ?: 0.0

        item.imgCurrencyFlag.setImageResource(flagRes)
        item.tvCurrencyCode.text = code
        item.tvCurrencyName.text = name
        item.tvCurrencyAmount.text = "$symbol ${"%,.2f".format(amount)}"
        item.tvCurrencyAmountAlt.text = "Retenido: $symbol ${"%,.2f".format(heldAmount)}"
        item.tvCurrencyAmountAlt.setTextColor(if (heldAmount > 0) 0xFFE67E22.toInt() else 0xFF757575.toInt())
    }

    private fun setupMovements() {
        viewModel.movements.observe(viewLifecycleOwner) { movements ->
            binding.rvMovimientos.layoutManager = LinearLayoutManager(context)
            binding.rvMovimientos.adapter = MovementAdapter(
                movements.take(5).map {
                    Movement(it.iconRes, it.type, it.operation, it.amount, "Completado", it.isPositive)
                }
            )
        }
    }

    private fun setupClickListeners() {
        binding.btnDepositar.setOnClickListener {
            findNavController().navigate(R.id.nav_deposit)
        }
        binding.btnRetirar.setOnClickListener {
            findNavController().navigate(R.id.nav_withdraw)
        }
        binding.btnTransferir.setOnClickListener {
            Toast.makeText(context, "Transferencia simulada pendiente", Toast.LENGTH_SHORT).show()
        }
        binding.layoutMisCuentasHeader.setOnClickListener {
            accountsExpanded = !accountsExpanded
            binding.layoutMisCuentasContent.visibility = if (accountsExpanded) View.VISIBLE else View.GONE
            binding.tvMisCuentasArrow.text = if (accountsExpanded) "v" else ">"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

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
            tvMovDetail.text = item.detail
            tvMovAmount.text = item.amount
            tvMovAmount.setTextColor(if (item.isPositive) 0xFF27AE60.toInt() else 0xFFE74C3C.toInt())
        }
    }

    override fun getItemCount() = items.size
}
