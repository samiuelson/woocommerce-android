package com.woocommerce.android.ui.prefs.cardreader.hub

import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.model.UiString
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class CardReaderManualsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {



}

data class ManualsListStated(
    val manuals: List<CardReaderManualsItem> = emptyList()
)

data class CardReaderManualsItem(
    @DrawableRes val icon: Int,
    val label: UiString,
    val onItemClicked: () -> Unit
)
