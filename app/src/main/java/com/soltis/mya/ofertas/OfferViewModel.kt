package com.soltis.mya.ofertas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OfferViewModel : ViewModel() {
    private val _myOffers = MutableLiveData<MutableList<MyOffer>>(mutableListOf())
    val myOffers: LiveData<MutableList<MyOffer>> = _myOffers

    fun addOffer(offer: MyOffer) {
        val currentList = _myOffers.value ?: mutableListOf()
        currentList.add(0, offer) // Add to the beginning of the list
        _myOffers.value = currentList
    }
}
