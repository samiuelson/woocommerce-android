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

    private fun getManualItems() = listOf(
        ManualItem(
            R.drawable.ic_bbposchipper,
            "BBPOS Chipper™ 2X BT",
            onManualClicked = ::onBbposManualCliked

            ),
        ManualItem(
            R.drawable.ic_p400,
            "Stripe M2 Reader",
            onManualClicked = ::onP400ManualCliked
        )
    )

    private fun onBbposManualCliked() {
        triggerEvent(ManualsEvents.NavigateToCardReaderManualFlow(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    private fun onP400ManualCliked() {
        triggerEvent(ManualsEvents.NavigateToCardReaderManualFlow(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    sealed class ManualsEvents: MultiLiveEvent.Event() {
        data class NavigateToCardReaderManualFlow(val url: String): ManualsEvents()
    }

    data class ManualItem(
        val icon: Int,
        val label: String,
        val onManualClicked: () -> Unit
    )

}



