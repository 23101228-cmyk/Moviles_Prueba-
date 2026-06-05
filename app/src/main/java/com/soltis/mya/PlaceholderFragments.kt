package com.soltis.mya

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.activityViewModels
import com.soltis.mya.databinding.*
import com.soltis.mya.ofertas.OfferViewModel

class InicioFragment : Fragment() {
    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class MarketOffer(val user: String, val stats: String, val price: String, val limits: String)

class MercadoFragment : Fragment() {
    private var _binding: FragmentMercadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMercadoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val offers = listOf(
            MarketOffer("Carlos_P2P", "250 órdenes | 99%", "S/ 3.75", "S/ 100 - S/ 2,000"),
            MarketOffer("Maria_Crypto", "89 órdenes | 95%", "S/ 3.76", "S/ 50 - S/ 500"),
            MarketOffer("Exchange_Fast", "1500 órdenes | 100%", "S/ 3.78", "S/ 500 - S/ 10,000")
        )

        binding.rvMercado.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MarketOfferAdapter(offers)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MarketOfferAdapter(private val items: List<MarketOffer>) :
    RecyclerView.Adapter<MarketOfferAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMarketOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarketOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvUserName.text = item.user
            tvUserStats.text = item.stats
            tvPriceValue.text = item.price
            tvLimitValue.text = item.limits
        }
    }

    override fun getItemCount() = items.size
}

data class MyOffer(val type: String, val asset: String, val amount: String, val price: String, val status: String)

class OfertasFragment : Fragment() {
    private var _binding: FragmentOfertasBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OfferViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOfertasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabAddOffer.setOnClickListener {
            findNavController().navigate(R.id.action_nav_ofertas_to_publishOfferFragment)
        }

        viewModel.myOffers.observe(viewLifecycleOwner) { offers ->
            if (offers.isNullOrEmpty()) {
                binding.emptyState.visibility = View.VISIBLE
                binding.rvOfertas.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.rvOfertas.visibility = View.VISIBLE
                binding.rvOfertas.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = MyOfferAdapter(offers)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MyOfferAdapter(private val items: List<MyOffer>) :
    RecyclerView.Adapter<MyOfferAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMyOfferBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvOfferType.text = item.type
            tvOfferAsset.text = item.asset
            tvOfferAmount.text = item.amount
            tvOfferPrice.text = item.price
            tvOfferStatus.text = item.status
            
            if (item.type == "VENTA") {
                tvOfferType.setTextColor(0xFFE74C3C.toInt())
                tvOfferType.setBackgroundColor(0xFFFFEBEE.toInt())
                tvOfferType.text = "VENTA"
            } else {
                tvOfferType.setTextColor(0xFF27AE60.toInt())
                tvOfferType.setBackgroundColor(0xFFE8F5E9.toInt())
                tvOfferType.text = "COMPRA"
            }
        }
    }

    override fun getItemCount() = items.size
}

class PerfilFragment : Fragment() {
    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun placeholderView(name: String, container: ViewGroup?): View {
    return android.widget.TextView(container?.context).apply {
        text = name
        textSize = 22f
        gravity = android.view.Gravity.CENTER
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }
}
