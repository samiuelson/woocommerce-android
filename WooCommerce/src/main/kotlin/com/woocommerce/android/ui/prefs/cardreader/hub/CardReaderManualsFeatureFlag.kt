package com.woocommerce.android.ui.prefs.cardreader.hub

import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

class CardReaderManualsFeatureFlag @Inject constructor() {
    fun isEnabled() = FeatureFlag.CARD_READER_MANUALS.isEnabled(null)
}
