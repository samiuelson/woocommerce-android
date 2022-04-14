package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
//    private val appPrefsWrapper: AppPrefsWrapper,
//    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {

    val manualState = MutableLiveData<ManualsListState>()

    fun onManualClick(manualURL: String) {

        // TODO triggerEvent(Manuallistevent.navigate...(url)
    }

    data class ManualsListState(
        val manuals: List<ManualsListItem> = emptyList()
    )

    data class ManualsListItem(
        @DrawableRes val icon: Int,
        val label: Int,
        val onManualClick: (String) -> Unit,
        val manual: ManualsListItem,
    )

    sealed class CardReaderManualsListEvent: MultiLiveEvent.Event() {
        data class NavigateToBBPOSChipperManual(val url: String) : CardReaderManualsListEvent()
        data class NavigateToP400Manual(val url: String): CardReaderManualsListEvent()
    }

}

