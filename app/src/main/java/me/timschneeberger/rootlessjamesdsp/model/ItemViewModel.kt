package me.timschneeberger.rootlessjamesdsp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ItemViewModel<T> : ViewModel() {
    private val mutableSelectedItem = MutableLiveData<T>()
    val selectedItem: LiveData<T> get() = mutableSelectedItem

    fun selectItem(item: T) {
        mutableSelectedItem.value = item!!
    }
}