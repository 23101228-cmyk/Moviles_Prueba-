package com.soltis.mya.wallet

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentMovementsBinding

data class MovementItem(
    val iconRes: Int,
    val iconTint: Int,
    val type: String,
    val operation: String,
    val date: String,
    val amount: String,
    val currency: String,
    val isPositive: Boolean,
    val typeCategory: String
)

class MovementsFragment : Fragment() {

    private var _binding: FragmentMovementsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WalletViewModel by activityViewModels()

    private var selectedCurrency = "TODOS"
    private var selectedType = "TODOS"
    private var allMovements = listOf<MovementItem>()
    private lateinit var adapter: MovementsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCurrencyTabs()
        setupTypeChips()
        setupClickListeners()

        viewModel.movements.observe(viewLifecycleOwner) { movements ->
            allMovements = movements
            applyFilters()
        }
    }

    private fun setupRecyclerView() {
        adapter = MovementsAdapter(emptyList()) { movement ->
            val index = allMovements.indexOf(movement)
            findNavController().navigate(
                R.id.nav_movement_detail_screen,
                Bundle().apply { putInt(MovementDetailFragment.ARG_MOVEMENT_INDEX, index) }
            )
        }
        binding.rvMovimientos.layoutManager = LinearLayoutManager(context)
        binding.rvMovimientos.adapter = adapter
    }

    private val currencyTabs by lazy {
        listOf(
            "TODOS" to binding.tabTodos,
            "PEN" to binding.tabPen,
            "USD" to binding.tabUsd,
            "EUR" to binding.tabEur
        )
    }

    private fun setupCurrencyTabs() {
        currencyTabs.forEach { (currency, tab) ->
            tab.setOnClickListener {
                selectedCurrency = currency
                currencyTabs.forEach { (_, t) ->
                    t.setBackgroundResource(android.R.color.transparent)
                    t.setTextColor(Color.parseColor("#888888"))
                    t.typeface = android.graphics.Typeface.DEFAULT
                }
                tab.setBackgroundResource(R.drawable.bg_tab_selected)
                tab.setTextColor(Color.WHITE)
                tab.typeface = android.graphics.Typeface.DEFAULT_BOLD
                applyFilters()
            }
        }
    }

    private val typeChips by lazy {
        listOf(
            "TODOS" to binding.chipTodos,
            "recarga" to binding.chipRecargas,
            "retencion" to binding.chipRetenciones,
            "liberacion" to binding.chipLiberaciones,
            "pago" to binding.chipPagos
        )
    }

    private fun setupTypeChips() {
        typeChips.forEach { (type, chip) ->
            chip.setOnClickListener {
                selectedType = type
                typeChips.forEach { (_, c) ->
                    c.setBackgroundResource(R.drawable.bg_type_chip_unselected)
                    c.setTextColor(Color.parseColor("#888888"))
                    c.typeface = android.graphics.Typeface.DEFAULT
                }
                chip.setBackgroundResource(R.drawable.bg_type_chip_selected)
                chip.setTextColor(Color.WHITE)
                chip.typeface = android.graphics.Typeface.DEFAULT_BOLD
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val filtered = allMovements.filter { movement ->
            val matchCurrency = selectedCurrency == "TODOS" || movement.currency == selectedCurrency
            val matchType = selectedType == "TODOS" || movement.typeCategory == selectedType
            matchCurrency && matchType
        }
        adapter.updateData(filtered)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnFilter.setOnClickListener {
            Toast.makeText(context, "Filtros avanzados simulados", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MovementsAdapter(
    private var items: List<MovementItem>,
    private val onClick: (MovementItem) -> Unit
) : RecyclerView.Adapter<MovementsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgMovIcon)
        val tvType: TextView = view.findViewById(R.id.tvMovType)
        val tvOp: TextView = view.findViewById(R.id.tvMovOperation)
        val tvDate: TextView = view.findViewById(R.id.tvMovDate)
        val tvAmount: TextView = view.findViewById(R.id.tvMovAmount)
        val tvCurrency: TextView = view.findViewById(R.id.tvMovCurrency)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movement_detail, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.imgIcon.setImageResource(item.iconRes)
        holder.imgIcon.setColorFilter(item.iconTint)
        holder.tvType.text = item.type
        holder.tvOp.text = item.operation
        holder.tvDate.text = item.date
        holder.tvAmount.text = item.amount
        holder.tvCurrency.text = item.currency
        holder.tvAmount.setTextColor(if (item.isPositive) Color.parseColor("#27AE60") else Color.parseColor("#E74C3C"))
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<MovementItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
