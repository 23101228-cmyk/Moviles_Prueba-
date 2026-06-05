package com.soltis.mya.mercado

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.databinding.FragmentMercadoBinding
import com.soltis.mya.databinding.ItemMarketOfferBinding

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
            MarketOffer("Carlos_P2P", "250 ordenes | 99%", "S/ 3.75", "S/ 100 - S/ 2,000"),
            MarketOffer("Maria_Crypto", "89 ordenes | 95%", "S/ 3.76", "S/ 50 - S/ 500"),
            MarketOffer("Exchange_Fast", "1500 ordenes | 100%", "S/ 3.78", "S/ 500 - S/ 10,000")
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
