package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.compose.runtime.toMutableStateList
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

    private fun getManualItems(): List<ManualItem> = listOf(
        ManualItem(
            R.drawable.ic_bbposchipper,
            R.string.bbpos_reader,
            onManualClicked = ::onBbposManualCliked

            ),
        ManualItem(
            R.drawable.ic_p400,
            R.string.M2_reader,
            onManualClicked = ::onM2anualCliked
        )
    )

    private fun onBbposManualCliked() {
        triggerEvent(ManualsEvents.NavigateToCardReaderManualLink(AppUrls.BBPOS_MANUAL_CARD_READER))
    }

    private fun onM2anualCliked() {
        triggerEvent(ManualsEvents.NavigateToCardReaderManualLink(AppUrls.M2_MANUAL_CARD_READER))
    }

    sealed class ManualsEvents: MultiLiveEvent.Event() {
        data class NavigateToCardReaderManualLink(val url: String): ManualsEvents()
    }

    data class ManualItem(
        val icon: Int,
        val label: Int,
        val onManualClicked: () -> Unit
    )

}



