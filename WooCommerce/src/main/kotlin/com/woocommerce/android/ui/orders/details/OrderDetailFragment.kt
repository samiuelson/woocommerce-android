package com.woocommerce.android.ui.orders.details

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.FEATURE_FEEDBACK_BANNER
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_DETAIL_PRODUCT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.databinding.FragmentOrderDetailBinding
import com.woocommerce.android.extensions.*
import com.woocommerce.android.model.*
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.SHIPPING_LABEL_M4
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentDialogFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.orders.OrderNavigationTarget
import com.woocommerce.android.ui.orders.OrderNavigator
import com.woocommerce.android.ui.orders.OrderProductActionListener
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel.OrderStatusUpdateSource
import com.woocommerce.android.ui.orders.details.adapter.OrderDetailShippingLabelsAdapter.OnShippingLabelClickListener
import com.woocommerce.android.ui.orders.details.editing.OrderEditingViewModel
import com.woocommerce.android.ui.orders.details.views.OrderDetailOrderStatusView.Mode
import com.woocommerce.android.ui.orders.fulfill.OrderFulfillViewModel
import com.woocommerce.android.ui.orders.notes.AddOrderNoteFragment
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragment
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundFragment
import com.woocommerce.android.ui.orders.simplepayments.TakePaymentViewModel
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragment
import com.woocommerce.android.ui.refunds.RefundSummaryFragment
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowUndoSnackbar
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderDetailFragment : BaseFragment(R.layout.fragment_order_detail), OrderProductActionListener {
    companion object {
        val TAG: String = OrderDetailFragment::class.java.simpleName
    }

    private val viewModel: OrderDetailViewModel by viewModels()
    private val orderEditingViewModel by hiltNavGraphViewModels<OrderEditingViewModel>(R.id.nav_graph_orders)

    @Inject lateinit var navigator: OrderNavigator
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap
    @Inject lateinit var dateUtils: DateUtils
    @Inject lateinit var cardReaderManager: CardReaderManager

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val skeletonView = SkeletonView()
    private var undoSnackbar: Snackbar? = null
    private var mnuSharePaymentLink: MenuItem? = null

    private var screenTitle = ""
        set(value) {
            field = value
            updateActivityTitle()
        }

    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(SHIPPING_LABEL_M4)?.feedbackState
            ?: UNANSWERED

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        undoSnackbar?.dismiss()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.default_window_background)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.snack_root
            duration = transitionDuration
            scrimColor = Color.TRANSPARENT
            startContainerColor = backgroundColor
            endContainerColor = backgroundColor
        }
    }

    override fun getFragmentTitle() = screenTitle

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderDetailBinding.bind(view)

        setHasOptionsMenu(true)
        setupObservers(viewModel)
        setupOrderEditingObservers(orderEditingViewModel)
        setupResultHandlers(viewModel)

        binding.orderDetailOrderStatus.initView(mode = Mode.OrderEdit) {
            viewModel.onEditOrderStatusSelected()
        }
        binding.orderRefreshLayout.apply {
            scrollUpChild = binding.scrollView
            setOnRefreshListener { viewModel.onRefreshRequested() }
        }

        ViewCompat.setTransitionName(
            binding.scrollView,
            getString(R.string.order_card_detail_transition_name)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_order_detail, menu)
        mnuSharePaymentLink = menu.findItem(R.id.menu_share_payment_link)
        menu.add(0, 123, 0, "Edit").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 123) {
            findNavController().navigate(OrderDetailFragmentDirections.actionOrderDetailFragmentToOrderCreationFragment(viewModel.order))
        }
        return if (item.itemId == R.id.menu_share_payment_link) {
            viewModel.onSharePaymentUrlClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
    override fun openOrderProductDetail(remoteProductId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductDetail(remoteProductId)
    }

    override fun openOrderProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        AnalyticsTracker.track(ORDER_DETAIL_PRODUCT_TAPPED)
        (activity as? MainNavigationRouter)?.showProductVariationDetail(remoteProductId, remoteVariationId)
    }

    private fun setupObservers(viewModel: OrderDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.orderInfo?.takeIfNotEqualTo(old?.orderInfo) {
                showOrderDetail(it.order!!, it.isPaymentCollectableWithCardReader, it.isReceiptButtonsVisible)
            }
            new.orderStatus?.takeIfNotEqualTo(old?.orderStatus) { showOrderStatus(it) }
            new.isMarkOrderCompleteButtonVisible?.takeIfNotEqualTo(old?.isMarkOrderCompleteButtonVisible) {
                showMarkOrderCompleteButton(it)
            }
            new.isCreateShippingLabelButtonVisible?.takeIfNotEqualTo(old?.isCreateShippingLabelButtonVisible) {
                showShippingLabelButton(it)
            }
            new.isProductListMenuVisible?.takeIfNotEqualTo(old?.isProductListMenuVisible) {
                showProductListMenuButton(it)
            }
            new.isCreateShippingLabelBannerVisible.takeIfNotEqualTo(old?.isCreateShippingLabelBannerVisible) {
                displayShippingLabelsWIPCard(it)
            }
            new.isProductListVisible?.takeIfNotEqualTo(old?.isProductListVisible) {
                binding.orderDetailProductList.isVisible = it
            }
            new.isSharePaymentLinkVisible?.takeIfNotEqualTo(old?.isSharePaymentLinkVisible) {
                mnuSharePaymentLink?.isVisible = new.isSharePaymentLinkVisible
            }
            new.toolbarTitle?.takeIfNotEqualTo(old?.toolbarTitle) { screenTitle = it }
            new.isOrderDetailSkeletonShown?.takeIfNotEqualTo(old?.isOrderDetailSkeletonShown) { showSkeleton(it) }
            new.isShipmentTrackingAvailable?.takeIfNotEqualTo(old?.isShipmentTrackingAvailable) {
                showAddShipmentTracking(it)
            }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.orderRefreshLayout.isRefreshing = it
            }
            new.refreshedProductId?.takeIfNotEqualTo(old?.refreshedProductId) { refreshProduct(it) }
        }

        viewModel.orderNotes.observe(
            viewLifecycleOwner,
            Observer {
                showOrderNotes(it)
            }
        )
        viewModel.orderRefunds.observe(
            viewLifecycleOwner,
            Observer {
                showOrderRefunds(it, viewModel.order)
            }
        )
        viewModel.productList.observe(
            viewLifecycleOwner,
            Observer {
                showOrderProducts(it, viewModel.order.currency)
            }
        )
        viewModel.shipmentTrackings.observe(
            viewLifecycleOwner,
            Observer {
                showShipmentTrackings(it)
            }
        )
        viewModel.shippingLabels.observe(
            viewLifecycleOwner,
            Observer {
                showShippingLabels(it, viewModel.order.currency)
            }
        )

        viewModel.event.observe(
            viewLifecycleOwner,
            Observer { event ->
                when (event) {
                    is ShowSnackbar -> {
                        if (event.args.isNotEmpty()) {
                            uiMessageResolver.getSnack(event.message, *event.args).show()
                        } else {
                            uiMessageResolver.showSnack(event.message)
                        }
                    }
                    is ShowUndoSnackbar -> {
                        displayUndoSnackbar(event.message, event.undoAction, event.dismissAction)
                    }
                    is OrderNavigationTarget -> navigator.navigate(this, event)
                    is TakePaymentViewModel.SharePaymentUrl -> {
                        sharePaymentUrl(event.storeName, event.paymentUrl)
                    }
                    else -> event.isHandled = false
                }
            }
        )
        viewModel.start()
    }

    private fun setupOrderEditingObservers(orderEditingViewModel: OrderEditingViewModel) {
        orderEditingViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OrderEditingViewModel.OrderEdited -> viewModel.onOrderEdited()
                is OrderEditingViewModel.OrderEditFailed -> viewModel.onOrderEditFailed(event.message)
            }
        }
    }

    private fun setupResultHandlers(viewModel: OrderDetailViewModel) {
        handleDialogResult<OrderStatusUpdateSource>(
            key = OrderStatusSelectorDialog.KEY_ORDER_STATUS_RESULT,
            entryId = R.id.orderDetailFragment
        ) { updateSource ->
            viewModel.onOrderStatusChanged(updateSource)
        }
        handleResult<OrderNote>(AddOrderNoteFragment.KEY_ADD_NOTE_RESULT) {
            viewModel.onNewOrderNoteAdded(it)
        }
        handleResult<Boolean>(ShippingLabelRefundFragment.KEY_REFUND_SHIPPING_LABEL_RESULT) {
            viewModel.onShippingLabelRefunded()
        }
        handleResult<OrderShipmentTracking>(AddOrderShipmentTrackingFragment.KEY_ADD_SHIPMENT_TRACKING_RESULT) {
            viewModel.onNewShipmentTrackingAdded(it)
        }
        handleResult<OrderStatusUpdateSource>(OrderFulfillViewModel.KEY_ORDER_FULFILL_RESULT) { updateSource ->
            viewModel.onOrderStatusChanged(updateSource)
        }
        handleResult<Boolean>(OrderFulfillViewModel.KEY_REFRESH_SHIPMENT_TRACKING_RESULT) {
            viewModel.refreshShipmentTracking()
        }
        handleDialogNotice<String>(
            key = CardReaderPaymentDialogFragment.KEY_CARD_PAYMENT_RESULT,
            entryId = R.id.orderDetailFragment
        ) {
            if (FeatureFlag.CARD_READER.isEnabled()) {
                viewModel.onCardReaderPaymentCompleted()
            }
        }
        handleNotice(RefundSummaryFragment.REFUND_ORDER_NOTICE_KEY) {
            viewModel.onOrderItemRefunded()
        }
        handleNotice(PrintShippingLabelFragment.KEY_LABEL_PURCHASED) {
            viewModel.onShippingLabelsPurchased()
        }
    }

    private fun showOrderDetail(
        order: Order,
        isPaymentCollectableWithCardReader: Boolean,
        isReceiptButtonsVisible: Boolean
    ) {
        binding.orderDetailOrderStatus.updateOrder(order)
        binding.orderDetailShippingMethodNotice.isVisible = order.multiShippingLinesAvailable
        binding.orderDetailCustomerInfo.updateCustomerInfo(
            order = order,
            isVirtualOrder = viewModel.hasVirtualProductsOnly(),
            isReadOnly = false
        )
        binding.orderDetailPaymentInfo.updatePaymentInfo(
            order = order,
            isPaymentCollectableWithCardReader = isPaymentCollectableWithCardReader,
            isReceiptAvailable = isReceiptButtonsVisible,
            formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(order.currency),
            onIssueRefundClickListener = { viewModel.onIssueOrderRefundClicked() },
            onSeeReceiptClickListener = {
                if (FeatureFlag.CARD_READER.isEnabled()) {
                    viewModel.onSeeReceiptClicked()
                }
            },
            onCollectCardPresentPaymentClickListener = {
                if (FeatureFlag.CARD_READER.isEnabled()) {
                    cardReaderManager.let {
                        viewModel.onAcceptCardPresentPaymentClicked()
                    }
                }
            },
            onPrintingInstructionsClickListener = {
                if (FeatureFlag.CARD_READER.isEnabled()) {
                    viewModel.onPrintingInstructionsClicked()
                }
            }
        )
    }

    private fun showOrderStatus(orderStatus: OrderStatus) {
        binding.orderDetailOrderStatus.updateStatus(orderStatus)
    }

    private fun showMarkOrderCompleteButton(isVisible: Boolean) {
        binding.orderDetailProductList.showMarkOrderCompleteButton(
            isVisible,
            viewModel::onMarkOrderCompleteButtonTapped
        )
    }

    private fun showShippingLabelButton(isVisible: Boolean) {
        binding.orderDetailProductList.showCreateShippingLabelButton(
            isVisible,
            viewModel::onCreateShippingLabelButtonTapped,
            viewModel::onShippingLabelNoticeTapped
        )
    }

    private fun showProductListMenuButton(isVisible: Boolean) {
        binding.orderDetailProductList.showProductListMenuButton(isVisible)
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> skeletonView.show(binding.orderDetailContainer, R.layout.skeleton_order_detail, delayed = true)
            false -> skeletonView.hide()
        }
    }

    private fun sharePaymentUrl(storeName: String, paymentUrl: String) {
        val subject = getString(R.string.simple_payments_share_payment_dialog_title, storeName)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, paymentUrl)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, storeName))
    }

    private fun refreshProduct(remoteProductId: Long) {
        binding.orderDetailProductList.notifyProductChanged(remoteProductId)
    }

    private fun showOrderNotes(orderNotes: List<OrderNote>) {
        binding.orderDetailNoteList.updateOrderNotesView(orderNotes) {
            viewModel.onAddOrderNoteClicked()
        }
    }

    private fun showOrderRefunds(refunds: List<Refund>, order: Order) {
        // display the refunds count in the refunds section
        val refundsCount = refunds.sumBy { refund -> refund.items.sumBy { it.quantity } }
        if (refundsCount > 0) {
            binding.orderDetailRefundsInfo.show()
            binding.orderDetailRefundsInfo.updateRefundCount(refundsCount) {
                viewModel.onViewRefundedProductsClicked()
            }
        } else {
            binding.orderDetailRefundsInfo.hide()
        }

        // display refunds list in the payment info section, if available
        val formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)

        refunds.whenNotNullNorEmpty {
            binding.orderDetailPaymentInfo.showRefunds(order, it, formatCurrency)
        }.otherwise {
            binding.orderDetailPaymentInfo.showRefundTotal(
                show = order.isRefundAvailable,
                refundTotal = order.refundTotal,
                formatCurrencyForDisplay = formatCurrency
            )
        }
    }

    private fun showOrderProducts(products: List<Order.Item>, currency: String) {
        products.whenNotNullNorEmpty {
            with(binding.orderDetailProductList) {
                updateProductList(
                    orderItems = products,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderDetailFragment,
                    onProductMenuItemClicked = viewModel::onCreateShippingLabelButtonTapped,
                    onViewAddonsClick = viewModel::onViewOrderedAddonButtonTapped
                )
            }
        }.otherwise { binding.orderDetailProductList.hide() }
    }

    private fun showAddShipmentTracking(show: Boolean) {
        with(binding.orderDetailShipmentList) {
            isVisible = show
            showAddTrackingButton(show) { viewModel.onAddShipmentTrackingClicked() }
        }
    }

    private fun showShipmentTrackings(
        shipmentTrackings: List<OrderShipmentTracking>
    ) {
        binding.orderDetailShipmentList.updateShipmentTrackingList(
            shipmentTrackings = shipmentTrackings,
            dateUtils = dateUtils,
            onDeleteShipmentTrackingClicked = {
                viewModel.onDeleteShipmentTrackingClicked(it)
            }
        )
    }

    private fun showShippingLabels(shippingLabels: List<ShippingLabel>, currency: String) {
        shippingLabels.whenNotNullNorEmpty {
            with(binding.orderDetailShippingLabelList) {
                show()
                updateShippingLabels(
                    shippingLabels = shippingLabels,
                    productImageMap = productImageMap,
                    formatCurrencyForDisplay = currencyFormatter.buildBigDecimalFormatter(currency),
                    productClickListener = this@OrderDetailFragment,
                    shippingLabelClickListener = object : OnShippingLabelClickListener {
                        override fun onRefundRequested(shippingLabel: ShippingLabel) {
                            viewModel.onRefundShippingLabelClick(shippingLabel.id)
                        }

                        override fun onPrintShippingLabelClicked(shippingLabel: ShippingLabel) {
                            viewModel.onPrintShippingLabelClicked(shippingLabel.id)
                        }

                        override fun onPrintCustomsFormClicked(shippingLabel: ShippingLabel) {
                            viewModel.onPrintCustomsFormClicked(shippingLabel)
                        }
                    }
                )
            }
        }.otherwise {
            binding.orderDetailShippingLabelList.hide()
        }
    }

    private fun displayShippingLabelsWIPCard(show: Boolean) {
        if (show && feedbackState != DISMISSED) {
            binding.orderDetailShippingLabelsWipCard.isVisible = true

            binding.orderDetailShippingLabelsWipCard.initView(
                getString(R.string.orderdetail_shipping_label_m2_wip_title),
                getString(R.string.orderdetail_shipping_label_m3_wip_message),
                onGiveFeedbackClick = { onGiveFeedbackClicked() },
                onDismissClick = { onDismissProductWIPNoticeCardClicked() }
            )
        } else binding.orderDetailShippingLabelsWipCard.isVisible = false
    }

    private fun onGiveFeedbackClicked() {
        val context = AnalyticsTracker.VALUE_SHIPPING_LABELS_M4_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(GIVEN)
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.SHIPPING_LABELS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissProductWIPNoticeCardClicked() {
        val context = AnalyticsTracker.VALUE_SHIPPING_LABELS_M4_FEEDBACK

        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to context,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(DISMISSED)
        displayShippingLabelsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeedbackState) {
        FeatureFeedbackSettings(
            SHIPPING_LABEL_M4,
            state
        ).registerItself()
    }

    private fun displayUndoSnackbar(
        message: String,
        actionListener: View.OnClickListener,
        dismissCallback: Snackbar.Callback
    ) {
        undoSnackbar = uiMessageResolver.getUndoSnack(
            message = message,
            actionListener = actionListener
        ).also {
            it.addCallback(dismissCallback)
            it.show()
        }
    }
}
