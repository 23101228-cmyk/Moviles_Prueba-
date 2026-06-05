package com.soltis.mya.ofertas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.soltis.mya.R
import com.soltis.mya.databinding.FragmentOfertasBinding
import com.soltis.mya.databinding.ItemMyOfferBinding

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
