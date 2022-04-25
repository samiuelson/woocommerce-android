package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.internal.toImmutableList
import javax.inject.Inject


@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
) : ScopedViewModel(savedState) {

    private val _manualState = MutableLiveData<ManualsListState>()
    val manualState: LiveData<ManualsListState> = _manualState

    fun onManualClick(manualURL: AppUrls) {

        triggerEvent(NavigateToManualEvent(manualURL))
    }

    data class ManualsListState(
        val manuals: List<ManualsListItem> = emptyList()
    )

    data class ManualsListItem(
        @DrawableRes val icon: Int,
        val label: Int,
        val onManualClick: String,
    )

    data class NavigateToManualEvent(val manualUrl: AppUrls) : MultiLiveEvent.Event()
}

