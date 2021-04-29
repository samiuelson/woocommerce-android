package com.woocommerce.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

abstract class ScopedViewModel(
    protected val savedState: SavedStateHandle,
    protected val dispatchers: CoroutineDispatchers
) : ViewModel(), CoroutineScope {
    private val _event = MultiLiveEvent<Event>()
    val event: LiveData<Event> = _event

    override val coroutineContext: CoroutineContext
        get() = viewModelScope.coroutineContext

    protected fun triggerEvent(event: Event) {
        event.isHandled = false
        _event.value = event
    }
}
