package com.woocommerce.android.ui.reviews.detail

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.RequestResult.ERROR
import com.woocommerce.android.model.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.model.RequestResult.SUCCESS
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.reviews.OnRequestModerateReviewEvent
import com.woocommerce.android.ui.reviews.ProductReviewModerationRequest
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.ui.reviews.ReviewDetailRepository
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.MarkNotificationAsRead
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val repository: ReviewDetailRepository
) : ScopedViewModel(savedState) {
    private var remoteReviewId = 0L

    private val _uiState = MutableStateFlow(
        ViewState(
            enableModeration = savedState.get<Boolean>("enableModeration")!!,
            reviewApproved = savedState.get<Boolean>("enableModeration")!!
        )
    )
    val uiState: StateFlow<ViewState> = _uiState.asStateFlow()

    init {
        loadProductReview(
            savedState.get<Long>("remoteReviewId")!!,
            savedState.get<Boolean>("launchedFromNotification")!!,
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.onCleanup()
    }

    fun moderateReview(newStatus: ProductReviewStatus) {
        if (networkStatus.isConnected()) {
            _uiState.value.productReview?.let { review ->
                // post an event to tell the notification list to moderate this
                // review, then close the fragment
                val event = OnRequestModerateReviewEvent(
                    ProductReviewModerationRequest(review, newStatus)
                )
                EventBus.getDefault().post(event)

                // Close the detail view
                triggerEvent(Exit)
            }
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    private fun loadProductReview(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
    ) {
        // Mark the notification as read
        launch {
            markAsRead(remoteReviewId, launchedFromNotification)
        }

        val shouldFetch = remoteReviewId != this.remoteReviewId
        this.remoteReviewId = remoteReviewId

        launch {
            _uiState.value = _uiState.value.copy(
                isSkeletonShown = true
            )

            val reviewInDb = repository.getCachedProductReview(remoteReviewId)
            if (reviewInDb != null) {
                _uiState.value = _uiState.value.copy(
                    productReview = reviewInDb,
                    isSkeletonShown = false,
                    reviewApproved = isReviewApproved(ProductReviewStatus.fromString(reviewInDb.status))
                )

                if (shouldFetch) {
                    // Fetch it asynchronously so the db version loads immediately
                    fetchProductReview(remoteReviewId)
                }
            } else {
                fetchProductReview(remoteReviewId)
            }
        }
    }

    private fun fetchProductReview(remoteReviewId: Long) {
        if (networkStatus.isConnected()) {
            launch {
                when (repository.fetchProductReview(remoteReviewId)) {
                    SUCCESS, NO_ACTION_NEEDED -> {
                        repository.getCachedProductReview(remoteReviewId)?.let { review ->
                            _uiState.value = _uiState.value.copy(
                                productReview = review,
                                isSkeletonShown = false,
                                reviewApproved = isReviewApproved(ProductReviewStatus.fromString(review.status))
                            )
                        }
                    }
                    ERROR -> triggerEvent(ShowSnackbar(R.string.wc_load_review_error))
                }
            }
        } else {
            // Network is not connected
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    private suspend fun markAsRead(remoteReviewId: Long, launchedFromNotification: Boolean) {
        repository.getCachedNotificationForReview(remoteReviewId)?.let {
            // remove notification from the notification panel if it exists
            triggerEvent(MarkNotificationAsRead(it.remoteNoteId))

            // send request to mark notification as read to the server
            repository.markNotificationAsRead(it)

            if (launchedFromNotification) {
                // Send the track event that a product review notification was opened
                AnalyticsTracker.track(
                    Stat.NOTIFICATION_OPEN,
                    mapOf(
                        AnalyticsTracker.KEY_TYPE to AnalyticsTracker.VALUE_REVIEW,
                        AnalyticsTracker.KEY_ALREADY_READ to it.read
                    )
                )
            }
        }
    }

    private fun isReviewApproved(status: ProductReviewStatus) =
        when (savedState.get<String>("tempStatus")?.let { ProductReviewStatus.fromString(it) } ?: status) {
            ProductReviewStatus.APPROVED -> true
            else -> false
        }

    @Parcelize
    data class ViewState(
        val productReview: ProductReview? = null,
        val isSkeletonShown: Boolean = false,
        val enableModeration: Boolean = true,
        val reviewApproved: Boolean = false
    ) : Parcelable

    sealed class ReviewDetailEvent : Event() {
        data class MarkNotificationAsRead(val remoteNoteId: Long) : ReviewDetailEvent()
    }
}
