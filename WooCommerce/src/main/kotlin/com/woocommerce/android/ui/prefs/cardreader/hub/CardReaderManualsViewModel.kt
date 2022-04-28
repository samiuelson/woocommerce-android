package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val _manualState = getManualItems().toMutableStateList()
    val manualState: List<ManualItem>
    get() = _manualState

//    fun onManualClick(manualURL: AppUrls) {
//
//        triggerEvent(NavigateToManualEvent(manualURL))
//    }
//
//    data class NavigateToManualEvent(val manualUrl: AppUrls) : MultiLiveEvent.Event()
}

private fun getManualItems() = listOf(
    ManualItem(
        R.drawable.ic_bbposchipper,
        "BBPOS Chipper™ 2X BT"
    ),
    ManualItem(
        R.drawable.ic_p400,
        "Verifone® P400 Card Reader"
    )
)


sealed class CardReaderManualsViewState {
    abstract val rows: List<ManualItem>

    data class Content(override val rows: List<ManualItem>) : CardReaderManualsViewState()
}

data class ManualItem(
    val icon: Int,
    val label: String,
)
