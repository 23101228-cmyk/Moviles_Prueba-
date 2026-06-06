package com.soltis.mya.ofertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OfferViewModel : ViewModel() {
    private val _myOffers = MutableLiveData<MutableList<MyOffer>>(mutableListOf())
    val myOffers: LiveData<MutableList<MyOffer>> = _myOffers

    fun addOffer(offer: MyOffer) {
        val currentList = _myOffers.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, offer)
        _myOffers.value = currentList
    }
}
